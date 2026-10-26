package app.gymapp.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class PostureTest {
    @Test fun routineIsAboutTenMinutesAndUsesEquipment() {
        val bare = postureRoutine(emptyList())
        val total = bare.sumOf { it.seconds } + bare.size * 5 // 5 s get-ready per move
        assertTrue("was ${total}s", total in 540..720)
        assertTrue(bare.any { it.name == "Prone T raises" } && bare.any { it.name == "Chair T-spine extension" })
        val kitted = postureRoutine(listOf("Resistance bands", "Foam roller"))
        assertTrue(kitted.any { it.name == "Band pull-aparts" } && kitted.any { it.name == "Foam roller T-spine extension" })
    }
}
