package ee.household.bills.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ee.household.bills.R

/** Keystore-encrypted storage for the session JWT and app settings. */
class Session(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, "nirgi_session", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var sessionToken: String?
        get() = prefs.getString("session_token", null)
        set(v) = prefs.edit().putString("session_token", v).apply()

    var email: String?
        get() = prefs.getString("email", null)
        set(v) = prefs.edit().putString("email", v).apply()

    var sessionExpiresAt: String?
        get() = prefs.getString("expires_at", null)
        set(v) = prefs.edit().putString("expires_at", v).apply()

    var serverUrl: String
        get() = prefs.getString("server_url", null)
            ?: context.getString(R.string.default_server_url)
        set(v) = prefs.edit().putString("server_url", v.trimEnd('/')).apply()

    var webClientId: String
        get() = prefs.getString("web_client_id", null)
            ?: context.getString(R.string.default_web_client_id)
        set(v) = prefs.edit().putString("web_client_id", v.trim()).apply()

    /** Cached summary JSON + fetch time — Phase-1 offline support. */
    var cachedSummaryJson: String?
        get() = prefs.getString("summary_json", null)
        set(v) = prefs.edit().putString("summary_json", v).apply()

    var lastSyncEpochMs: Long
        get() = prefs.getLong("last_sync", 0L)
        set(v) = prefs.edit().putLong("last_sync", v).apply()

    val isSignedIn: Boolean get() = !sessionToken.isNullOrEmpty()

    // Generic cache (series/bills/meters JSON + per-key fetch time).
    fun putCache(key: String, json: String) =
        prefs.edit().putString(key, json).putLong("${key}_t", System.currentTimeMillis()).apply()
    fun getCache(key: String): String? = prefs.getString(key, null)
    fun cacheTime(key: String): Long = prefs.getLong("${key}_t", 0L)

    fun signOut() {
        prefs.edit().clear().apply()
    }
}
