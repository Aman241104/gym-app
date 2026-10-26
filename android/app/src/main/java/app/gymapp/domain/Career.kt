package app.gymapp.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate

/** One day's career habits, stored as item "day-yyyy-mm-dd". */
@Serializable
data class CareerDay(val applied: Int = 0, val followUps: Int = 0, val learnMinutes: Int = 0)

/** A skill being learned, stored as item "skill-…". [log] is minutes per local date. */
@Serializable
data class Skill(val name: String, val log: Map<String, Int> = emptyMap()) {
    val minutes: Int get() = log.values.sum()
}

data class CareerWeek(val applied: Int, val followUps: Int, val learnMinutes: Int)

fun careerWeek(days: Map<LocalDate, CareerDay>, today: LocalDate): CareerWeek {
    val week = (0..6).mapNotNull { days[today.minusDays(it.toLong())] }
    return CareerWeek(week.sumOf { it.applied }, week.sumOf { it.followUps }, week.sumOf { it.learnMinutes })
}
