package app.gymapp.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.gymapp.data.Repo
import app.gymapp.domain.formatWeight
import app.gymapp.domain.localDate
import app.gymapp.domain.sessionsInLast7Days
import app.gymapp.domain.streak
import app.gymapp.domain.suggest
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.ScreenHeader
import app.gymapp.ui.ScreenPadding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TodayScreen(onStart: () -> Unit, onPosture: () -> Unit) {
    val postureDays by Repo.postureDays.collectAsState()
    val workouts by Repo.workouts.collectAsState()
    val settings by Repo.settings.collectAsState()
    val today = LocalDate.now()
    val last = workouts.maxByOrNull { it.date }
    val suggestions = last?.exercises.orEmpty().mapNotNull { suggest(it, settings.unit) }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader(today.format(DateTimeFormatter.ofPattern("EEEE, d MMM")), "Today") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val daysSince = last?.let { ChronoUnit.DAYS.between(it.localDate(), today) }
                Stat("Streak", "${streak(workouts, today)}d", Modifier.weight(1f))
                Stat("Last 7 days", "${sessionsInLast7Days(workouts, today)}", Modifier.weight(1f))
                Stat("Last lift", daysSince?.let { if (it == 0L) "today" else "${it}d ago" } ?: "—", Modifier.weight(1f))
            }
        }
        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text("  Start workout", style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            val doneToday = today.toString() in postureDays
            Panel(Modifier.clickable(onClick = onPosture)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Posture · 10 min", style = MaterialTheme.typography.titleLarge)
                        Muted(if (doneToday) "Done today. Streak ${streak(postureDays.map(LocalDate::parse).toSet(), today)}d." else "Chin tucks, chest opener, upper-back work. Guided step by step.")
                    }
                    Icon(
                        if (doneToday) Icons.Rounded.CheckCircle else Icons.Rounded.SelfImprovement,
                        contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
        if (suggestions.isNotEmpty()) item {
            Panel {
                Text("Next time", style = MaterialTheme.typography.titleLarge)
                suggestions.forEach { s ->
                    Text("${s.exercise}  ·  ${formatWeight(s.weight)} ${settings.unit} × ${s.reps}", style = MaterialTheme.typography.titleSmall)
                    Muted(s.reason)
                }
            }
        }
        if (workouts.isEmpty()) item {
            Panel {
                Text("Log your first session", style = MaterialTheme.typography.titleLarge)
                Muted("Tap Start workout. Sessions save offline and sync when you have signal.")
            }
        }
        if (settings.equipment.isEmpty()) item {
            Panel {
                Text("What's in your gym?", style = MaterialTheme.typography.titleLarge)
                Muted("Tick your equipment on the You tab. Plans will only use what you have.")
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Panel(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Muted(label)
    }
}
