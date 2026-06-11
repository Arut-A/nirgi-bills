package ee.household.bills.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dashboard palette — the app must feel like the dashboard.
val Bg = Color(0xFF0F1117)
val Surface = Color(0xFF1A1D27)
val Surface2 = Color(0xFF222531)
val Border = Color(0xFF2A2D3A)
val TextMain = Color(0xFFE2E8F0)
val Muted = Color(0xFF64748B)
val Primary = Color(0xFF6366F1)
val Secondary = Color(0xFFA855F7)
val Up = Color(0xFFEF4444)      // spending up = bad = red
val Down = Color(0xFF22C55E)    // spending down = good = green

val CategoryColors = mapOf(
    "electricity" to Color(0xFFF59E0B),
    "electricity_network" to Color(0xFFFCD34D),
    "gas" to Color(0xFF3B82F6),
    "gas_network" to Color(0xFF93C5FD),
    "water" to Color(0xFF06B6D4),
    "house_insurance" to Color(0xFF14B8A6),
    "internet" to Color(0xFF10B981),
    "home_security" to Color(0xFF10B981),
    "phone" to Color(0xFF8B5CF6),
    "garbage" to Color(0xFF6B7280),
    "pellets" to Color(0xFFEF4444),
)

val CategoryIcons = mapOf(
    "electricity" to "⚡", "electricity_network" to "🔌",
    "gas" to "🔥", "gas_network" to "🏭",
    "water" to "💧", "phone" to "📱", "internet" to "🌐",
    "home_security" to "🔒", "garbage" to "🗑️",
    "pellets" to "🪵", "house_insurance" to "🏠",
)

fun categoryColor(slug: String): Color = CategoryColors[slug] ?: Muted
fun categoryIcon(slug: String): String = CategoryIcons[slug] ?: "📄"

private val DarkScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Bg,
    surface = Surface,
    surfaceVariant = Surface2,
    onPrimary = Color.White,
    onBackground = TextMain,
    onSurface = TextMain,
    outline = Border,
)

@Composable
fun NirgiBillsTheme(content: @Composable () -> Unit) {
    // Single-user app, dark like the dashboard — no light theme on purpose.
    isSystemInDarkTheme() // intentionally ignored
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
