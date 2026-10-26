package app.gymapp.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.gymapp.data.Repo
import app.gymapp.ui.glass.GlassLook
import app.gymapp.ui.posture.PostureScreen
import app.gymapp.ui.run.ActiveRunScreen
import app.gymapp.ui.run.RunScreen
import app.gymapp.ui.theme.LotusDark
import app.gymapp.ui.theme.LotusLight
import app.gymapp.ui.theme.LotusTypography
import app.gymapp.ui.today.TodayScreen
import app.gymapp.ui.train.LogScreen
import app.gymapp.ui.train.TrainScreen
import app.gymapp.ui.you.YouScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun GymApp() {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) LotusDark else LotusLight
    MaterialTheme(colorScheme = scheme, typography = LotusTypography) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface) {
            Box(Modifier.fillMaxSize().background(scheme.background)) { MainScaffold(dark) }
        }
    }
}

@Composable
private fun MainScaffold(dark: Boolean) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // A full-screen flow pushed over the tabs: "log" (session), "posture" (guided routine) or "running".
    var route by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = route != null) { route = null }
    val backdrop = rememberLayerBackdrop()
    val surface = MaterialTheme.colorScheme.surface
    val look = remember(dark, surface) { GlassLook.liquid(dark = dark, tint = surface) }

    // Flush the offline outbox as soon as the phone gets signal again.
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Repo.syncNow()
            }
        }
        cm.registerNetworkCallback(request, callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }

    val startWorkout = { route = "log" }
    val close = { route = null }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            Glow()
            when (route) {
                "log" -> LogScreen(onDone = close)
                "posture" -> PostureScreen(onDone = close)
                "running" -> ActiveRunScreen(onClose = close)
                else -> when (tab) {
                    0 -> TodayScreen(onStart = startWorkout, onPosture = { route = "posture" }, onRun = { tab = 2 })
                    1 -> TrainScreen(onStart = startWorkout)
                    2 -> RunScreen(onOpenRun = { route = "running" })
                    else -> YouScreen()
                }
            }
        }
        if (route == null) {
            BottomBar(
                selected = { tab },
                onSelect = { tab = it },
                backdrop = backdrop,
                look = look,
                isLight = !dark,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
