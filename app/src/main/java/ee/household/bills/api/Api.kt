package ee.household.bills.api

import ee.household.bills.core.Session
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class AuthRequest(@SerialName("id_token") val idToken: String)

@Serializable
data class AuthResponse(
    @SerialName("session_token") val sessionToken: String,
    @SerialName("expires_at") val expiresAt: String,
    val email: String,
)

@Serializable
data class MonthTotal(val month: String, val total: Double)

@Serializable
data class CategoryAmount(val category: String, val amount: Double, val pct: Double)

@Serializable
data class UnitCost(
    val value: Double,
    val month: String,
    val baseline: Double? = null,
    val pct: Double? = null,
)

@Serializable
data class Deviation(
    val category: String,
    val latest: Double,
    val baseline: Double,
    val delta: Double,
    val pct: Double? = null,
    val anomaly: Boolean = false,
)

@Serializable
data class Summary(
    @SerialName("cutoff_ym") val cutoffYm: String,
    @SerialName("month_to_date") val monthToDate: Double,
    @SerialName("month_to_date_bills") val monthToDateBills: Int,
    @SerialName("latest_month") val latestMonth: String? = null,
    @SerialName("latest_total") val latestTotal: Double? = null,
    @SerialName("prev_month") val prevMonth: String? = null,
    @SerialName("mom_pct") val momPct: Double? = null,
    @SerialName("monthly_avg_12") val monthlyAvg12: Double? = null,
    @SerialName("avg_months_used") val avgMonthsUsed: Int,
    @SerialName("grand_total") val grandTotal: Double,
    @SerialName("months_count") val monthsCount: Int,
    @SerialName("top_category") val topCategory: String? = null,
    @SerialName("top_category_total") val topCategoryTotal: Double? = null,
    @SerialName("breakdown_latest") val breakdownLatest: List<CategoryAmount>,
    @SerialName("last_12_months") val last12Months: List<MonthTotal>,
    @SerialName("unit_costs") val unitCosts: Map<String, UnitCost>,
    @SerialName("baseline_total") val baselineTotal: Double? = null,
    @SerialName("latest_vs_baseline_pct") val latestVsBaselinePct: Double? = null,
    val deviations: List<Deviation> = emptyList(),
)

@Serializable
data class Consumption(
    @SerialName("elec_kwh") val elecKwh: Map<String, Double> = emptyMap(),
    @SerialName("gas_m3") val gasM3: Map<String, Double> = emptyMap(),
    @SerialName("gas_kwh") val gasKwh: Map<String, Double> = emptyMap(),
    @SerialName("water_m3") val waterM3: Map<String, Double> = emptyMap(),
)

@Serializable
data class MonthUnitCosts(
    @SerialName("elec_s_kwh") val elecSKwh: Double? = null,
    @SerialName("gas_s_kwh") val gasSKwh: Double? = null,
    @SerialName("gas_s_m3") val gasSM3: Double? = null,
    @SerialName("water_eur_m3") val waterEurM3: Double? = null,
    @SerialName("pellets_s_kwh") val pelletsSKwh: Double? = null,
)

@Serializable
data class Series(
    @SerialName("cutoff_ym") val cutoffYm: String,
    val vendors: List<String>,
    val months: List<String>,
    val data: Map<String, Map<String, Double>>,
    val consumption: Consumption,
    @SerialName("unit_costs") val unitCosts: Map<String, MonthUnitCosts>,
)

@Serializable
data class Bill(
    val id: String? = null,   // UUID string from the server, not an int
    @SerialName("vendor_category") val vendorCategory: String,
    @SerialName("invoice_date") val invoiceDate: String? = null,
    @SerialName("billing_period_start") val periodStart: String? = null,
    @SerialName("billing_period_end") val periodEnd: String? = null,
    @SerialName("total_amount") val totalAmount: Double? = null,
    @SerialName("energy_kwh") val energyKwh: Double? = null,
    @SerialName("gas_m3") val gasM3: Double? = null,
    @SerialName("water_m3") val waterM3: Double? = null,
    @SerialName("raw_pdf_path") val rawPdfPath: String? = null,
    @SerialName("cost_month") val costMonth: String? = null,  // attributed month (server, dashboard-consistent)
) {
    /** Bare filename for the public /bills/{file} endpoint, or null. */
    val pdfFile: String?
        get() = rawPdfPath
            ?.takeIf { it.isNotBlank() && it != "manual_entry" && !it.startsWith("email:") }
            ?.substringAfterLast('/')
}

@Serializable
data class MeterReading(
    val id: Int? = null,
    @SerialName("meter_id") val meterId: String? = null,
    @SerialName("vendor_category") val vendorCategory: String? = null,
    @SerialName("reading_date") val readingDate: String? = null,
    @SerialName("reading_value") val readingValue: Double? = null,
    @SerialName("unit_type") val unitType: String? = null,
)

interface BillsApi {
    @POST("api/auth/google")
    suspend fun authGoogle(@Body body: AuthRequest): AuthResponse

    @GET("api/bills/summary")
    suspend fun summary(): Summary

    @GET("api/bills/series")
    suspend fun series(): Series

    @GET("api/bills")
    suspend fun bills(@Query("limit") limit: Int = 500): List<Bill>

    @GET("api/meter-readings")
    suspend fun meterReadings(@Query("limit") limit: Int = 500): List<MeterReading>
}

object ApiFactory {
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /** Built per-call site; cheap enough for a single-user app and always
     *  picks up the current server URL + session token. */
    fun create(session: Session): BillsApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = session.sessionToken
                val req = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token").build()
                } else chain.request()
                chain.proceed(req)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(session.serverUrl + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BillsApi::class.java)
    }
}
