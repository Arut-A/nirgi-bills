package ee.household.bills.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ee.household.bills.api.Series

/** Range-preset + category filter. Presets are one-tap and always visible
 *  (12M / 24M / YTD / each year / All); categories toggle via the legend or
 *  the filter sheet. This replaces the buried year+month chip grid — the
 *  rolling-window range is the comfortable 90% case for phone analysis. */
class FilterState {
    val categories = mutableStateMapOf<String, Boolean>()
    var preset by mutableStateOf("YTD")   // default range on Costs & Usage
    private var inited = false

    fun initFrom(s: Series) {
        if (inited) return
        s.vendors.forEach { categories[it] = true }
        inited = true
    }

    fun presets(s: Series): List<String> {
        val years = s.months.map { it.take(4) }.distinct().sortedDescending()
        return listOf("12M", "24M", "YTD") + years + "All"
    }

    fun months(s: Series): List<String> = when (val p = preset) {
        "All" -> s.months
        "12M" -> s.months.takeLast(12)
        "24M" -> s.months.takeLast(24)
        "YTD" -> s.months.lastOrNull()?.take(4)?.let { y -> s.months.filter { it.take(4) == y } } ?: s.months
        else -> s.months.filter { it.take(4) == p }   // a specific year
    }

    fun activeCategories(s: Series): List<String> = s.vendors.filter { categories[it] != false }

    val allCategoriesActive: Boolean
        get() = categories.values.all { it }

    /** Toggle a category, but never let the last one turn off. */
    fun toggleCategory(v: String, s: Series) {
        val active = s.vendors.count { categories[it] != false }
        if (categories[v] != false && active <= 1) return
        categories[v] = categories[v] == false
    }
}
