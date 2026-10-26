package app.gymapp.domain

import java.time.DayOfWeek

enum class Phase(val label: String) { WarmUp("Warm-up walk"), Run("Run"), Walk("Walk"), CoolDown("Cool-down walk") }

data class Interval(val phase: Phase, val seconds: Int)

/** One Couch-to-5K session: 5-minute walks either side of the run/walk block. */
data class RunSession(val week: Int, val day: Int, val intervals: List<Interval>) {
    val code: String get() = "W${week}D$day"
    val totalSeconds: Int get() = intervals.sumOf { it.seconds }
    val runSeconds: Int get() = intervals.filter { it.phase == Phase.Run }.sumOf { it.seconds }
    val summary: String get() {
        val block = intervals.filter { it.phase == Phase.Run || it.phase == Phase.Walk }
        return if (block.size == 1) "Run ${block[0].seconds / 60} min" else block.joinToString(", ") { "${it.phase.label.lowercase()} ${fmt(it.seconds)}" }
            .let { it.replaceFirstChar(Char::uppercase) }
    }
}

private fun fmt(s: Int) = if (s % 60 == 0) "${s / 60}m" else if (s < 60) "${s}s" else "${s / 60}m${s % 60}s"

private fun r(s: Int) = Interval(Phase.Run, s)
private fun w(s: Int) = Interval(Phase.Walk, s)
private fun repeat(n: Int, vararg block: Interval) = List(n) { block.toList() }.flatten()

/** The run/walk core of each session, by week and day. Standard 9-week Couch-to-5K progression. */
private val cores: List<List<List<Interval>>> = listOf(
    List(3) { repeat(8, r(60), w(90)) },
    List(3) { repeat(6, r(90), w(120)) },
    List(3) { repeat(2, r(90), w(90), r(180), w(180)) },
    List(3) { listOf(r(180), w(90), r(300), w(150), r(180), w(90), r(300)) },
    listOf(repeat(3, r(300), w(180)), listOf(r(480), w(300), r(480)), listOf(r(1200))),
    listOf(listOf(r(300), w(180), r(480), w(180), r(300)), listOf(r(600), w(180), r(600)), listOf(r(1320))),
    List(3) { listOf(r(1500)) },
    List(3) { listOf(r(1680)) },
    List(3) { listOf(r(1800)) },
)

val C25k: List<RunSession> = cores.flatMapIndexed { wi, days ->
    days.mapIndexed { di, core ->
        // Trailing walk dropped so the block always ends on a run before the cool-down.
        val trimmed = if (core.last().phase == Phase.Walk) core.dropLast(1) else core
        RunSession(wi + 1, di + 1, listOf(Interval(Phase.WarmUp, 300)) + trimmed + Interval(Phase.CoolDown, 300))
    }
}

/** First session not yet completed; after week 9 the last one repeats as a maintenance run. */
fun nextRun(completedCodes: Set<String>): RunSession = C25k.firstOrNull { it.code !in completedCodes } ?: C25k.last()

/**
 * Three run days that avoid lifting days where possible, spread through the week.
 * Easy run-walks are fine on a lifting day too, so busy weeks still get three.
 */
fun runDays(liftDays: Set<DayOfWeek>): List<DayOfWeek> {
    val preference = listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.MONDAY)
    return (preference.filter { it !in liftDays } + preference.filter { it in liftDays }).take(3).sorted()
}
