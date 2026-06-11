package ee.household.bills.core

import ee.household.bills.api.ApiFactory
import ee.household.bills.api.Summary

sealed class SummaryState {
    data object Loading : SummaryState()
    /** Fresh from the server. */
    data class Live(val summary: Summary) : SummaryState()
    /** Server unreachable — showing cache. Offline is a designed state. */
    data class Stale(val summary: Summary, val lastSyncMs: Long, val reason: String) : SummaryState()
    /** No data at all (first run offline). */
    data class Empty(val reason: String) : SummaryState()
    /** Session rejected — must sign in again. */
    data object Unauthorized : SummaryState()
}

class SummaryRepo(private val session: Session) {

    suspend fun load(): SummaryState {
        try {
            val s = ApiFactory.create(session).summary()
            session.cachedSummaryJson = ApiFactory.json.encodeToString(Summary.serializer(), s)
            session.lastSyncEpochMs = System.currentTimeMillis()
            return SummaryState.Live(s)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) return SummaryState.Unauthorized
            return cachedOr("Server error ${e.code()}")
        } catch (e: Exception) {
            return cachedOr(e.message ?: "offline")
        }
    }

    private fun cachedOr(reason: String): SummaryState {
        val json = session.cachedSummaryJson ?: return SummaryState.Empty(reason)
        return try {
            val s = ApiFactory.json.decodeFromString(Summary.serializer(), json)
            SummaryState.Stale(s, session.lastSyncEpochMs, reason)
        } catch (_: Exception) {
            SummaryState.Empty(reason)
        }
    }
}
