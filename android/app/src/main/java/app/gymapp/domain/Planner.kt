package app.gymapp.domain

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY

/** A movement slot in a day. [prefer] is a hand-picked order; [muscles] + [force] is the fallback search. */
data class Slot(val key: String, val label: String, val prefer: List<String>, val muscles: Set<String>, val force: String?, val main: Boolean)

private val squat = Slot("squat", "Squat", listOf("Barbell_Squat", "Leg_Press", "Goblet_Squat", "Dumbbell_Squat", "Hack_Squat"), setOf("quadriceps"), "push", true)
private val hinge = Slot("hinge", "Hinge", listOf("Romanian_Deadlift", "Stiff-Legged_Dumbbell_Deadlift", "Kettlebell_One-Legged_Deadlift", "Barbell_Hip_Thrust", "Single_Leg_Glute_Bridge", "Butt_Lift_Bridge"), setOf("hamstrings", "glutes"), "pull", true)
private val singleLeg = Slot("single_leg", "Single leg", listOf("Split_Squat_with_Dumbbells", "Dumbbell_Lunges", "Barbell_Lunge", "Bodyweight_Walking_Lunge"), setOf("quadriceps", "glutes"), "push", false)
private val legCurl = Slot("leg_curl", "Hamstrings", listOf("Lying_Leg_Curls", "Seated_Leg_Curl", "Ball_Leg_Curl", "Band_Good_Morning", "Good_Morning", "Stiff-Legged_Dumbbell_Deadlift"), setOf("hamstrings"), "pull", false)
private val calves = Slot("calves", "Calves", listOf("Standing_Calf_Raises", "Seated_Calf_Raise", "Standing_Dumbbell_Calf_Raise", "Calf_Raise_On_A_Dumbbell"), setOf("calves"), null, false)
private val hPush = Slot("h_push", "Chest press", listOf("Barbell_Bench_Press_-_Medium_Grip", "Dumbbell_Bench_Press", "Machine_Bench_Press", "Dumbbell_Floor_Press", "Pushups", "Push-Up_Wide"), setOf("chest"), "push", true)
private val inclinePush = Slot("incline_push", "Upper chest", listOf("Incline_Dumbbell_Press", "Barbell_Incline_Bench_Press_-_Medium_Grip", "Hammer_Grip_Incline_DB_Bench_Press", "Push-Ups_With_Feet_Elevated"), setOf("chest"), "push", false)
private val vPush = Slot("v_push", "Shoulder press", listOf("Dumbbell_Shoulder_Press", "Machine_Shoulder_Military_Press", "Seated_Barbell_Military_Press", "Leverage_Shoulder_Press", "Shoulder_Press_-_With_Bands"), setOf("shoulders"), "push", true)
private val hPull = Slot("h_pull", "Row", listOf("Seated_Cable_Rows", "One-Arm_Dumbbell_Row", "Bent_Over_Barbell_Row", "Bent_Over_Two-Dumbbell_Row"), setOf("middle back", "lats"), "pull", true)
private val hPull2 = Slot("h_pull_2", "Second row", listOf("Bent_Over_Two-Dumbbell_Row_With_Palms_In", "One-Arm_Dumbbell_Row", "Seated_Cable_Rows", "Bent_Over_Barbell_Row"), setOf("middle back", "lats"), "pull", false)
private val vPull = Slot("v_pull", "Pulldown / pull-up", listOf("Wide-Grip_Lat_Pulldown", "Pullups", "Chin-Up", "Close-Grip_Front_Lat_Pulldown", "Straight-Arm_Pulldown"), setOf("lats"), "pull", true)
private val rearDelt = Slot("rear_delt", "Rear delts (posture)", listOf("Face_Pull", "Band_Pull_Apart", "Reverse_Flyes", "Cable_Rear_Delt_Fly", "Seated_Bent-Over_Rear_Delt_Raise", "Bent_Over_Dumbbell_Rear_Delt_Raise_With_Head_On_Bench"), setOf("shoulders"), "pull", false)
private val lateral = Slot("lateral", "Side delts", listOf("Side_Lateral_Raise", "Cable_Seated_Lateral_Raise", "Lateral_Raise_-_With_Bands"), setOf("shoulders"), "push", false)
private val biceps = Slot("biceps", "Biceps", listOf("Dumbbell_Bicep_Curl", "Alternate_Hammer_Curl", "Barbell_Curl", "Cable_Hammer_Curls_-_Rope_Attachment"), setOf("biceps"), "pull", false)
private val triceps = Slot("triceps", "Triceps", listOf("Triceps_Pushdown_-_Rope_Attachment", "Triceps_Pushdown", "Cable_Rope_Overhead_Triceps_Extension", "EZ-Bar_Skullcrusher", "Dumbbell_One-Arm_Triceps_Extension", "Bench_Dips"), setOf("triceps"), "push", false)
private val core = Slot("core", "Core", listOf("Dead_Bug", "Plank", "Ab_Roller", "Cable_Crunch", "Crunches"), setOf("abdominals"), null, false)

