package ee.household.bills.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.household.bills.api.Series
import kotlin.math.abs

private fun monthTotal(s: Series, cats: List<String>, m: String): Double =
    cats.sumOf { s.data[it]?.get(m) ?: 0.0 }

@Composable
fun CostsScreen(s: Series, filters: FilterState) {
    AnalysisHeader(s, filters, showMoneySummary = true)
    val months = filters.months(s)
    val cats = filters.activeCategories(s)
    val complete = months.filter { it < s.cutoffYm }

    // ── Monthly cost by category (stacked, tap a bar to drill into the month) ──
    var drillIdx by remember(months) { mutableStateOf(months.lastIndex) }
    Card {
        SectionLabel("Monthly cost by category · tap a bar")
        val seriesData = cats.map { v ->
            categoryColor(v) to months.map { m -> s.data[v]?.get(m) ?: 0.0 }
        }
        StackedBarChart(
            months, seriesData,
            dimLastBar = months.lastOrNull()?.let { it >= s.cutoffYm } == true,
            readoutFor = { i -> "${fmtYm(months[i])} · ${eur(monthTotal(s, cats, months[i]))}" },
            axisLabel = { eur0(it) },
            onSelect = { drillIdx = it },
        )
        Spacer(Modifier.height(8.dp))
        FlowLegend(s, filters)
        if (months.lastOrNull()?.let { it >= s.cutoffYm } == true) {
            Text("current month faded — incomplete (accrual clip)", color = Muted, fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
    }

    // ── Drill-down: the tapped month's category breakdown ──
    months.getOrNull(drillIdx)?.let { dm ->
        val rows = cats.map { v -> v to (s.data[v]?.get(dm) ?: 0.0) }
            .filter { it.second > 0 }.sortedByDescending { it.second }
        val total = rows.sumOf { it.second }
        Card {
            SectionLabel("${fmtYm(dm)} breakdown · ${eur(total)}")
            if (rows.isEmpty()) {
                Text("No spend in this month.", color = Muted, fontSize = 13.sp)
            } else rows.forEach { (v, amt) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).background(categoryColor(v), RoundedCornerShape(2.dp)))
                    Spacer(Modifier.size(8.dp))
                    Text("${categoryIcon(v)} ${prettyVendor(v)}", color = TextMain,
                        fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                    Text("${n1(if (total > 0) amt / total * 100 else 0.0)}%",
                        color = Muted, fontSize = 11.sp)
                    Spacer(Modifier.size(10.dp))
                    Text(eur(amt), color = TextMain, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // ── Category breakdown (avg/mo over visible range) ──
    Card {
        SectionLabel("Category breakdown · avg/mo")
        val n = complete.size.coerceAtLeast(1)
        cats.map { v -> v to complete.sumOf { s.data[v]?.get(it) ?: 0.0 } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .forEach { (v, total) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(9.dp).background(categoryColor(v), RoundedCornerShape(2.dp)))
                    Spacer(Modifier.size(8.dp))
                    Text("${categoryIcon(v)} ${prettyVendor(v)}", color = TextMain,
                        fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(eur0(total / n) + "/mo", color = TextMain, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.size(8.dp))
                    Text(eur0(total), color = Muted, fontSize = 11.sp)
                }
            }
    }

    // ── Year-over-year (avg/mo per category, last two years in range) ──
    val years = complete.map { it.substring(0, 4) }.distinct().sorted()
    if (years.size >= 2) {
        val y0 = years[years.size - 2]; val y1 = years.last()
        Card {
            SectionLabel("Year-over-year · avg/mo · $y0 vs $y1")
            cats.forEach { v ->
                val a = complete.filter { it.startsWith(y0) }.mapNotNull { s.data[v]?.get(it) }
                val b = complete.filter { it.startsWith(y1) }.mapNotNull { s.data[v]?.get(it) }
                if (a.isNotEmpty() && b.isNotEmpty()) {
                    val avgA = a.average(); val avgB = b.average()
                    val pct = if (avgA > 0) (avgB - avgA) / avgA * 100 else 0.0
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${categoryIcon(v)} ${prettyVendor(v)}", color = TextMain,
                            fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(eur(avgA), color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.size(10.dp))
                        Text(eur(avgB), color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.size(10.dp))
                        val up = pct > 0
                        Text((if (up) "▲ " else "▼ ") + n1(abs(pct)) + "%",
                            color = if (up) Up else Down, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // ── Budget forecast: monthly totals + trailing 6-mo average projection ──
    if (complete.size >= 3) {
        Card {
            SectionLabel("Monthly total · tap a point")
            val totals = complete.map { monthTotal(s, cats, it) }
            val avg = totals.takeLast(6).average()
            LineChart(
                complete, totals.map { it as Double? }, Primary,
                readoutFor = { i -> "${fmtYm(complete[i])} · ${eur(totals[i])}" },
                height = 130, axisLabel = { eur0(it) }, baseline = avg,
            )
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(10.dp)).padding(10.dp)) {
                Text("📈 Forecast next 3 months ≈ ${eur0(avg)}/mo  (trailing 6-month average)",
                    color = TextMain, fontSize = 12.sp)
            }
        }
    }
}

/** Tappable legend — tap a category to toggle it in/out of the charts.
 *  Inactive categories show dimmed. No sheet needed for the common case. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowLegend(s: Series, filters: FilterState) {
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        s.vendors.forEach { v ->
            val on = filters.categories[v] != false
            Row(
                Modifier.clickable { filters.toggleCategory(v, s) }.padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(9.dp).background(
                    if (on) categoryColor(v) else Surface2, RoundedCornerShape(2.dp)))
                Spacer(Modifier.size(5.dp))
                Text(prettyVendor(v), color = if (on) TextMain else Muted, fontSize = 11.sp)
            }
        }
    }
}
