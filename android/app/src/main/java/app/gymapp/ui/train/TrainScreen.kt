package app.gymapp.ui.train

import app.gymapp.domain.muscleBalance
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import app.gymapp.ui.design.Accent
import app.gymapp.ui.design.rise
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.gymapp.data.Workout
import app.gymapp.domain.LibExercise
import app.gymapp.ui.library.ExerciseSheet
import app.gymapp.ui.rememberWeekPlan
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.rounded.SwapHoriz
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.gymapp.data.Repo
import app.gymapp.domain.formatWeight
import app.gymapp.domain.localDate
import app.gymapp.domain.volume
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.ScreenHeader
import app.gymapp.ui.ScreenPadding
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM")

@Composable
fun TrainScreen(onStart: () -> Unit) {
    var confirmDelete by remember { mutableStateOf<Workout?>(null) }
    var detail by remember { mutableStateOf<LibExercise?>(null) }
    val planState = rememberWeekPlan()
    val library by Repo.library.collectAsState()
    val workouts by Repo.workouts.collectAsState()
    val settings by Repo.settings.collectAsState()
    val outbox by Repo.outbox.collectAsState()
    val pending = outbox.flatMap { listOfNotNull(it.clientId, it.serverId) }.toSet()

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Gym", "Train", "smart", Accent.Train.color) }
        item {
            Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = Accent.Train.color), modifier = Modifier.fillMaxWidth().height(56.dp).rise()) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text(if (Draft.exercises.isEmpty()) "  Start workout" else "  Resume workout", style = MaterialTheme.typography.titleMedium)
            }
        }
        planState?.let { (plan, week) ->
            item {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("This week", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Muted("Week ${week + 1}" + if (plan.deload) " · deload" else "")
                }
            }
            if (settings.equipment.isEmpty()) item { Muted("Tick your equipment on the You tab to get a plan that fits your gym.") }
            items(plan.days, key = { "plan-${it.day}" }) { day ->
                Panel(accent = Accent.Train.color) {
                    Text(
                        "${day.day.getDisplayName(TextStyle.FULL, Locale.getDefault())} · ${day.title}",
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                    )
                    day.exercises.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { detail = p.exercise }) {
                                Text(p.exercise.name, style = MaterialTheme.typography.bodyLarge)
                                Muted("${p.slot.label} · ${p.sets} × ${p.reps}")
                            }
                            if (p.options > 1) {
                                IconButton(onClick = { Repo.swap(p.slot.key) }) {
                                    Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap ${p.exercise.name}")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (workouts.isNotEmpty() && library.isNotEmpty()) item {
            val balance = muscleBalance(workouts, library)
            val max = balance.maxOf { it.second }.coerceAtLeast(1.0)
            val accent = Accent.Train.color
            Panel {
                Text("Muscle balance", style = MaterialTheme.typography.titleLarge)
                Muted("Sets per area in the last 7 days. Grey means neglected.")
                balance.forEach { (region, sets) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Text(region, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(0.32f))
                        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(accent.copy(alpha = 0.12f))) {
                            Box(Modifier.fillMaxWidth((sets / max).toFloat()).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(accent.copy(alpha = 0.45f + 0.55f * (sets / max).toFloat())))
                        }
                        Muted(if (sets == 0.0) "none" else "%.0f".format(sets), Modifier.padding(start = 10.dp).fillMaxWidth(0.12f))
                    }
                }
            }
        }
        item { Text("History", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
        if (workouts.isEmpty()) item { Muted("Nothing logged yet.") }
        items(workouts.sortedByDescending { it.date }, key = { it.key }) { w ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(w.localDate().format(dayFormat), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (w.clientId in pending || w.serverId in pending) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = "Waiting to sync", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // Editing reuses the session draft, so it's offered only when no session is in progress.
                    if (Draft.exercises.isEmpty()) {
                        IconButton(onClick = { Draft.load(w); onStart() }) { Icon(Icons.Rounded.Edit, contentDescription = "Edit") }
                    }
                    IconButton(onClick = { confirmDelete = w }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete") }
                }
                Text(w.exercises.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodyLarge)
                val sets = w.exercises.sumOf { it.sets.size }
                Muted("$sets ${if (sets == 1) "set" else "sets"} · ${formatWeight(w.volume())} ${settings.unit} volume")
            }
        }
    }
    detail?.let { ExerciseSheet(it, onDismiss = { detail = null }) }
    confirmDelete?.let { w ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this session?") },
            text = { Text(w.exercises.joinToString(" · ") { it.name }) },
            confirmButton = { TextButton(onClick = { Repo.deleteWorkout(w); confirmDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Keep") } },
        )
    }
}
