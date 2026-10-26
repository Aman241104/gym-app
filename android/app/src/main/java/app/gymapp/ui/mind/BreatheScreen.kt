package app.gymapp.ui.mind

import android.os.SystemClock
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.gymapp.data.Repo
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.ScreenHeader
import app.gymapp.ui.design.Accent
import kotlinx.coroutines.delay

/** A breathing pattern as (label, seconds) steps; "In" grows the circle, "Out" shrinks it, holds keep it. */
data class Pattern(val name: String, val why: String, val steps: List<Pair<String, Float>>)

val Patterns = listOf(
    Pattern("Box", "Calm focus before training or work.", listOf("In" to 4f, "Hold" to 4f, "Out" to 4f, "Hold" to 4f)),
    Pattern("4-7-8", "Winding down for sleep.", listOf("In" to 4f, "Hold" to 7f, "Out" to 8f)),
    Pattern("Coherent", "Steady, even breathing for everyday stress.", listOf("In" to 5.5f, "Out" to 5.5f)),
)

private val lengths = listOf(2, 5, 10)

@Composable
fun BreatheScreen(onClose: () -> Unit) {
    val sky = Accent.Mind.color
    var pattern by rememberSaveable { mutableIntStateOf(0) }
    var minutes by rememberSaveable { mutableIntStateOf(5) }
    var running by rememberSaveable { mutableStateOf(false) }
    var elapsed by rememberSaveable { mutableLongStateOf(0L) }
    var done by rememberSaveable { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }
    val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)

    val p = Patterns[pattern]
    val cycle = p.steps.sumOf { it.second.toDouble() }.toFloat() * 1000
    val inCycle = (elapsed % cycle.toLong()).toFloat()
    val stepIndex = stepAt(p, elapsed)
    val acc = p.steps.take(stepIndex).sumOf { it.second.toDouble() * 1000 }.toFloat()
    val step = p.steps[stepIndex]
    val t = ((inCycle - acc) / (step.second * 1000)).coerceIn(0f, 1f)
    val prevSize = p.steps.take(stepIndex).fold(0.35f) { size, s -> when (s.first) { "In" -> 1f; "Out" -> 0.35f; else -> size } }
    val scale = when (step.first) {
        "In" -> 0.35f + 0.65f * easeInOut(t)
        "Out" -> 1f - 0.65f * easeInOut(t)
        else -> prevSize
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = SystemClock.elapsedRealtime()
        var lastStep = -1
        while (running) {
            delay(16)
            val now = SystemClock.elapsedRealtime()
            elapsed += now - last
            last = now
            val current = stepAt(p, elapsed)
            if (current != lastStep) {
                lastStep = current
                vibrator?.vibrate(VibrationEffect.createOneShot(if (p.steps[current].first == "In") 60 else 25, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            if (elapsed >= minutes * 60_000L) {
                running = false
                done = true
                Repo.logMind(minutes)
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) { ScreenHeader("Mind", "Just", "breathe", sky) }
                IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(top = 20.dp)) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(280.dp)) {
                    val r = size.minDimension / 2
                    drawCircle(sky.copy(alpha = 0.08f), r)
                    drawCircle(Brush.radialGradient(listOf(sky.copy(alpha = 0.9f), sky.copy(alpha = 0.25f)), center, r * scale), r * scale)
                    drawCircle(Color.White.copy(alpha = 0.18f), r * scale * 0.55f, center + Offset(-r * scale * 0.2f, -r * scale * 0.25f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (done) "Done" else if (running) step.first else p.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (running) Muted("%d:%02d left".format(((minutes * 60_000L - elapsed) / 1000) / 60, ((minutes * 60_000L - elapsed) / 1000) % 60))
                }
            }
        }
        if (done) item {
            Panel(accent = sky) {
                Text("$minutes minutes of breathing", style = MaterialTheme.typography.titleLarge)
                Muted("Notice how your shoulders feel. Let them drop.")
            }
        }
        if (!running) {
            item { Text("Pattern", style = MaterialTheme.typography.titleMedium) }
            items(Patterns.size) { i ->
                val it = Patterns[i]
                Panel(accent = if (i == pattern) sky else null, onClick = { pattern = i; done = false; elapsed = 0 }) {
                    val rhythm = it.steps.joinToString("-") { s -> if (s.second % 1 == 0f) "%.0f".format(s.second) else "%.1f".format(s.second) }
                    Text(if (rhythm == it.name) it.name else "${it.name}  ·  $rhythm", style = MaterialTheme.typography.titleMedium, color = if (i == pattern) sky else MaterialTheme.colorScheme.onSurface)
                    Muted(it.why)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lengths.forEach { m ->
                        FilterChip(
                            selected = minutes == m, onClick = { minutes = m }, label = { Text("$m min") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = sky.copy(alpha = 0.25f)),
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    if (running) running = false else { done = false; elapsed = 0; running = true }
                },
                colors = ButtonDefaults.buttonColors(containerColor = sky),
                modifier = Modifier.fillMaxWidth().height(56.dp).navigationBarsPadding(),
            ) { Text(if (running) "Stop" else "Begin", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

private fun stepAt(p: Pattern, elapsed: Long): Int {
    val cycle = (p.steps.sumOf { it.second.toDouble() } * 1000).toLong()
    var t = (elapsed % cycle).toFloat()
    p.steps.forEachIndexed { i, s ->
        if (t < s.second * 1000) return i
        t -= s.second * 1000
    }
    return p.steps.lastIndex
}

private fun easeInOut(t: Float) = if (t < 0.5f) 2 * t * t else 1 - (-2 * t + 2) * (-2 * t + 2) / 2
