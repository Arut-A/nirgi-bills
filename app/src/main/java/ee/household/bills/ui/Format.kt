package ee.household.bills.ui

import java.util.Locale

// Locale-pinned: phone language must never change number meaning (31.51 != 3151).
fun eur(v: Double): String = String.format(Locale.ROOT, "€%.2f", v)
fun eur0(v: Double): String = String.format(Locale.ROOT, "€%,.0f", v)
fun n1(v: Double): String = String.format(Locale.ROOT, "%.1f", v)
fun n2(v: Double): String = String.format(Locale.ROOT, "%.2f", v)

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "2026-05" -> "May 2026". */
fun fmtYm(ym: String?): String {
    if (ym == null || ym.length < 7) return "—"
    val m = ym.substring(5, 7).toIntOrNull() ?: return ym
    return "${MONTHS[m - 1]} ${ym.substring(0, 4)}"
}

/** "2026-05" -> "May '26" (compact axis label). */
fun fmtYmShort(ym: String): String {
    if (ym.length < 7) return ym
    val m = ym.substring(5, 7).toIntOrNull() ?: return ym
    return "${MONTHS[m - 1]} '${ym.substring(2, 4)}"
}

fun monthName(mm: String): String =
    mm.toIntOrNull()?.let { MONTHS.getOrNull(it - 1) } ?: mm

fun prettyVendor(slug: String): String =
    slug.replace('_', ' ').replaceFirstChar { it.uppercase() }
