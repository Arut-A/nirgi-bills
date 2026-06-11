package ee.household.bills.core

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import ee.household.bills.api.ApiFactory
import ee.household.bills.api.AuthRequest

sealed class AuthResult {
    data class Success(val email: String) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/** "Sign in with Google" via Credential Manager, then exchange the Google
 *  ID token for a bills_api session JWT. */
class AuthFlow(private val context: Context, private val session: Session) {

    suspend fun signIn(): AuthResult {
        val clientId = session.webClientId
        if (clientId.isBlank()) {
            return AuthResult.Failure(
                "Web client ID not set — paste it in Settings (from Google Cloud Console)")
        }
        val idToken = try {
            val option = GetSignInWithGoogleOption.Builder(clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val result = CredentialManager.create(context)
                .getCredential(context, request)
            GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        } catch (e: Exception) {
            return AuthResult.Failure("Google sign-in failed: ${e.message ?: e.javaClass.simpleName}")
        }

        return try {
            val resp = ApiFactory.create(session).authGoogle(AuthRequest(idToken))
            session.sessionToken = resp.sessionToken
            session.email = resp.email
            session.sessionExpiresAt = resp.expiresAt
            AuthResult.Success(resp.email)
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                403 -> AuthResult.Failure("This Google account is not authorised for this household")
                429 -> AuthResult.Failure("Too many attempts — wait a minute")
                503 -> AuthResult.Failure("Server auth not configured yet (GOOGLE_WEB_CLIENT_ID missing in .env)")
                else -> AuthResult.Failure("Server error ${e.code()}")
            }
        } catch (e: Exception) {
            AuthResult.Failure("Cannot reach server: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
