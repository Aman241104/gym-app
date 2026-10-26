package app.gymapp.ui.run

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.gymapp.domain.Phase
import app.gymapp.run.RunService
import app.gymapp.run.formatClock
import app.gymapp.run.formatPace
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.design.Accent
import androidx.compose.material3.ButtonDefaults

/** Live view of [RunService]; the run keeps going if this screen is left or the phone locks. */
@Composable
fun ActiveRunScreen(onClose: () -> Unit, onStretch: () -> Unit = {}) {
    val context = LocalContext.current
    val s by RunService.state.collectAsState()
    var confirmStop by remember { mutableStateOf(false) }

    s.finished?.let { run ->
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (run.completed) "Run complete" else "Run saved", style = MaterialTheme.typography.displaySmall)
            Panel {
                Stat("Distance", "%.2f km".format(run.distanceM / 1000.0))
                Stat("Time", formatClock(run.durationSec.toLong()))
                Stat("Pace", formatPace(run.durationSec.toLong(), run.distanceM.toDouble()))
            }
            Muted("Stretch your calves, hip flexors and hamstrings now while you're warm.")
            Button(
                onClick = { RunService.dismissSummary(); onStretch() },
                colors = ButtonDefaults.buttonColors(containerColor = Accent.Run.color),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Stretch now · 6 min") }
            TextButton(onClick = { RunService.dismissSummary(); onClose() }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
        return
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.session?.let { "Week ${it.week} · run ${it.day}" } ?: "Free run", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Hide") }
        }
        val interval = s.interval
        Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
            if (interval != null) {
                val color = if (interval.phase == Phase.Run) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                val track = MaterialTheme.colorScheme.surfaceContainerHigh
                Canvas(Modifier.size(240.dp)) {
                    val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(track, 0f, 360f, false, style = stroke)
                    drawArc(color, -90f, 360f * (s.phaseLeftMs.coerceAtLeast(0) / (interval.seconds * 1000f)), false, style = stroke)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(interval?.phase?.label ?: "Running", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    if (interval != null) formatClock((s.phaseLeftMs.coerceAtLeast(0) + 999) / 1000) else formatClock(s.elapsedMs / 1000),
                    style = MaterialTheme.typography.displayLarge,
                )
                s.next?.let { Muted("then ${it.phase.label.lowercase()} ${formatClock(it.seconds.toLong())}") }
            }
        }
        Panel {
            Stat("Distance", "%.2f km".format(s.distanceM / 1000))
            Stat("Time", formatClock(s.elapsedMs / 1000))
            Stat("Pace", formatPace(s.elapsedMs / 1000, s.distanceM))
            if (!s.gps) Muted("Waiting for GPS. The timer works without it.")
        }
        if (s.paused) Muted("Paused. Distance isn't counted while paused.")
        Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            OutlinedIconButton(onClick = { confirmStop = true }, modifier = Modifier.size(64.dp)) { Icon(Icons.Rounded.Stop, contentDescription = "End run") }
            FilledIconButton(onClick = RunService::togglePause, modifier = Modifier.size(88.dp)) {
                Icon(if (s.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = if (s.paused) "Resume" else "Pause", modifier = Modifier.size(44.dp))
            }
        }
    }
    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("End this run?") },
            text = { Text(if (s.session != null) "It's saved, but it won't count as finishing this Couch-to-5K step." else "Your run will be saved.") },
            confirmButton = { TextButton(onClick = { confirmStop = false; RunService.stop(context) }) { Text("End run") } },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Keep going") } },
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row {
        Muted(label, Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
