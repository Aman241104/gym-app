package app.gymapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MergeTest {
    private fun w(server: String?, client: String?, note: String = "") =
        Workout(serverId = server, clientId = client, date = "2026-10-27T10:00:00Z", exercises = listOf(Exercise(note, emptyList())))

    @Test fun queuedChangesSurviveAPull() {
        val local = listOf(w(null, "new"), w("s1", null, "edited"), w("s2", null))
        val remote = listOf(w("s1", null, "old"), w("s2", null), w("s3", null))
        val pending = listOf(OutboxEntry("workout", "new"), OutboxEntry("update", serverId = "s1"), OutboxEntry("delete", serverId = "s3"))
        val merged = Repo.merge(local, remote, pending)
        assertEquals(listOf("new", "s1", "s2"), merged.map { it.clientId ?: it.serverId })
        assertEquals("edited", merged[1].exercises.single().name)
    }

    @Test fun uploadedCreateIsNotDuplicated() {
        val merged = Repo.merge(listOf(w(null, "c")), listOf(w("s9", "c")), listOf(OutboxEntry("workout", "c")))
        assertEquals(1, merged.size)
    }
}
