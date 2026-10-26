package app.gymapp.ui.train

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
    val workouts by Repo.workouts.collectAsState()
    val settings by Repo.settings.collectAsState()
    val outbox by Repo.outbox.collectAsState()
    val pending = outbox.flatMap { listOfNotNull(it.clientId, it.serverId) }.toSet()

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Gym", "Train") }
        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text(if (Draft.exercises.isEmpty()) "  Start workout" else "  Resume workout", style = MaterialTheme.typography.titleMedium)
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
                Muted("${w.exercises.sumOf { it.sets.size }} sets · ${formatWeight(w.volume())} ${settings.unit} volume")
            }
        }
    }
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
