package app.gymapp.run

import app.gymapp.data.Run
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaceTest {
    private fun run(meters: Int, seconds: Int) = Run(clientId = "$meters-$seconds", date = "2026-10-27T06:00:00Z", durationSec = seconds, distanceM = meters, completed = true)

    @Test fun zonesNeedFiveKAndUseBestEffort() {
        assertNull(paceZones(listOf(run(3000, 1200))))
        // 30:00 for 5 km = 6:00/km -> easy 7:30-8:06, tempo 6:28, intervals 5:52.
        val z = paceZones(listOf(run(5000, 1800), run(5000, 2100)))!!
        assertEquals("7:30-8:06", z.easy)
        assertEquals("6:28", z.tempo)
        assertEquals("5:52", z.interval)
        assertEquals("1:00 /km", formatPace(300, 5000.0))
    }
}
