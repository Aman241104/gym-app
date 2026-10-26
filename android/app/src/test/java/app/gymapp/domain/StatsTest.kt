package app.gymapp.domain

import app.gymapp.data.Exercise
import app.gymapp.data.SetEntry
import app.gymapp.data.Workout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatsTest {
    private val today = LocalDate.of(2026, 10, 27)
    private fun on(day: LocalDate) = Workout(date = day.atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toString(), exercises = emptyList())

    @Test fun streakCountsBackFromTodayOrYesterday() {
        assertEquals(3, streak(listOf(on(today), on(today.minusDays(1)), on(today.minusDays(2)), on(today.minusDays(4))), today))
        assertEquals(2, streak(listOf(on(today.minusDays(1)), on(today.minusDays(2))), today))
        assertEquals(0, streak(listOf(on(today.minusDays(2))), today))
        assertEquals(1, streak(listOf(on(today), on(today)), today))
    }

    @Test fun overloadRules() {
        fun ex(vararg sets: Pair<Int, Double>) = Exercise("Row", sets.map { SetEntry(it.first, it.second) })
        assertEquals(42.5, suggest(ex(10 to 40.0, 10 to 40.0), "kg")!!.weight, 0.0)
        assertEquals(45.0, suggest(ex(12 to 40.0), "lbs")!!.weight, 0.0)
        assertEquals(37.5, suggest(ex(4 to 40.0), "kg")!!.weight, 0.0)
        assertEquals(10, suggest(ex(9 to 40.0), "kg")!!.reps)
        assertEquals(40.0, suggest(ex(6 to 40.0), "kg")!!.weight, 0.0)
    }

    @Test fun personalRecords() {
        val old = Workout(date = "2026-10-20T10:00:00Z", exercises = listOf(Exercise("Bench Press", listOf(SetEntry(5, 60.0), SetEntry(5, 57.5)))))
        val prs = findPRs(listOf(old), listOf(
            Exercise("bench press", listOf(SetEntry(3, 62.5))), // heavier: PR (case-insensitive)
            Exercise("Squat", listOf(SetEntry(5, 100.0))),      // never done: not a PR
        ))
        assertEquals(listOf(PersonalRecord("bench press", 62.5, 60.0)), prs)
        assertEquals(emptyList<PersonalRecord>(), findPRs(listOf(old), listOf(Exercise("Bench Press", listOf(SetEntry(8, 60.0))))))
    }
}
