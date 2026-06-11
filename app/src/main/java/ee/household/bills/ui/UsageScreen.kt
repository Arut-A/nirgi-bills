package ee.household.bills.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.household.bills.api.MonthUnitCosts
import ee.household.bills.api.Series

private val ELEC = Color(0xFFF59E0B)
private val GAS = Color(0xFF3B82F6)
private val WATER = Color(0xFF06B6D4)
private val PELLET = Color(0xFFEF4444)

private fun latest(months: List<String>, pick: (String) -> Double?): Pair<String, Double>? {
    for (m in months.reversed()) pick(m)?.let { return m to it }
    return null
}

@Composable
fun UsageScreen(s: Series, filters: FilterState) {
    AnalysisHeader(s, filters, showMoneySummary = false)
    val months = filters.months(s)
    fun uc(m: String): MonthUnitCosts? = s.unitCosts[m]

    // ── Latest unit-cost big numbers ──
    Card {
        SectionLabel("Unit costs · latest")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            latest(months) { uc(it)?.elecSKwh }?.let { Kpi(n1(it.second), "⚡ s/kWh", ELEC, Modifier.weight(1f)) }
            latest(months) { uc(it)?.gasSKwh }?.let { Kpi(n1(it.second), "🔥 gas s/kWh", GAS, Modifier.weight(1f)) }
            latest(months) { uc(it)?.waterEurM3 }?.let { Kpi(n2(it.second), "💧 €/m³", WATER, Modifier.weight(1f)) }
            latest(months) { uc(it)?.pelletsSKwh }?.let { Kpi(n1(it.second), "🪵 s/kWh", PELLET, Modifier.weight(1f)) }
        }
    }

    UnitCostCard("⚡ Electricity · s/kWh (incl. network & VAT)", months, ELEC, "s/kWh", ::n1) { uc(it)?.elecSKwh }
    UnitCostCard("🔥 Gas · s/kWh", months, GAS, "s/kWh", ::n1) { uc(it)?.gasSKwh }
    UnitCostCard("🔥 Gas · s/m³", months, GAS, "s/m³", ::n1) { uc(it)?.gasSM3 }
    UnitCostCard("💧 Water · €/m³", months, WATER, "€/m³", ::n2) { uc(it)?.waterEurM3 }
    UnitCostCard("🪵 Pellets · s/kWh (season avg)", months, PELLET, "s/kWh", ::n1) { uc(it)?.pelletsSKwh }

    // ── Consumption trends (usage, not cost) ──
    Card {
        SectionLabel("Consumption · usage, not cost")
        ConsumptionLine("⚡ electricity", "kWh", months, ELEC) { s.consumption.elecKwh[it] }
        ConsumptionLine("🔥 gas", "m³", months, GAS) { s.consumption.gasM3[it] }
        ConsumptionLine("💧 water", "m³", months, WATER) { s.consumption.waterM3[it] }
        Text("heating seasons visible — gas & electricity peak in winter, water stays flat",
            color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun UnitCostCard(
    title: String, months: List<String>, color: Color, unit: String,
    fmt: (Double) -> String, pick: (String) -> Double?,
) {
    val values = months.map(pick)
    if (values.all { it == null }) return
    val hist = values.filterNotNull()
    val avg = if (hist.isNotEmpty()) hist.average() else null
    Card {
        SectionLabel(title)
        LineChart(months, values, color, height = 120,
            readoutFor = { i -> "${fmtYm(months[i])} · " + (values[i]?.let { "${fmt(it)} $unit" } ?: "—") },
            axisLabel = { fmt(it) }, baseline = avg)
    }
}

@Composable
private fun ConsumptionLine(label: String, unit: String, months: List<String>, color: Color, pick: (String) -> Double?) {
    val values = months.map(pick)
    if (values.all { it == null }) return
    Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp))
    LineChart(months, values, color, height = 96,
        readoutFor = { i -> "${fmtYm(months[i])} · " + (values[i]?.let { "${n1(it)} $unit" } ?: "—") },
        axisLabel = { n1(it) })
}
