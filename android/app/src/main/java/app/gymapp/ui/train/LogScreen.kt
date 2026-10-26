package app.gymapp.ui.train

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.remember
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.gymapp.data.Exercise
import app.gymapp.data.Repo
import app.gymapp.data.SetEntry
import app.gymapp.data.Workout
import app.gymapp.domain.LibExercise
import app.gymapp.domain.PlanDay
import app.gymapp.domain.doableWith
import app.gymapp.ui.library.ExerciseSheet
import app.gymapp.domain.formatWeight
import app.gymapp.domain.lastByExercise
import app.gymapp.domain.suggest
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel

internal class SetDraft(reps: String, weight: String) {
    var reps by mutableStateOf(reps)
    var weight by mutableStateOf(weight)
}

/** [target] is the plan's "3 × 8-10" when the exercise came from today's plan. */
internal class ExerciseDraft(val name: String, sets: List<SetDraft>, val target: String? = null) {
    val sets = mutableStateListOf(*sets.toTypedArray())
}

/** The session in progress. Lives outside composition so leaving the screen (or rotating) keeps it. */
// ponytail: lost if Android kills the process mid-session; persist to DataStore if that ever bites.
internal object Draft {
    val exercises = mutableStateListOf<ExerciseDraft>()
    /** Set when editing a past session instead of logging a new one. */
    var editing by mutableStateOf<Workout?>(null)
    /** Wall-clock end of the running rest timer, 0 when none. */
    var restEndsAt by mutableLongStateOf(0L)

    fun load(workout: Workout) {
        clear()
        editing = workout
        workout.exercises.forEach { e -> exercises.add(ExerciseDraft(e.name, e.sets.map { SetDraft(it.reps.toString(), formatWeight(it.weight)) })) }
    }

    /** Starts a session from a planned day, prefilled from last time's overload hint where there is one. */
    fun loadPlan(day: PlanDay, history: Map<String, Exercise>, unit: String) {
        clear()
        day.exercises.forEach { p ->
            val hint = history[p.exercise.name.lowercase()]?.let { suggest(it, unit) }
            val reps = hint?.reps?.toString() ?: p.reps.substringBefore('-')
            val weight = hint?.let { formatWeight(it.weight) }.orEmpty()
            exercises.add(ExerciseDraft(p.exercise.name, List(p.sets) { SetDraft(reps, weight) }, "${p.sets} × ${p.reps}"))
        }
    }

    fun clear() {
        exercises.clear()
        editing = null
        restEndsAt = 0L
    }

    fun toExercises(): List<Exercise> = exercises.mapNotNull { e ->
        val sets = e.sets.mapNotNull { s ->
            val reps = s.reps.toIntOrNull() ?: return@mapNotNull null
            if (reps <= 0) null else SetEntry(reps, s.weight.replace(',', '.').toDoubleOrNull() ?: 0.0)
        }
        if (sets.isEmpty()) null else Exercise(e.name, sets)
    }
}

@Composable
fun LogScreen(onDone: () -> Unit) {
    val workouts by Repo.workouts.collectAsState()
    val settings by Repo.settings.collectAsState()
    val history = lastByExercise(workouts)
    val library by Repo.library.collectAsState()
    val kit = settings.equipment.toSet()
    val byName = remember(library) { library.associateBy { it.name.lowercase() } }
    var detail by remember { mutableStateOf<LibExercise?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val ready = Draft.toExercises().isNotEmpty()

    fun add(name: String) {
        val clean = name.trim()
        if (clean.isEmpty() || Draft.exercises.any { it.name.equals(clean, ignoreCase = true) }) return
        // Prefill from last time's progressive-overload hint, so most sets are just a tap to confirm.
        val last = history[clean.lowercase()]
        val hint = last?.let { suggest(it, settings.unit) }
        val sets = if (hint != null) List(last.sets.size) { SetDraft(hint.reps.toString(), formatWeight(hint.weight)) } else listOf(SetDraft("", ""))
        Draft.exercises.add(ExerciseDraft(last?.name ?: clean, sets))
        query = ""
    }

    LazyColumn(
        Modifier.imePadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                Text(if (Draft.editing != null) "Edit session" else "Session", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                if (Draft.exercises.isNotEmpty()) TextButton(onClick = { Draft.clear(); onDone() }) { Text("Discard") }
            }
        }
        if (Draft.editing == null) item { RestTimer() }
        itemsIndexed(Draft.exercises, key = { _, e -> e.name }) { _, exercise ->
            ExerciseCard(
                exercise, settings.unit,
                onRemove = { Draft.exercises.remove(exercise) },
                onInfo = byName[exercise.name.lowercase()]?.let { lib -> { detail = lib } },
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Add exercise") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add(query) }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            // Recent exercises first, then everything in the library your equipment allows.
            val q = query.trim()
            val doable = remember(library, kit) { library.filter { it.category != "stretching" && it.doableWith(kit) }.map { it.name } }
            val names = (history.values.map { it.name } + doable).distinctBy { it.lowercase() }
                .filter { q.isBlank() || it.contains(q, ignoreCase = true) }
                .filterNot { n -> Draft.exercises.any { it.name.equals(n, ignoreCase = true) } }
                .take(30)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(names) { name -> SuggestionChip(onClick = { add(name) }, label = { Text(name) }) }
            }
        }
        item {
            Button(
                onClick = {
                    Repo.saveWorkout(Draft.toExercises(), Draft.editing)
                    Draft.clear()
                    onDone()
                },
                enabled = ready,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp),
            ) { Text(if (Draft.editing != null) "Save changes" else "Finish & save", style = MaterialTheme.typography.titleMedium) }
        }
    }
    detail?.let { ExerciseSheet(it, onDismiss = { detail = null }) }
}

@Composable
private fun ExerciseCard(exercise: ExerciseDraft, unit: String, onRemove: () -> Unit, onInfo: (() -> Unit)?) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                exercise.target?.let { Muted("Target $it") }
            }
            if (onInfo != null) IconButton(onClick = onInfo) { Icon(Icons.Rounded.Info, contentDescription = "How to do ${exercise.name}") }
            IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, contentDescription = "Remove ${exercise.name}") }
        }
        exercise.sets.forEachIndexed { i, set ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Muted("${i + 1}", Modifier.width(20.dp))
                OutlinedTextField(
                    value = set.weight, onValueChange = { set.weight = it }, label = { Text(unit) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = set.reps, onValueChange = { set.reps = it.filter(Char::isDigit) }, label = { Text("reps") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { exercise.sets.removeAt(i) }, enabled = exercise.sets.size > 1) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove set ${i + 1}")
                }
            }
        }
        TextButton(onClick = {
            val last = exercise.sets.lastOrNull()
            exercise.sets.add(SetDraft(last?.reps.orEmpty(), last?.weight.orEmpty()))
        }) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text(" Add set")
        }
    }
}
