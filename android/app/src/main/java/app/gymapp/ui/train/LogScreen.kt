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
import app.gymapp.domain.PersonalRecord
import app.gymapp.domain.findPRs
import app.gymapp.ui.design.Accent
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import app.gymapp.domain.PlanDay
import app.gymapp.domain.PreferredExerciseIds
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
    /** Ticked off in the gym; ticking starts the rest timer. Purely a logging aid. */
    var done by mutableStateOf(false)
}

/** Personal records from the last save, shown as a celebration over whatever screen is next. */
internal object Celebrate {
    var records by mutableStateOf<List<PersonalRecord>>(emptyList())
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
    /** Rest length the next ticked set starts; set by the preset chips. */
    var restSeconds by mutableIntStateOf(90)

    fun startRest() {
        restEndsAt = System.currentTimeMillis() + restSeconds * 1000L
    }

    /** Saves the session (new or edited) and queues PR confetti for new sessions. */
    fun save(history: List<Workout>) {
        val done = toExercises()
        if (done.isEmpty()) return
        if (editing == null) Celebrate.records = findPRs(history, done)
        Repo.saveWorkout(done, editing)
        clear()
    }

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
        // Bottom room for the session dock that replaces the tab bar while logging.
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                Text(if (Draft.editing != null) "Edit session" else "Session", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                if (Draft.exercises.isNotEmpty()) TextButton(onClick = { Draft.clear(); onDone() }) { Text("Discard") }
            }
        }
        if (Draft.editing == null) item { RestPresets() }
        itemsIndexed(Draft.exercises, key = { _, e -> e.name }) { _, exercise ->
            ExerciseCard(
                exercise, settings.unit, history[exercise.name.lowercase()],
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
            val doable = remember(library, kit) {
                // Planner favourites first, then the rest of what your equipment allows, A-Z.
                val byId = library.associateBy { it.id }
                val favourites = PreferredExerciseIds.mapNotNull { byId[it] }.filter { it.doableWith(kit) }
                (favourites + library.filter { it.category != "stretching" && it.doableWith(kit) }).map { it.name }.distinct()
            }
            val names = (history.values.map { it.name } + doable).distinctBy { it.lowercase() }
                .filter { q.isBlank() || it.contains(q, ignoreCase = true) }
                .filterNot { n -> Draft.exercises.any { it.name.equals(n, ignoreCase = true) } }
                .take(30)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(names) { name -> SuggestionChip(onClick = { add(name) }, label = { Text(name) }) }
            }
        }
    }
    detail?.let { ExerciseSheet(it, onDismiss = { detail = null }) }
}

@Composable
private fun ExerciseCard(exercise: ExerciseDraft, unit: String, last: Exercise?, onRemove: () -> Unit, onInfo: (() -> Unit)?) {
    val accent = Accent.Train.color
    val haptics = LocalHapticFeedback.current
    Panel(accent = if (exercise.sets.isNotEmpty() && exercise.sets.all { it.done }) accent else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                exercise.target?.let { Muted("Target $it") }
                last?.let { l ->
                    val weights = l.sets.map { formatWeight(it.weight) }.distinct()
                    Muted("Last: " + (if (weights.size == 1) "${weights[0]} $unit × " + l.sets.joinToString(", ") { "${it.reps}" } else l.sets.joinToString(", ") { "${formatWeight(it.weight)}×${it.reps}" }))
                }
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
                // One tap = set done + rest timer started, like Hevy/Strong.
                IconButton(onClick = {
                    set.done = !set.done
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (set.done) Draft.startRest()
                }) {
                    Icon(
                        if (set.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (set.done) "Set ${i + 1} done" else "Mark set ${i + 1} done",
                        tint = if (set.done) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
        Row {
            TextButton(onClick = {
                val prev = exercise.sets.lastOrNull()
                exercise.sets.add(SetDraft(prev?.reps.orEmpty(), prev?.weight.orEmpty()))
            }) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(" Add set")
            }
            if (exercise.sets.size > 1) TextButton(onClick = { exercise.sets.removeAt(exercise.sets.lastIndex) }) { Text("Remove last") }
        }
    }
}