/**
 * Day templates. Every upper or push day carries a rear-delt slot and pulling volume beats
 * pushing, because rounded shoulders come from a strong front and a weak back.
 */
private val fullBody = "Full body" to listOf(squat, hPush, hPull, hinge, vPull, rearDelt, core)
private val upper = "Upper" to listOf(hPush, hPull, vPush, vPull, rearDelt, biceps, triceps)
private val lower = "Lower" to listOf(squat, hinge, singleLeg, legCurl, calves, core)
private val push = "Push" to listOf(hPush, vPush, inclinePush, lateral, rearDelt, triceps)
private val pull = "Pull" to listOf(vPull, hPull, hPull2, rearDelt, biceps)
private val legs = "Legs" to listOf(squat, hinge, singleLeg, legCurl, calves, core)

private val schedules: Map<Int, List<Pair<DayOfWeek, Pair<String, List<Slot>>>>> = mapOf(
    2 to listOf(MONDAY to fullBody, THURSDAY to fullBody),
    3 to listOf(MONDAY to fullBody, WEDNESDAY to fullBody, FRIDAY to fullBody),
    4 to listOf(MONDAY to upper, TUESDAY to lower, THURSDAY to upper, FRIDAY to lower),
    5 to listOf(MONDAY to push, TUESDAY to pull, WEDNESDAY to legs, FRIDAY to upper, SATURDAY to lower),
    6 to listOf(MONDAY to push, TUESDAY to pull, WEDNESDAY to legs, THURSDAY to push, FRIDAY to pull, SATURDAY to legs),
)

enum class Goal(val label: String, val main: Pair<Int, String>, val accessory: Pair<Int, String>) {
    Strength("Strength", 4 to "5", 3 to "8"),
    Muscle("Build muscle", 3 to "8-10", 3 to "10-12"),
    FatLoss("Lose fat", 3 to "10-12", 2 to "12-15"),
    General("Stay fit", 3 to "8-12", 2 to "12"),
}

data class PlannedExercise(val slot: Slot, val exercise: LibExercise, val sets: Int, val reps: String, val options: Int)
data class PlanDay(val day: DayOfWeek, val title: String, val exercises: List<PlannedExercise>)
data class WeekPlan(val days: List<PlanDay>, val deload: Boolean) {
    fun on(day: DayOfWeek): PlanDay? = days.find { it.day == day }
}

private val explosive = listOf("clean", "snatch", "jerk", "swing", "throw", "jump", "toss", "slam")

/** Week number since the plan started (0-based), from the Monday it began. */
fun planWeek(planStart: String?, today: java.time.LocalDate): Int =
    planStart?.let { maxOf(0, java.time.temporal.ChronoUnit.WEEKS.between(java.time.LocalDate.parse(it), today).toInt()) } ?: 0

fun goalOf(name: String): Goal = Goal.entries.find { it.name == name } ?: Goal.Muscle

/** Every 5th week drops a set from everything so joints and nerves recover. */
fun isDeloadWeek(week: Int) = week % 5 == 4

fun candidates(slot: Slot, library: List<LibExercise>, kit: Set<String>): List<LibExercise> {
    val byId = library.associateBy { it.id }
    val preferred = slot.prefer.mapNotNull { byId[it] }.filter { it.doableWith(kit) }
    val fallback = library.asSequence()
        .filter { it.category == "strength" && it.level != "expert" && it.id !in slot.prefer }
        .filter { (slot.force == null || it.force == slot.force) && it.primaryMuscles.any { m -> m in slot.muscles } }
        // Main slots get compound lifts, the rest isolation; never an explosive lift as filler.
        .filter { it.mechanic == (if (slot.main) "compound" else "isolation") && explosive.none { w -> w in it.name.lowercase() } }
        .filter { it.doableWith(kit) }
        .sortedBy { it.name }
        .take(8)
        .toList()
    return preferred + fallback
}

/**
 * Rules-first weekly plan: equipment + days per week + goal. [swaps] holds, per slot key, how
 * many times the user tapped "swap" so they can cycle through the alternatives.
 */
fun buildPlan(library: List<LibExercise>, equipment: List<String>, daysPerWeek: Int, goal: Goal, week: Int, swaps: Map<String, Int> = emptyMap()): WeekPlan {
    val kit = equipment.toSet()
    val deload = isDeloadWeek(week)
    val schedule = schedules[daysPerWeek.coerceIn(2, 6)]!!
    val days = schedule.map { (day, template) ->
        val (title, slots) = template
        val used = mutableSetOf<String>()
        val planned = slots.mapNotNull { slot ->
            val options = candidates(slot, library, kit).filter { it.id !in used }
            if (options.isEmpty()) return@mapNotNull null
            val pick = options[(swaps[slot.key] ?: 0).mod(options.size)]
            used += pick.id
            val (sets, reps) = if (slot.main) goal.main else goal.accessory
            PlannedExercise(slot, pick, if (deload) maxOf(1, sets - 1) else sets, reps, options.size)
        }
        PlanDay(day, title, planned)
    }
    return WeekPlan(days, deload)
}
