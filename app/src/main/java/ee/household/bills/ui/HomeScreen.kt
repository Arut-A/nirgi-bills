package ee.household.bills.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.household.bills.api.Summary
import ee.household.bills.api.UnitCost
import ee.household.bills.core.SummaryState
import kotlin.math.abs

@Composable
fun HomeScreen(state: SummaryState, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Bg)
            .verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (state) {
            is SummaryState.Loading -> Box(
                Modifier.fillMaxWidth().padding(top = 120.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = Primary) }

            is SummaryState.Empty -> Card {
                Text("No data yet — server unreachable (${state.reason}).",
                    color = Muted, fontSize = 14.sp)
                Text("Pull to retry or check Settings.", color = Muted, fontSize = 12.sp)
            }

            is SummaryState.Unauthorized -> Card {
                Text("Session expired — sign in again.", color = Up, fontSize = 14.sp)
            }

            is SummaryState.Live -> SummaryContent(state.summary, staleMs = null)
            is SummaryState.Stale -> SummaryContent(state.summary, staleMs = state.lastSyncMs)
        }
    }
}

@Composable
private fun SummaryContent(s: Summary, staleMs: Long?) {
    if (staleMs != null) StaleBanner(staleMs)

    // Hero: current month so far + last complete month
    Card {
        SectionLabel("${fmtYm(s.cutoffYm)} · so far")
        Row(verticalAlignment = Alignment.Bottom) {
            Text(eur(s.monthToDate), color = TextMain, fontSize = 32.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(10.dp))
            Text("${s.monthToDateBills} bills in", color = Muted, fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 5.dp))
        }
        if (s.latestMonth != null && s.latestTotal != null) {
            Row {
                Text("${fmtYm(s.latestMonth)} total ", color = Muted, fontSize = 13.sp)
                Text(eur(s.latestTotal), color = TextMain, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold)
                s.momPct?.let {
                    Spacer(Modifier.size(8.dp))
                    val up = it > 0
                    Text(
                        (if (up) "▲ " else "▼ ") + n1(kotlin.math.abs(it)) + "% vs ${fmtYm(s.prevMonth)}",
                        color = if (up) Up else Down, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    // ── What changed: last complete month vs its trailing baseline ──
    if (s.latestMonth != null && s.latestTotal != null && s.baselineTotal != null) {
        val pct = s.latestVsBaselinePct ?: 0.0
        val up = pct > 0
        val verdictColor = if (kotlin.math.abs(pct) < 5) Muted else if (up) Up else Down
        Card {
            SectionLabel("What changed · ${fmtYm(s.latestMonth)}")
            Row(verticalAlignment = Alignment.Bottom) {
                Text(eur(s.latestTotal), color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(10.dp))
                Text((if (up) "▲ " else "▼ ") + n1(kotlin.math.abs(pct)) + "%",
                    color = verdictColor, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 3.dp))
            }
            val deltaAbs = s.latestTotal - s.baselineTotal
            Text(
                when {
                    kotlin.math.abs(pct) < 5 -> "In line with your 6-month average of ${eur0(s.baselineTotal)}."
                    up -> "${eur0(deltaAbs)} above your 6-month average of ${eur0(s.baselineTotal)}."
                    else -> "${eur0(-deltaAbs)} below your 6-month average of ${eur0(s.baselineTotal)}."
                },
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp),
            )
        }
    }

    // ── Biggest movers: per-category deviation from each category's baseline ──
    if (s.deviations.isNotEmpty()) {
        Card {
            SectionLabel("Biggest movers vs usual")
            s.deviations.take(6).forEach { d ->
                val up = d.delta > 0
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(categoryIcon(d.category), fontSize = 15.sp)
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(prettyVendor(d.category), color = TextMain, fontSize = 13.5.sp)
                            if (d.anomaly) {
                                Spacer(Modifier.size(5.dp))
                                Text("⚠", fontSize = 12.sp)
                            }
                        }
                        Text("now ${eur(d.latest)} · usually ${eur0(d.baseline)}",
                            color = Muted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text((if (up) "+" else "−") + eur0(kotlin.math.abs(d.delta)),
                            color = if (up) Up else Down, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        d.pct?.let {
                            Text((if (up) "▲" else "▼") + n1(kotlin.math.abs(it)) + "%",
                                color = if (up) Up else Down, fontSize = 10.sp)
                        }
                    }
                }
            }
            Text("↑ red = above your normal · ↓ green = below", color = Muted, fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
    }

    // ── Unit costs with trend vs 6-month baseline ──
    Card {
        SectionLabel("Unit costs · latest vs usual")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnitCostKpi(s.unitCosts["elec_s_kwh"], "⚡ s/kWh", Color(0xFFF59E0B), ::n1, Modifier.weight(1f))
            UnitCostKpi(s.unitCosts["gas_s_kwh"], "🔥 s/kWh", Color(0xFF3B82F6), ::n1, Modifier.weight(1f))
            UnitCostKpi(s.unitCosts["water_eur_m3"], "💧 €/m³", Color(0xFF06B6D4), ::n2, Modifier.weight(1f))
            UnitCostKpi(s.unitCosts["pellets_s_kwh"], "🪵 s/kWh", Color(0xFFEF4444), ::n1, Modifier.weight(1f))
        }
    }

    // Spending by category, latest complete month
    if (s.breakdownLatest.isNotEmpty()) {
        Card {
            SectionLabel("Spending by category · ${fmtYm(s.latestMonth)}")
            s.breakdownLatest.forEach { c ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(9.dp).background(categoryColor(c.category), CircleShape))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${categoryIcon(c.category)} ${c.category.replace('_', ' ')}",
                        color = TextMain, fontSize = 14.sp, modifier = Modifier.weight(1f),
                    )
                    Text("${n1(c.pct)}%", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.size(10.dp))
                    Text(eur(c.amount), color = TextMain, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // 12-month trend (premium area line; tap for any month)
    if (s.last12Months.isNotEmpty()) {
        Card {
            SectionLabel("Last 12 complete months · tap a point")
            val mm = s.last12Months.map { it.month }
            LineChart(mm, s.last12Months.map { it.total as Double? }, Primary,
                readoutFor = { i -> "${fmtYm(mm[i])} · ${eur(s.last12Months[i].total)}" },
                height = 120, axisLabel = { eur0(it) }, baseline = s.monthlyAvg12)
        }
    }
}


@Composable
private fun UnitCostKpi(
    uc: UnitCost?, label: String, color: Color,
    fmt: (Double) -> String, modifier: Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (uc == null) {
            Text("—", color = Muted, fontSize = 17.sp)
            Text(label, color = Muted, fontSize = 10.sp)
            return@Column
        }
        Text(fmt(uc.value), color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        uc.pct?.let {
            if (abs(it) >= 1) {
                val up = it > 0
                Text((if (up) "▲" else "▼") + n1(abs(it)) + "%",
                    color = if (up) Up else Down, fontSize = 9.sp)
            }
        }
        Text(label, color = Muted, fontSize = 10.sp)
    }
}
