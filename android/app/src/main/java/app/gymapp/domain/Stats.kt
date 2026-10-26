package app.gymapp.domain

import app.gymapp.data.Exercise
import app.gymapp.data.Workout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

fun Workout.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.parse(date).atZone(zone).toLocalDate()

fun Exercise.volume(): Double = sets.sumOf { it.reps * it.weight }
fun Workout.volume(): Double = exercises.sumOf { it.volume() }

/** Consecutive days ending today or yesterday (port of utils/stats.ts calculateStreak). */
fun streak(days: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
    var day = if (today in days) today else today.minusDays(1)
    var count = 0
    while (day in days) {
        count++
        day = day.minusDays(1)
    }
    return count
}

fun streak(workouts: List<Workout>, today: LocalDate = LocalDate.now()): Int = streak(workouts.map { it.localDate() }.toSet(), today)

fun sessionsInLast7Days(workouts: List<Workout>, today: LocalDate = LocalDate.now()): Int =
    workouts.count { ChronoUnit.DAYS.between(it.localDate(), today) in 0..6 }

data class Suggestion(val exercise: String, val weight: Double, val reps: Int, val reason: String)

/** Progressive-overload hint per exercise from its last session (port of utils/suggestions.ts). */
fun suggest(exercise: Exercise, unit: String): Suggestion? {
    if (exercise.sets.isEmpty()) return null
    val maxWeight = exercise.sets.maxOf { it.weight }
    val avgReps = exercise.sets.sumOf { it.reps }.toDouble() / exercise.sets.size
    val step = if (unit == "lbs") 5.0 else 2.5
    val stepText = "${formatWeight(step)} $unit"
    return when {
        avgReps >= 10 -> Suggestion(exercise.name, maxWeight + step, avgReps.roundToInt(), "High reps last time. Add $stepText.")
        avgReps < 5 && maxWeight > 0 -> Suggestion(exercise.name, maxOf(0.0, maxWeight - step), avgReps.roundToInt(), "Reps were low. Drop $stepText and own the form.")
        avgReps >= 8 -> Suggestion(exercise.name, maxWeight, minOf(12, (avgReps + 1).roundToInt()), "Good progress. Push one extra rep per set.")
        else -> Suggestion(exercise.name, maxWeight, avgReps.roundToInt(), "Hold this weight and build consistency.")
    }
}

/** Latest logged session of each exercise, newest workout first. */
fun lastByExercise(workouts: List<Workout>): Map<String, Exercise> {
    val result = linkedMapOf<String, Exercise>()
    for (w in workouts.sortedByDescending { it.date }) for (e in w.exercises) result.putIfAbsent(e.name.lowercase(), e)
    return result
}

fun formatWeight(w: Double): String = if (w % 1.0 == 0.0) w.toInt().toString() else "%.1f".format(w)

/** free-exercise-db muscle names grouped the way people think about their body. */
val MuscleRegions: List<Pair<String, List<String>>> = listOf(
    "Chest" to listOf("chest"),
    "Shoulders" to listOf("shoulders"),
    "Upper back" to listOf("middle back", "traps", "lats"),
    "Lower back" to listOf("lower back"),
    "Arms" to listOf("biceps", "triceps", "forearms"),
    "Core" to listOf("abdominals"),
    "Glutes & hips" to listOf("glutes", "abductors", "adductors"),
    "Quads" to listOf("quadriceps"),
    "Hamstrings" to listOf("hamstrings"),
    "Calves" to listOf("calves"),
)

/**
 * Hard sets per region over the last 7 days: a set counts fully for an exercise's primary
 * muscles and half for secondary ones. Exercises not in the library are skipped.
 */
fun muscleBalance(workouts: List<Workout>, library: List<LibExercise>, today: LocalDate = LocalDate.now()): List<Pair<String, Double>> {
    val byName = library.associateBy { it.name.lowercase() }
    val sets = mutableMapOf<String, Double>()
    workouts.filter { ChronoUnit.DAYS.between(it.localDate(), today) in 0..6 }.forEach { w ->
        w.exercises.forEach { e ->
            val lib = byName[e.name.lowercase()] ?: return@forEach
            lib.primaryMuscles.forEach { sets[it] = (sets[it] ?: 0.0) + e.sets.size }
            lib.secondaryMuscles.forEach { sets[it] = (sets[it] ?: 0.0) + e.sets.size * 0.5 }
        }
    }
    return MuscleRegions.map { (region, muscles) -> region to muscles.sumOf { sets[it] ?: 0.0 } }
}
