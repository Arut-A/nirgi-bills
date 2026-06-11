package ee.household.bills.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.household.bills.core.Session

@Composable
fun SettingsScreen(
    session: Session,
    onSaved: () -> Unit,
    onSignOut: () -> Unit,
) {
    var serverUrl by remember { mutableStateOf(session.serverUrl) }
    var clientId by remember { mutableStateOf(session.webClientId) }

    Column(
        Modifier.fillMaxSize().background(Bg)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        if (session.isSignedIn) {
            Column(
                Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).padding(14.dp),
            ) {
                Text("ACCOUNT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(session.email ?: "signed in", color = TextMain, fontSize = 15.sp)
                Text("session until ${session.sessionExpiresAt?.take(10) ?: "?"} · renews silently",
                    color = Muted, fontSize = 12.sp)
            }
        }

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextMain, unfocusedTextColor = TextMain,
            focusedBorderColor = Primary, unfocusedBorderColor = Border,
            focusedLabelColor = Primary, unfocusedLabelColor = Muted,
            cursorColor = Primary,
        )

        OutlinedTextField(
            value = serverUrl, onValueChange = { serverUrl = it },
            label = { Text("Server URL (HTTPS)") },
            singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = clientId, onValueChange = { clientId = it },
            label = { Text("Google Web client ID") },
            supportingText = { Text("From Google Cloud Console → Credentials (the WEB client)",
                color = Muted, fontSize = 11.sp) },
            singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                session.serverUrl = serverUrl
                session.webClientId = clientId
                onSaved()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }

        if (session.isSignedIn) {
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = Up),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign out (wipes session + cache)") }
        }

        Text("Nirgi Bills 1.0.0 · Phase 1 walking skeleton\nHTTPS only · session in Android Keystore",
            color = Muted, fontSize = 11.sp)
    }
}
