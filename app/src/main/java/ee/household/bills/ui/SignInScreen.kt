package ee.household.bills.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignInScreen(
    busy: Boolean,
    error: String?,
    onSignIn: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Bg).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(
                    Brush.linearGradient(listOf(Primary, Secondary)),
                    RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) { Text("🧾", fontSize = 40.sp) }

        Spacer(Modifier.height(18.dp))
        Text("Nirgi Bills", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Home utility bills", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(40.dp))

        if (busy) {
            CircularProgressIndicator(color = Primary)
        } else {
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, contentColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text("Sign in with Google", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Only the household account is authorised.\nAny other Google account is rejected by the server.",
            color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center,
        )
        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(error, color = Up, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onOpenSettings) {
            Text("Settings", color = MaterialTheme.colorScheme.primary)
        }
    }
}
