package ee.household.bills.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(Surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(Locale.ROOT), color = Muted, fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
    Spacer(Modifier.height(6.dp))
}

@Composable
fun Kpi(value: String, label: String, color: Color = TextMain, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Muted, fontSize = 10.sp)
    }
}

@Composable
fun StaleBanner(lastSyncMs: Long) {
    val ts = if (lastSyncMs > 0)
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastSyncMs))
    else "never"
    Box(
        Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(10.dp)).padding(8.dp),
        contentAlignment = Alignment.Center,
    ) { Text("⚠ offline · showing data from $ts", color = Color(0xFFF59E0B), fontSize = 12.sp) }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.size(5.dp))
        Text(label, color = Muted, fontSize = 10.sp)
    }
}
