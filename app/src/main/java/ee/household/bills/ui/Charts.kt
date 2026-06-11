package ee.household.bills.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GridLine = Color(0x14FFFFFF)

/** Coloured selection pill shown above each chart: dot · month · value. */
@Composable
private fun Readout(color: Color, month: String, value: String) {
    Row(
        Modifier.padding(bottom = 6.dp)
            .background(Surface2, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Text("  $month", color = Muted, fontSize = 12.sp)
        Text("   $value", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** Smooth cubic path through points (horizontal-tangent control points). */
private fun smoothPath(pts: List<Offset>): Path {
    val p = Path()
    if (pts.isEmpty()) return p
    p.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
        val prev = pts[i - 1]; val cur = pts[i]
        val midX = (prev.x + cur.x) / 2f
        p.cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
    }
    return p
}

/** Premium line chart: animated draw-in, smooth curve, gradient fill,
 *  gridlines, tap-to-select with guide + value pill. Null values break the
 *  line (no fake zero-dips). */
@Composable
fun LineChart(
    months: List<String>,
    values: List<Double?>,
    color: Color,
    readoutFor: (Int) -> String,
    modifier: Modifier = Modifier,
    height: Int = 120,
    axisLabel: (Double) -> String = { it.toInt().toString() },
    baseline: Double? = null,
) {
    val present = values.filterNotNull()
    if (present.isEmpty()) { EmptyChart(modifier, height); return }
    val max = present.max()
    val min = present.min()
    val range = (max - min).coerceAtLeast(0.0001)
    val lastReal = values.indexOfLast { it != null }
    var selected by remember(values.size) { mutableIntStateOf(lastReal) }
    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(color = Muted, fontSize = 9.sp)
    val avgStyle = TextStyle(color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)

    val anim = remember(values.hashCode()) { Animatable(0f) }
    LaunchedEffect(values.hashCode()) { anim.animateTo(1f, tween(550, easing = FastOutSlowInEasing)) }

    val sel = selected.coerceIn(0, values.size - 1)
    val readout = readoutFor(sel).split(" · ", limit = 2)

    Column(modifier) {
        Readout(color, readout.getOrElse(0) { "" }, readout.getOrElse(1) { "" })
        Canvas(
            Modifier.fillMaxWidth().height(height.dp).pointerInput(values.size) {
                detectTapGestures { o ->
                    val raw = (o.x / size.width * values.size).toInt().coerceIn(0, values.size - 1)
                    selected = (0..values.size).firstNotNullOfOrNull { d ->
                        listOf(raw - d, raw + d).firstOrNull { it in values.indices && values[it] != null }
                    } ?: raw
                }
            },
        ) {
            val padTop = size.height * 0.16f
            val padBot = size.height * 0.16f
            val usable = size.height - padTop - padBot
            val n = values.size
            val stepX = if (n > 1) size.width / (n - 1) else 0f
            fun yOf(v: Double): Float {
                val t = (v - min) / range
                val full = padTop + (usable - (t * usable).toFloat())
                val base = size.height - padBot
                return base - (base - full) * anim.value     // grow up from baseline
            }

            // gridlines (3)
            repeat(3) { g ->
                val y = padTop + usable * g / 2f
                drawLine(GridLine, Offset(0f, y), Offset(size.width, y), 1f)
            }
            // Y-axis scale labels (max top-left, min bottom-left)
            drawText(measurer, axisLabel(max), topLeft = Offset(2f, 0f), style = axisStyle)
            drawText(measurer, axisLabel(min),
                topLeft = Offset(2f, size.height - padBot * 0.9f), style = axisStyle)
            // average / baseline reference line — deviations read against it
            baseline?.let { b ->
                if (b in min..max) {
                    val yb = padTop + (usable - ((b - min) / range * usable).toFloat())
                    drawLine(Color(0x55FFFFFF), Offset(0f, yb), Offset(size.width, yb), 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                    drawText(measurer, "avg ${axisLabel(b)}",
                        topLeft = Offset(size.width * 0.62f, yb - 13f), style = avgStyle)
                }
            }

            // contiguous non-null segments
            var seg = mutableListOf<Offset>()
            fun flush() {
                if (seg.size >= 2) {
                    val line = smoothPath(seg)
                    // gradient area fill
                    val fill = Path().apply {
                        addPath(line)
                        lineTo(seg.last().x, size.height - padBot)
                        lineTo(seg.first().x, size.height - padBot)
                        close()
                    }
                    drawPath(fill, Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.28f * anim.value), color.copy(alpha = 0f)),
                        startY = padTop, endY = size.height - padBot))
                    drawPath(line, color, style = Stroke(width = 3.5f))
                } else if (seg.size == 1) {
                    drawCircle(color, 3.5f, seg[0])
                }
                seg = mutableListOf()
            }
            values.forEachIndexed { i, v ->
                if (v == null) flush() else seg.add(Offset(i * stepX, yOf(v)))
            }
            flush()

            // selection guide + marker
            values.getOrNull(sel)?.let { v ->
                val x = sel * stepX; val y = yOf(v)
                drawLine(Color(0x33FFFFFF), Offset(x, padTop * 0.4f), Offset(x, size.height - padBot), 1.5f)
                drawCircle(color, 7f, Offset(x, y))
                drawCircle(Color.White, 3.5f, Offset(x, y))
            }
        }
        AxisRow(months, sel)
    }
}

/** Premium stacked bars: rounded-top stack, per-segment fill, animated grow,
 *  gridlines, tap-to-select with highlight + total pill. */
@Composable
fun StackedBarChart(
    months: List<String>,
    series: List<Pair<Color, List<Double>>>,
    dimLastBar: Boolean,
    readoutFor: (Int) -> String,
    modifier: Modifier = Modifier,
    height: Int = 190,
    axisLabel: (Double) -> String = { it.toInt().toString() },
    onSelect: (Int) -> Unit = {},
) {
    if (months.isEmpty()) { EmptyChart(modifier, height); return }
    val totals = months.indices.map { i -> series.sumOf { it.second.getOrElse(i) { 0.0 } } }
    val max = (totals.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val avg = totals.filterIndexed { i, _ -> !(dimLastBar && i == months.lastIndex) }
        .filter { it > 0 }.average().let { if (it.isNaN()) 0.0 else it }
    var selected by remember(months.size) { mutableIntStateOf(months.size - 1) }
    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(color = Muted, fontSize = 9.sp)
    val avgStyle = TextStyle(color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)

    val anim = remember(months.size, series.size) { Animatable(0f) }
    LaunchedEffect(months.size, series.size) { anim.animateTo(1f, tween(550, easing = FastOutSlowInEasing)) }

    val sel = selected.coerceIn(0, months.size - 1)
    val readout = readoutFor(sel).split(" · ", limit = 2)

    Column(modifier) {
        Readout(Primary, readout.getOrElse(0) { "" }, readout.getOrElse(1) { "" })
        Canvas(
            Modifier.fillMaxWidth().height(height.dp).pointerInput(months.size) {
                detectTapGestures { o ->
                    selected = (o.x / size.width * months.size).toInt().coerceIn(0, months.size - 1)
                    onSelect(selected)
                }
            },
        ) {
            repeat(3) { g ->
                val y = size.height * g / 3f
                drawLine(GridLine, Offset(0f, y), Offset(size.width, y), 1f)
            }
            // peak-scale label + average reference line
            drawText(measurer, axisLabel(max), topLeft = Offset(2f, 0f), style = axisStyle)
            if (avg > 0) {
                val yb = size.height - (avg / max * size.height).toFloat()
                drawLine(Color(0x55FFFFFF), Offset(0f, yb), Offset(size.width, yb), 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                drawText(measurer, "avg ${axisLabel(avg)}",
                    topLeft = Offset(size.width * 0.62f, yb - 13f), style = avgStyle)
            }
            val n = months.size
            val gap = (size.width / n) * 0.30f
            val bw = (size.width - gap * (n - 1)) / n
            val r = (bw * 0.28f).coerceAtMost(10f)
            months.indices.forEach { i ->
                val x = i * (bw + gap)
                val total = totals[i]
                if (total <= 0) return@forEach
                if (i == sel) drawRoundRect(Color(0x18FFFFFF), Offset(x - gap / 2, 0f),
                    Size(bw + gap, size.height), CornerRadius(6f, 6f))
                val totalH = (total / max * size.height).toFloat() * anim.value
                // clip the stack to a rounded-top rect, then paint segments
                val clip = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(
                        Rect(Offset(x, size.height - totalH), Size(bw, totalH)),
                        topLeft = CornerRadius(r, r), topRight = CornerRadius(r, r),
                        bottomLeft = CornerRadius(0f, 0f), bottomRight = CornerRadius(0f, 0f)))
                }
                clipPath(clip) {
                    var yTop = size.height
                    val dim = dimLastBar && i == n - 1
                    series.forEach { (c, vals) ->
                        val v = vals.getOrElse(i) { 0.0 }
                        if (v > 0) {
                            val h = (v / max * size.height).toFloat() * anim.value
                            yTop -= h
                            drawRect(if (dim) c.copy(alpha = 0.3f) else c,
                                Offset(x, yTop), Size(bw, h))
                        }
                    }
                }
            }
        }
        AxisRow(months, sel)
    }
}

@Composable
fun Donut(slices: List<Pair<Color, Double>>, centerLabel: String, size: Int = 104) {
    val total = slices.sumOf { it.second }.coerceAtLeast(0.0001)
    Box(Modifier.size(size.dp).padding(2.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val d = minOf(this.size.width, this.size.height)
            val topLeft = Offset((this.size.width - d) / 2f, (this.size.height - d) / 2f)
            var start = -90f
            val ring = d * 0.16f
            slices.forEach { (c, v) ->
                val sweep = (v / total * 360f).toFloat()
                drawArc(c, start, sweep, false,
                    Offset(topLeft.x + ring / 2, topLeft.y + ring / 2),
                    Size(d - ring, d - ring), style = Stroke(width = ring))
                start += sweep
            }
        }
        Text(centerLabel, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AxisRow(months: List<String>, selected: Int) {
    if (months.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(fmtYmShort(months.first()), color = Muted, fontSize = 9.sp,
            modifier = Modifier.weight(1f))
        if (selected in 1 until months.size - 1) {
            Text(fmtYmShort(months[selected]), color = TextMain, fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Text(fmtYmShort(months.last()), color = Muted, fontSize = 9.sp,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun EmptyChart(modifier: Modifier = Modifier, height: Int = 60) {
    Box(modifier.fillMaxWidth().height(height.dp), contentAlignment = Alignment.Center) {
        Text("no data in range", color = Muted, fontSize = 11.sp)
    }
}
