package app.gymapp.ui.posture

import app.gymapp.ui.design.Accent
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.gymapp.data.Repo
import app.gymapp.domain.postureRoutine
import app.gymapp.domain.runnerStretches
import app.gymapp.domain.streak
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import kotlinx.coroutines.delay
import java.time.LocalDate

private const val READY_MS = 5_000L

/** Guided posture block: a 5-second "get ready" before each move, then a timed hold with cues. */
@Composable
fun PostureScreen(onDone: () -> Unit, onScan: () -> Unit, runner: Boolean = false) {
    val settings by Repo.settings.collectAsState()
    val days by Repo.postureDays.collectAsState()
    val moves = remember(settings.equipment, runner) { if (runner) runnerStretches() else postureRoutine(settings.equipment) }
    var started by rememberSaveable { mutableStateOf(false) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var ready by rememberSaveable { mutableStateOf(true) }
    var remainingMs by rememberSaveable { mutableLongStateOf(READY_MS) }
    var paused by rememberSaveable { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)
    // Spoken cues so the phone can sit on the floor during the routine.
    var muted by rememberSaveable { mutableStateOf(false) }
    val tts = remember { arrayOfNulls<TextToSpeech>(1) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) {}
        tts[0] = engine
        onDispose { engine.shutdown() }
    }
    fun say(text: String) {
        if (!muted) tts[0]?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue")
    }
    LaunchedEffect(started, index, ready, finished) {
        when {
            !started -> Unit
            finished -> say("Done. Shoulders back, chin tucked.")
            ready -> say("Next: " + moves[index].name + (moves[index].side?.let { ", $it" } ?: ""))
            else -> say("Go")
        }
    }
    fun buzz(ms: Long) = vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))

    fun goTo(i: Int) {
        index = i
        ready = true
        remainingMs = READY_MS
    }

    LaunchedEffect(started, paused, finished, index, ready) {
        if (!started || paused || finished) return@LaunchedEffect
        var last = SystemClock.elapsedRealtime()
        while (remainingMs > 0) {
            delay(100)
            val now = SystemClock.elapsedRealtime()
            remainingMs -= now - last
            last = now
        }
        when {
            ready -> {
                buzz(150)
                remainingMs = moves[index].seconds * 1000L
                ready = false
            }
            index < moves.lastIndex -> {
                buzz(300)
                goTo(index + 1)
            }
            else -> {
                buzz(600)
                if (!runner) Repo.logPosture()
                finished = true
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (started && !finished) "${index + 1} / ${moves.size}" else "Posture",
                    style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f),
                )
                if (started && !finished) IconButton(onClick = { muted = !muted }) {
                    Icon(if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = if (muted) "Unmute voice" else "Mute voice")
                }
                IconButton(onClick = onDone) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }
        }
        when {
            finished -> item {
                Panel {
                    Text("Done. Shoulders back, chin tucked.", style = MaterialTheme.typography.headlineSmall)
                    Muted("Posture streak: ${streak(days.map(LocalDate::parse).toSet())} day(s). Same time tomorrow.")
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Back to Today") }
                }
            }
            !started -> {
                item {
                    Panel {
                        Text(if (runner) "6 minutes to cool down after your run" else "10 minutes for forward head and rounded shoulders", style = MaterialTheme.typography.titleLarge)
                        Muted("Stretch the tight front, free the upper back, then strengthen the back of the shoulders. Moves adapt to the equipment you ticked.")
                        Muted("Stop any move that causes pain, tingling or numbness.")
                        if (!runner) TextButton(onClick = onScan) { Text("Check my posture with a photo") }
                    }
                }
                items(moves) { m ->
                    Row {
                        Text(m.name + (m.side?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Muted("${m.seconds}s")
                    }
                }
                item {
                    Button(onClick = { started = true }, modifier = Modifier.navigationBarsPadding().fillMaxWidth().height(56.dp)) {
                        Text("Start", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            else -> item {
                val move = moves[index]
                val total = if (ready) READY_MS else move.seconds * 1000L
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        val ring = Accent.Posture.color
                        val track = MaterialTheme.colorScheme.surfaceContainerHigh
                        Canvas(Modifier.size(220.dp)) {
                            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            drawArc(track, 0f, 360f, false, style = stroke)
                            drawArc(ring, -90f, 360f * (remainingMs.coerceAtLeast(0) / total.toFloat()), false, style = stroke)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(remainingMs.coerceAtLeast(0) + 999) / 1000}", style = MaterialTheme.typography.displayLarge)
                            Muted(if (ready) "get ready" else "seconds")
                        }
                    }
                    Panel {
                        if (ready) Muted("Next up")
                        Text(move.name, style = MaterialTheme.typography.headlineSmall)
                        move.side?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                        move.cues.forEach {
                            Row {
                                Text("•", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(20.dp))
                                Text(it, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Muted(move.why, Modifier.padding(top = 4.dp))
                    }
                    Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { goTo((index - 1).coerceAtLeast(0)) }) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous move") }
                        FilledIconButton(onClick = { paused = !paused }, modifier = Modifier.size(72.dp)) {
                            Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = if (paused) "Resume" else "Pause", modifier = Modifier.size(36.dp))
                        }
                        IconButton(onClick = {
                            if (index < moves.lastIndex) goTo(index + 1) else { if (!runner) Repo.logPosture(); finished = true }
                        }) { Icon(Icons.Rounded.SkipNext, contentDescription = "Next move") }
                    }
                }
            }
        }
    }
}
