package app.gymapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosingTest {
    // A front double biceps drawn by hand (image y grows downward). Person's left is image right.
    private fun body(leftElbowY: Float = 200f): List<Pt> {
        val p = MutableList(33) { Pt(0f, 0f, 1f) }
        p[11] = Pt(600f, 200f, 1f); p[12] = Pt(400f, 200f, 1f) // shoulders
        p[13] = Pt(700f, leftElbowY, 1f); p[14] = Pt(300f, 200f, 1f) // elbows out level
        p[15] = Pt(666f, 106f, 1f); p[16] = Pt(334f, 106f, 1f) // fists curled in toward the head: ~70° at the elbow
        p[23] = Pt(580f, 500f, 1f); p[24] = Pt(420f, 500f, 1f) // hips
        return p
    }

    @Test fun anglesAndPerfectPose() {
        assertEquals(90f, angleAt(Pt(0f, 1f, 1f), Pt(0f, 0f, 1f), Pt(1f, 0f, 1f)), 0.01f)
        val check = checkPose(body(), Poses.first { it.name == "Front double biceps" })!!
        assertTrue("score ${check.score}", check.score >= 90)
        assertEquals("Hold it. Breathe and squeeze.", check.tip)
    }

    @Test fun droppedElbowGetsTip() {
        val check = checkPose(body(leftElbowY = 330f), Poses.first { it.name == "Front double biceps" })!!
        assertTrue(check.score < 90)
        assertEquals("Raise your left elbow", check.tip)
    }
}
