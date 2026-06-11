package ee.household.bills.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.household.bills.api.Series

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(s: Series, filters: FilterState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface2) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text("Range", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(Modifier.padding(top = 6.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                filters.presets(s).forEach { p ->
                    FilterChip(p, filters.preset == p) { filters.preset = p }
                }
            }

            Text("Categories", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(Modifier.padding(top = 6.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                s.vendors.forEach { v ->
                    val on = filters.categories[v] != false
                    FilterChip("${categoryIcon(v)} ${prettyVendor(v)}", on) {
                        filters.toggleCategory(v, s)
                    }
                }
            }
        }
    }
}
