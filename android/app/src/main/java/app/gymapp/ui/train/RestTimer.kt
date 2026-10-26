package app.gymapp.ui.train

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import kotlinx.coroutines.delay

/** Tap a preset after a set; the phone buzzes when rest is over. */
// ponytail: only buzzes while the session screen is open; move to a foreground service if that matters.
@Composable
internal fun RestTimer() {
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
    Panel {
        if (endsAt > 0) {
            val left = ((endsAt - now).coerceAtLeast(0) + 999) / 1000
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rest  %d:%02d".format(left / 60, left % 60), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                TextButton(onClick = { Draft.restEndsAt += 30_000 }) { Text("+30s") }
                TextButton(onClick = { Draft.restEndsAt = 0L }) { Text("Skip") }
            }
        } else {
            Muted("Rest timer")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120, 180).forEach { secs ->
                    AssistChip(
                        onClick = { Draft.restEndsAt = System.currentTimeMillis() + secs * 1000L },
                        label = { Text(if (secs < 60) "${secs}s" else "%d:%02d".format(secs / 60, secs % 60)) },
                    )
                }
            }
        }
    }
}
