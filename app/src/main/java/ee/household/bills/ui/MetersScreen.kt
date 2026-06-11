package ee.household.bills.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.household.bills.api.Series
import java.util.Locale

private data class MeterDef(val key: String, val cat: String, val unit: String, val color: Color)

/** Meter usage measured on each bill (consumption per period). The bills carry
 *  kWh / m³ readings; the dedicated meter_readings table is empty, so this
 *  reads the real per-period values straight from the series. */
@Composable
fun MetersScreen(s: Series) {
    val meters = listOf(
        MeterDef("elec", "electricity", "kWh", Color(0xFFF59E0B)) to s.consumption.elecKwh,
        MeterDef("gas", "gas", "m³", Color(0xFF3B82F6)) to s.consumption.gasM3,
        MeterDef("water", "water", "m³", Color(0xFF06B6D4)) to s.consumption.waterM3,
    )

    Box(
        Modifier.background(Surface2, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) { Text("📥 usage measured per bill", color = TextMain, fontSize = 11.sp) }
    Spacer(Modifier.size(4.dp))

    val anyData = meters.any { it.second.isNotEmpty() }
    if (!anyData) {
        Card {
            Text("No usage data yet.", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Values appear here once bills carrying meter readings are parsed.",
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        return
    }

    meters.forEach { (def, data) ->
        if (data.isEmpty()) return@forEach
        val months = data.keys.sorted()
        val latestMonth = months.last()
        val latestVal = data[latestMonth] ?: 0.0
        val cumulative = data.values.sum()

        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).background(Surface2, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(categoryIcon(def.cat), fontSize = 16.sp)
                }
                Spacer(Modifier.size(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(prettyVendor(def.cat), color = TextMain, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text("latest: ${fmtYm(latestMonth)}", color = Muted, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(String.format(Locale.ROOT, "%,.1f %s", latestVal, def.unit),
                        color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Σ ${String.format(Locale.ROOT, "%,.0f %s", cumulative, def.unit)}",
                        color = Muted, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.size(6.dp))
            val avg = months.mapNotNull { data[it] }.let { if (it.isEmpty()) null else it.average() }
            LineChart(
                months, months.map { data[it] }, def.color,
                readoutFor = { i ->
                    "${fmtYm(months[i])} · " +
                        String.format(Locale.ROOT, "%,.1f %s", data[months[i]] ?: 0.0, def.unit)
                },
                height = 110,
                axisLabel = { String.format(Locale.ROOT, "%,.0f", it) },
                baseline = avg,
            )
        }
        Spacer(Modifier.size(10.dp))
    }
}
