package ee.household.bills.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.household.bills.api.Bill
import ee.household.bills.core.Session

@Composable
fun BillsScreen(bills: List<Bill>, session: Session) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf<String?>(null) }
    val categories = remember(bills) { bills.map { it.vendorCategory }.distinct().sorted() }

    val shown = bills.filter { filter == null || it.vendorCategory == filter }
    // Group by the attributed cost-month (server-computed, dashboard-consistent):
    // a June-issued May-service transport bill groups under May, not June.
    fun monthOf(b: Bill) = b.costMonth ?: (b.invoiceDate ?: "").take(7)
    val byMonth = shown.groupBy(::monthOf).toSortedMap(compareByDescending { it })

    Column {
        // Filter chips
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip("All", filter == null) { filter = null }
            categories.forEach { c ->
                FilterChip("${categoryIcon(c)} ${prettyVendor(c)}", filter == c) { filter = c }
            }
        }

        byMonth.forEach { (month, monthBills) ->
            val subtotal = monthBills.sumOf { it.totalAmount ?: 0.0 }
            Text("${fmtYm(month)} · ${eur(subtotal)}", color = Muted, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
            Card {
                monthBills.forEach { b ->
                    BillRow(b) {
                        b.pdfFile?.let { f ->
                            val url = "${session.serverUrl}/bills/" + Uri.encode(f)
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                }
            }
            Spacer(Modifier.size(4.dp))
        }
        if (bills.isEmpty()) {
            Card { Text("No bills in cache.", color = Muted, fontSize = 14.sp) }
        }
    }
}

@Composable
private fun BillRow(b: Bill, onPdf: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = b.pdfFile != null, onClick = onPdf)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).background(Surface2, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center) {
            Text(categoryIcon(b.vendorCategory), fontSize = 16.sp)
        }
        Spacer(Modifier.size(11.dp))
        Column(Modifier.weight(1f)) {
            Text(prettyVendor(b.vendorCategory), color = TextMain, fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold)
            val period = listOfNotNull(b.periodStart, b.periodEnd).joinToString(" – ")
            Text(if (period.isNotBlank()) "period $period" else (b.invoiceDate ?: ""),
                color = Muted, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(eur(b.totalAmount ?: 0.0), color = TextMain, fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
            if (b.pdfFile != null) Text("PDF", color = Primary, fontSize = 10.sp)
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (selected) Primary else Surface2, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = if (selected) androidx.compose.ui.graphics.Color.White else TextMain,
            fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
