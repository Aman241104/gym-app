package app.gymapp.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.gymapp.domain.LibExercise
import app.gymapp.ui.Muted
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

/** Start/end photos flip once a second so the movement reads like a tiny animation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSheet(exercise: LibExercise, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(exercise.name, style = MaterialTheme.typography.headlineSmall)
            val urls = exercise.imageUrls
            if (urls.isNotEmpty()) {
                var frame by remember(exercise.id) { mutableIntStateOf(0) }
                LaunchedEffect(exercise.id) {
                    while (urls.size > 1) {
                        delay(1000)
                        frame = (frame + 1) % urls.size
                    }
                }
                Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    // Both frames stay composed so the flip never waits on the network.
                    urls.forEachIndexed { i, url ->
                        AsyncImage(
                            model = url, contentDescription = if (i == frame) "${exercise.name}, ${if (i == 0) "start" else "end"} position" else null,
                            contentScale = ContentScale.Crop, alpha = if (i == frame) 1f else 0f, modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                        )
                    }
                }
            }
            Muted(listOfNotNull(exercise.equipment?.replaceFirstChar(Char::uppercase), exercise.level.replaceFirstChar(Char::uppercase), exercise.mechanic).joinToString(" · "))
            Text("Works: " + (exercise.primaryMuscles + exercise.secondaryMuscles).distinct().joinToString(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            exercise.instructions.forEachIndexed { i, step ->
                Row {
                    Text("${i + 1}.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(26.dp))
                    Text(step, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Muted("From free-exercise-db (public domain).", Modifier.padding(top = 6.dp))
        }
    }
}
