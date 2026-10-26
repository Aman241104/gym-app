package app.gymapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

// Same shape as the backend's lib/models/Workout.ts, so one class covers local storage and the API.
@Serializable
data class SetEntry(val reps: Int, val weight: Double)

@Serializable
data class Exercise(val name: String, val sets: List<SetEntry>)

@Serializable
data class Workout(
    @SerialName("_id") val serverId: String? = null,
    val clientId: String? = null,
    val date: String, // ISO-8601 instant, as Mongo returns it
    val exercises: List<Exercise>,
) {
    /** Stable key for lists: web-logged workouts only have a server id. */
    val key: String get() = clientId ?: serverId ?: date
}

@Serializable
data class Settings(
    val unit: String = "kg",
    val equipment: List<String> = emptyList(),
    val daysPerWeek: Int = 3,
    val goal: String = "Muscle",
    /** Monday the plan started (yyyy-mm-dd); week numbers and deloads count from here. */
    val planStart: String? = null,
)

/** A finished (or stopped early) run. [plan] is the Couch-to-5K code like "W1D2", null for a free run. */
@Serializable
data class Run(
    @SerialName("_id") val serverId: String? = null,
    val clientId: String,
    val date: String,
    val plan: String? = null,
    val durationSec: Int,
    val distanceM: Int,
    val completed: Boolean,
)

/** One posture photo check. Photos stay in app storage on the phone; only these numbers are kept. */
@Serializable
data class PostureScan(
    val date: String,
    val kind: String, // "side" or "front"
    val headForward: Float? = null,
    val upperBack: Float? = null,
    val shoulderTilt: Float? = null,
    val hipTilt: Float? = null,
    val photo: String,
)

/** Where the Next.js backend lives. Local only, never sent anywhere but that server. */
@Serializable
data class Server(val url: String = "", val token: String = "") {
    val configured: Boolean get() = url.isNotBlank() && token.isNotBlank()
}

/** kind: "workout" / "run" (POST, upserts by clientId), "update" (PUT serverId), "delete" (DELETE serverId), "settings". */
@Serializable
data class OutboxEntry(val kind: String, val clientId: String? = null, val serverId: String? = null)

@Serializable
data class ApiResponse<T>(val success: Boolean, val data: T? = null, val error: String? = null)
