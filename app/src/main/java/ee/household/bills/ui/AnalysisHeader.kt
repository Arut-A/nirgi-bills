package ee.household.bills.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.household.bills.api.Series

/** Always-visible range presets + a summary of the selected range.
 *  Shared by Costs and Usage so analysis is one tap, never buried. */
@Composable
fun AnalysisHeader(s: Series, filters: FilterState, showMoneySummary: Boolean) {
    val months = filters.months(s)
    val cats = filters.activeCategories(s)
    val complete = months.filter { it < s.cutoffYm }

    // One-tap range presets
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        filters.presets(s).forEach { p ->
            FilterChip(p, filters.preset == p) { filters.preset = p }
        }
    }
    Spacer(Modifier.height(8.dp))

    if (showMoneySummary && complete.isNotEmpty()) {
        val totals = complete.map { m -> cats.sumOf { s.data[it]?.get(m) ?: 0.0 } }
        val total = totals.sum()
        val avg = total / complete.size
        val peakIdx = totals.indices.maxByOrNull { totals[it] } ?: 0
        Card {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Kpi(eur0(total), "total · ${complete.size} mo", modifier = Modifier.weight(1f))
                Kpi(eur0(avg), "avg / month", modifier = Modifier.weight(1f))
                Kpi(eur0(totals[peakIdx]), "peak: ${fmtYmShort(complete[peakIdx])}",
                    modifier = Modifier.weight(1f))
            }
            if (!filters.allCategoriesActive) {
                Text("${cats.size} of ${s.vendors.size} categories shown — tap the legend to change",
                    color = Muted, fontSize = 10.sp, modifier = Modifier.height(16.dp))
            }
        }
    }
}
