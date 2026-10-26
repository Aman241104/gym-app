package app.gymapp.ui.run

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.gymapp.data.Repo
import app.gymapp.domain.C25k
import app.gymapp.domain.nextRun
import app.gymapp.domain.runDays
import app.gymapp.run.RunService
import app.gymapp.run.formatClock
import app.gymapp.run.formatPace
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.ScreenHeader
import app.gymapp.ui.ScreenPadding
import app.gymapp.ui.rememberWeekPlan
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM")

@Composable
fun RunScreen(onOpenRun: () -> Unit) {
    val context = LocalContext.current
    val runs by Repo.runs.collectAsState()
    val live by RunService.state.collectAsState()
    val done = runs.filter { it.completed && it.plan != null }.mapNotNull { it.plan }.toSet()
    val next = nextRun(done)
    val liftDays = rememberWeekPlan()?.first?.days?.map { it.day }?.toSet().orEmpty()
    var pending by remember { mutableStateOf<String?>(null) }

    // Ask for GPS + notification permission once, then start whatever was tapped (GPS is optional).
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        RunService.start(context, pending)
        onOpenRun()
    }
    fun begin(code: String?) {
        pending = code
        permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS))
    }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader("Couch to 5K", "Run") }
        if (live.active) item {
            Button(onClick = onOpenRun, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Run in progress · open", style = MaterialTheme.typography.titleMedium) }
        } else {
            item {
                Panel {
                    Text("Next: Week ${next.week}, run ${next.day}", style = MaterialTheme.typography.titleLarge)
                    Muted("${next.totalSeconds / 60} min total · ${next.runSeconds / 60} min running")
                    Text(next.summary, style = MaterialTheme.typography.bodyLarge)
                    Muted("5-minute walk to warm up and to cool down. Run slowly enough to talk.")
                    Button(onClick = { begin(next.code) }, modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 6.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null)
                        Text("  Start run", style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = { begin(null) }) { Text("Free run (no intervals)") }
                }
            }
        }
        item {
            Panel {
                Text("Progress", style = MaterialTheme.typography.titleMedium)
                Muted("Run days: " + runDays(liftDays).joinToString { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) } + " (away from lifting where possible)")
                C25k.chunked(3).forEachIndexed { w, sessions ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Muted("Week ${w + 1}", Modifier.padding(end = 4.dp).fillMaxWidth(0.25f))
                        sessions.forEach { s ->
                            val color = when {
                                s.code in done -> MaterialTheme.colorScheme.primary
                                s.code == next.code -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                            Box(Modifier.size(18.dp).clip(CircleShape).background(color))
                        }
                    }
                }
            }
        }
        item { Text("History", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp)) }
        if (runs.isEmpty()) item { Muted("No runs yet. Week 1 is mostly walking, so start easy.") }
        items(runs.sortedByDescending { it.date }, key = { it.clientId }) { r ->
            Panel {
                Row {
                    Text(Instant.parse(r.date).atZone(ZoneId.systemDefault()).format(dayFormat), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Muted(r.plan?.let { if (r.completed) it else "$it · stopped early" } ?: "Free run")
                }
                Muted("%.2f km · %s · %s".format(r.distanceM / 1000.0, formatClock(r.durationSec.toLong()), formatPace(r.durationSec.toLong(), r.distanceM.toDouble())))
            }
        }
    }
}
