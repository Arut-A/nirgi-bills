package ee.household.bills

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.household.bills.api.Bill
import ee.household.bills.api.MeterReading
import ee.household.bills.api.Series
import ee.household.bills.core.AuthFlow
import ee.household.bills.core.AuthResult
import ee.household.bills.core.DataRepo
import ee.household.bills.core.Loaded
import ee.household.bills.core.Session
import ee.household.bills.core.SummaryRepo
import ee.household.bills.core.SummaryState
import ee.household.bills.ui.Bg
import ee.household.bills.ui.BillsScreen
import ee.household.bills.ui.Card
import ee.household.bills.ui.CostsScreen
import ee.household.bills.ui.FilterSheet
import ee.household.bills.ui.FilterState
import ee.household.bills.ui.HomeScreen
import ee.household.bills.ui.MetersScreen
import ee.household.bills.ui.Muted
import ee.household.bills.ui.NirgiBillsTheme
import ee.household.bills.ui.Primary
import ee.household.bills.ui.SettingsScreen
import ee.household.bills.ui.SignInScreen
import ee.household.bills.ui.StaleBanner
import ee.household.bills.ui.Surface
import ee.household.bills.ui.TextMain
import ee.household.bills.ui.Up
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = Session(applicationContext)
        setContent { NirgiBillsTheme { App(session, AuthFlow(this, session)) } }
    }
}

private enum class Tab(val label: String, val emoji: String) {
    HOME("Home", "⌂"),
    COSTS("Costs", "€"),
    USAGE("Usage", "⚡"),
    BILLS("Bills", "🧾"),
    METERS("Meters", "🧮"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(session: Session, authFlow: AuthFlow) {
    val scope = rememberCoroutineScope()
    val summaryRepo = remember { SummaryRepo(session) }
    val dataRepo = remember { DataRepo(session) }
    val filters = remember { FilterState() }

    var signedIn by remember { mutableStateOf(session.isSignedIn) }
    var showSettings by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }

    var summary by remember { mutableStateOf<SummaryState>(SummaryState.Loading) }
    var series by remember { mutableStateOf<Loaded<Series>>(Loaded.Loading) }
    var bills by remember { mutableStateOf<Loaded<List<Bill>>>(Loaded.Loading) }

    var authBusy by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    fun onUnauthorized() {
        session.sessionToken = null
        signedIn = false
    }

    suspend fun loadTab(t: Tab) {
        when (t) {
            Tab.HOME -> { summary = summaryRepo.load(); if (summary is SummaryState.Unauthorized) onUnauthorized() }
            Tab.COSTS, Tab.USAGE, Tab.METERS -> {
                series = dataRepo.series()
                if (series is Loaded.Unauthorized) onUnauthorized()
                (series as? Loaded.Live)?.let { filters.initFrom(it.value) }
                (series as? Loaded.Stale)?.let { filters.initFrom(it.value) }
            }
            Tab.BILLS -> { bills = dataRepo.bills(); if (bills is Loaded.Unauthorized) onUnauthorized() }
        }
    }

    fun refresh() {
        scope.launch { refreshing = true; loadTab(Tab.entries[tab]); refreshing = false }
    }

    if (!signedIn) {
        if (showSettings) {
            SettingsScreen(session, onSaved = { showSettings = false }, onSignOut = { showSettings = false })
        } else {
            SignInScreen(
                busy = authBusy, error = authError,
                onSignIn = {
                    scope.launch {
                        authBusy = true; authError = null
                        when (val r = authFlow.signIn()) {
                            is AuthResult.Success -> { signedIn = true; refresh() }
                            is AuthResult.Failure -> authError = r.message
                        }
                        authBusy = false
                    }
                },
                onOpenSettings = { showSettings = true },
            )
        }
        return
    }

    if (showSettings) {
        SettingsScreen(
            session,
            onSaved = { showSettings = false },
            onSignOut = { session.signOut(); signedIn = false; showSettings = false },
        )
        return
    }

    val current = Tab.entries[tab]
    val filterable = current == Tab.COSTS || current == Tab.USAGE

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text(if (current == Tab.HOME) "Nirgi Bills" else current.label,
                    color = TextMain, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
                actions = {
                    if (filterable) IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Filled.List, "Filters", tint = Muted)
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, "Settings", tint = Muted)
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(t.emoji, fontSize = 17.sp,
                            color = if (tab == i) Primary else Muted) },
                        label = { Text(t.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = Primary, indicatorColor = Surface,
                            unselectedTextColor = Muted,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        // Load the tab's data on first visit / tab change.
        LaunchedEffect(tab) { loadTab(current) }

        PullToRefreshBox(
            isRefreshing = refreshing, onRefresh = { refresh() },
            modifier = Modifier.padding(pad),
        ) {
            when (current) {
                Tab.HOME -> HomeScreen(summary, onRetry = { refresh() })
                Tab.COSTS -> ScrollTab { LoadedContent(series) { CostsScreen(it, filters) } }
                Tab.USAGE -> ScrollTab { LoadedContent(series) { UsageScreenWrap(it, filters) } }
                Tab.BILLS -> ScrollTab { LoadedContent(bills) { BillsScreen(it, session) } }
                Tab.METERS -> ScrollTab { LoadedContent(series) { MetersScreen(it) } }
            }
        }
    }

    if (showFilters) {
        val s = (series as? Loaded.Live)?.value ?: (series as? Loaded.Stale)?.value
        if (s != null) FilterSheet(s, filters, onDismiss = { showFilters = false })
        else showFilters = false
    }
}

@Composable
private fun ScrollTab(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
    ) { content() }
}

/** Renders Loaded<T> with stale banner / loading / empty handling. */
@Composable
private fun <T> LoadedContent(state: Loaded<T>, content: @Composable (T) -> Unit) {
    when (state) {
        is Loaded.Loading -> Box(Modifier.fillMaxSize().padding(top = 100.dp),
            contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        is Loaded.Empty -> Card { Text("No data — ${state.reason}.", color = Muted) }
        is Loaded.Unauthorized -> Card { Text("Session expired — sign in again.", color = Up) }
        is Loaded.Live -> content(state.value)
        is Loaded.Stale -> { StaleBanner(state.lastSyncMs); content(state.value) }
    }
}

// Usage screen lives in ui package; thin wrapper keeps the import there.
@Composable
private fun UsageScreenWrap(s: Series, filters: FilterState) =
    ee.household.bills.ui.UsageScreen(s, filters)
