package ee.household.bills.core

import ee.household.bills.api.ApiFactory
import ee.household.bills.api.Bill
import ee.household.bills.api.MeterReading
import ee.household.bills.api.Series
import kotlinx.serialization.builtins.ListSerializer

/** Generic load-with-cache result. Offline is a designed state. */
sealed class Loaded<out T> {
    data object Loading : Loaded<Nothing>()
    data class Live<T>(val value: T) : Loaded<T>()
    data class Stale<T>(val value: T, val lastSyncMs: Long, val reason: String) : Loaded<T>()
    data class Empty(val reason: String) : Loaded<Nothing>()
    data object Unauthorized : Loaded<Nothing>()
}

/** Series + bills + meters with prefs-backed offline cache.
 *  (Room arrives in a later phase; prefs JSON is enough for read-only data.) */
class DataRepo(private val session: Session) {

    suspend fun series(): Loaded<Series> = load(
        key = "series_json",
        fetch = { ApiFactory.create(session).series() },
        ser = Series.serializer(),
    )

    suspend fun bills(): Loaded<List<Bill>> = load(
        key = "bills_json",
        fetch = { ApiFactory.create(session).bills() },
        ser = ListSerializer(Bill.serializer()),
    )

    suspend fun meters(): Loaded<List<MeterReading>> = load(
        key = "meters_json",
        fetch = { ApiFactory.create(session).meterReadings() },
        ser = ListSerializer(MeterReading.serializer()),
    )

    private suspend fun <T> load(
        key: String,
        fetch: suspend () -> T,
        ser: kotlinx.serialization.KSerializer<T>,
    ): Loaded<T> {
        return try {
            val v = fetch()
            session.putCache(key, ApiFactory.json.encodeToString(ser, v))
            Loaded.Live(v)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) Loaded.Unauthorized else cachedOr(key, ser, "server ${e.code()}")
        } catch (e: Exception) {
            cachedOr(key, ser, e.message ?: "offline")
        }
    }

    private fun <T> cachedOr(key: String, ser: kotlinx.serialization.KSerializer<T>, reason: String): Loaded<T> {
        val json = session.getCache(key) ?: return Loaded.Empty(reason)
        return try {
            Loaded.Stale(ApiFactory.json.decodeFromString(ser, json),
                session.cacheTime(key), reason)
        } catch (_: Exception) {
            Loaded.Empty(reason)
        }
    }
}
