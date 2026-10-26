package app.gymapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY

class C25kTest {
    @Test fun planShape() {
        assertEquals(27, C25k.size)
        assertEquals("W1D1", C25k.first().code)
        assertEquals(8 * 60, C25k.first().runSeconds)
        assertEquals(30 * 60, C25k.last().runSeconds)
        C25k.forEach { s ->
            assertEquals(Phase.WarmUp, s.intervals.first().phase)
            assertEquals(Phase.CoolDown, s.intervals.last().phase)
            assertEquals(Phase.Run, s.intervals[s.intervals.size - 2].phase) // never a walk right before cool-down
        }
    }

    @Test fun progressionAndScheduling() {
        assertEquals("W1D1", nextRun(emptySet()).code)
        assertEquals("W2D1", nextRun(setOf("W1D1", "W1D2", "W1D3")).code)
        assertEquals("W9D3", nextRun(C25k.map { it.code }.toSet()).code)
        val days = runDays(setOf(MONDAY, WEDNESDAY, FRIDAY))
        assertEquals(3, days.size)
        assertTrue(days.none { it in setOf(MONDAY, WEDNESDAY, FRIDAY) })
    }

    @Test fun summariesCompressRepeats() {
        assertEquals("8 × run 1m · walk 1m30s", C25k[0].summary)
        assertEquals("2 × run 1m30s · walk 1m30s · run 3m · walk 3m", C25k.first { it.code == "W3D1" }.summary)
        assertEquals("Run 20 min", C25k.first { it.code == "W5D3" }.summary)
        assertEquals("2 × run 3m · walk 1m30s · run 5m · walk 2m30s", C25k.first { it.code == "W4D1" }.summary)
        assertEquals("2 × run 8m · walk 5m", C25k.first { it.code == "W5D2" }.summary)
    }
}
