package app.gymapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostureMathTest {
    private fun body(vararg set: Pair<Int, Pt>): List<Pt> {
        val pts = MutableList(33) { Pt(0f, 0f, 0f) }
        set.forEach { (i, p) -> pts[i] = p }
        return pts
    }

    @Test fun sideFacingRightAndLeftGiveSameForwardAngle() {
        // Ear 30 px ahead of the shoulder over 100 px of height: atan(0.3) ≈ 16.7°.
        val right = body(0 to Pt(560f, 100f, 1f), 7 to Pt(530f, 120f, 1f), 11 to Pt(500f, 220f, 1f), 23 to Pt(500f, 520f, 1f))
        val left = body(0 to Pt(440f, 100f, 1f), 8 to Pt(470f, 120f, 1f), 12 to Pt(500f, 220f, 1f), 24 to Pt(500f, 520f, 1f))
        val a = analyzeSide(right) as ScanResult.Side
        val b = analyzeSide(left) as ScanResult.Side
        assertEquals(16.7f, a.headForward, 0.1f)
        assertEquals(a.headForward, b.headForward, 0.01f)
        assertEquals(0f, a.upperBack, 0.01f)
    }

    @Test fun frontTiltAndUnclear() {
        // Person's left shoulder (index 11, image right) 10 px lower over 200 px: ≈ 2.9°.
        val front = body(11 to Pt(600f, 310f, 1f), 12 to Pt(400f, 300f, 1f), 23 to Pt(580f, 600f, 1f), 24 to Pt(420f, 600f, 1f))
        val r = analyzeFront(front) as ScanResult.Front
        assertEquals(2.86f, r.shoulderTilt, 0.05f)
        assertEquals("Left shoulder lower by 2.9°.", tiltLabel("shoulder", r.shoulderTilt))
        assertEquals("Hips level.", tiltLabel("hip", r.hipTilt))
        assertTrue(analyzeFront(body()) is ScanResult.Unclear)
    }
}
