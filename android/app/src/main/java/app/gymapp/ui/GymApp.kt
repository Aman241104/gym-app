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
import app.gymapp.ui.eat.EatScreen
import app.gymapp.ui.mind.BreatheScreen
import app.gymapp.ui.read.ReadScreen
import app.gymapp.ui.skin.SkinScreen
import app.gymapp.ui.pose.PoseCoachScreen
import app.gymapp.ui.design.Aurora
import app.gymapp.ui.design.Confetti
import androidx.compose.ui.unit.dp
import app.gymapp.domain.formatWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import app.gymapp.ui.design.Accent
import app.gymapp.ui.train.Celebrate
import app.gymapp.ui.train.Draft
import app.gymapp.ui.train.SessionDock
import app.gymapp.domain.lastByExercise
import app.gymapp.run.RunService
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.collectAsState
import java.time.LocalDate
import app.gymapp.ui.design.StatusScrim
import app.gymapp.ui.glass.GlassLook
import app.gymapp.ui.posture.PostureScanScreen
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
    // Dock button -> screen hand-offs for actions that live inside a screen (permissions, scanner).
    var runSignal by remember { mutableIntStateOf(0) }
    var scanSignal by remember { mutableIntStateOf(0) }
    val planState = rememberWeekPlan()
    val workouts by Repo.workouts.collectAsState()
    val settings by Repo.settings.collectAsState()
    val live by RunService.state.collectAsState()
    val action = when (tab) {
        0 -> DockAction(Icons.Rounded.PlayArrow, "Start today's session", Accent.Train.color) {
            val day = planState?.first?.on(LocalDate.now().dayOfWeek)
            if (Draft.exercises.isEmpty() && day != null) Draft.loadPlan(day, lastByExercise(workouts), settings.unit)
            route = "log"
        }
        1 -> DockAction(Icons.Rounded.Add, "New workout", Accent.Train.color) { route = "log" }
        2 -> DockAction(Icons.AutoMirrored.Rounded.DirectionsRun, if (live.active) "Open run" else "Start run", Accent.Run.color) {
            if (live.active) route = "running" else runSignal++
        }
        3 -> DockAction(Icons.Rounded.QrCodeScanner, "Scan a barcode", Accent.Eat.color) { scanSignal++ }
        else -> DockAction(Icons.Rounded.Sync, "Sync now", Accent.Mind.color) { Repo.syncNow() }
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            Aurora()
            when (route) {
                "log" -> LogScreen(onDone = close)
                "posture" -> PostureScreen(onDone = close, onScan = { route = "scan" })
                "scan" -> PostureScanScreen(onBack = { route = "posture" })
                "running" -> ActiveRunScreen(onClose = close, onStretch = { route = "stretch" })
                "stretch" -> PostureScreen(onDone = close, onScan = {}, runner = true)
                "breathe" -> BreatheScreen(onClose = close)
                "read" -> ReadScreen(onClose = close)
                "skin" -> SkinScreen(onClose = close)
                "pose" -> PoseCoachScreen(onClose = close)
                else -> when (tab) {
                    0 -> TodayScreen(onStart = startWorkout, onPosture = { route = "posture" }, onRun = { tab = 2 }, onEat = { tab = 3 }, onBreathe = { route = "breathe" }, onRead = { route = "read" }, onSkin = { route = "skin" })
                    1 -> TrainScreen(onStart = startWorkout, onPose = { route = "pose" })
                    2 -> RunScreen(onOpenRun = { route = "running" }, startSignal = runSignal, onStartHandled = { runSignal = 0 })
                    3 -> EatScreen(scanSignal = scanSignal, onScanHandled = { scanSignal = 0 })
                    else -> YouScreen()
                }
            }
        }
        StatusScrim(Modifier.align(Alignment.TopCenter))
        if (route == null) {
            BottomBar(
                selected = { tab },
                onSelect = { tab = it },
                backdrop = backdrop,
                look = look,
                isLight = !dark,
                action = action,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else if (route == "log") {
            SessionDock(backdrop, workouts, onFinished = close, modifier = Modifier.align(Alignment.BottomCenter))
        }
        if (Celebrate.records.isNotEmpty()) PrCelebration()
    }
}

/** Confetti + a card listing the new records; dismisses itself after a few seconds or on tap. */
@Composable
private fun PrCelebration() {
    val context = LocalContext.current
    LaunchedEffect(Celebrate.records) {
        context.getSystemService(android.os.Vibrator::class.java)
            ?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 60, 80, 60, 80, 220), -1))
        kotlinx.coroutines.delay(4500)
        Celebrate.records = emptyList()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Confetti(listOf(Accent.Train.color, Accent.Run.color, Accent.Eat.color, Accent.Posture.color, Accent.Mind.color))
        Panel(Modifier.padding(32.dp), accent = Accent.Train.color, onClick = { Celebrate.records = emptyList() }) {
            Text("New personal record", style = MaterialTheme.typography.labelMedium, color = Accent.Train.color)
            Celebrate.records.forEach { pr ->
                Text("${pr.exercise}  ${formatWeight(pr.weight)}", style = MaterialTheme.typography.headlineSmall)
                Muted("Previous best ${formatWeight(pr.previous)}")
            }
        }
    }
}
