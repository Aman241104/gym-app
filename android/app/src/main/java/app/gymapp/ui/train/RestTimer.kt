package app.gymapp.ui.train

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.gymapp.data.Workout
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.design.Accent
import app.gymapp.ui.glass.LiquidButton
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay

/** In-list panel: how long to rest after each ticked set. */
@Composable
internal fun RestPresets() {
    Panel {
        Muted("Rest after each ticked set")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(60, 90, 120, 180).forEach { secs ->
                FilterChip(
                    selected = Draft.restSeconds == secs,
                    onClick = { Draft.restSeconds = secs },
                    label = { Text("%d:%02d".format(secs / 60, secs % 60)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent.Train.color.copy(alpha = 0.25f)),
                )
            }
        }
    }
}

/**
 * The tab bar's stand-in while logging: a glass rest timer (tap to start, +30 s while running)
 * and Finish. It stays on screen, so the countdown and the end-of-rest buzz never get scrolled away.
 */
@Composable
internal fun SessionDock(backdrop: Backdrop, history: List<Workout>, onFinished: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val endsAt = Draft.restEndsAt
    LaunchedEffect(endsAt) {
        while (endsAt > 0 && System.currentTimeMillis() < endsAt) {
            now = System.currentTimeMillis()
            delay(250)
        }
        if (endsAt > 0) {
            Draft.restEndsAt = 0L
            context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
        }
    }
    val ready = Draft.toExercises().isNotEmpty()
    val train = Accent.Train.color
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiquidButton(
            onClick = { if (endsAt > 0) Draft.restEndsAt += 30_000 else Draft.startRest() },
            backdrop = backdrop,
            modifier = Modifier.weight(1f).height(64.dp),
        ) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = train)
            if (endsAt > 0) {
                val left = ((endsAt - now).coerceAtLeast(0) + 999) / 1000
                Text("%d:%02d".format(left / 60, left % 60), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Muted("+30s")
            } else {
                Text("Rest %d:%02d".format(Draft.restSeconds / 60, Draft.restSeconds % 60), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        LiquidButton(
            onClick = { if (ready) { Draft.save(history); onFinished() } },
            backdrop = backdrop,
            tint = train,
            modifier = Modifier.size(height = 64.dp, width = 140.dp).alpha(if (ready) 1f else 0.5f),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            Text(if (Draft.editing != null) "Save" else "Finish", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
