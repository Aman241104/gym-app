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

/** Where the Next.js backend lives. Local only, never sent anywhere but that server. */
@Serializable
data class Server(val url: String = "", val token: String = "") {
    val configured: Boolean get() = url.isNotBlank() && token.isNotBlank()
}

/** kind: "workout" (POST, upserts by clientId), "update" (PUT serverId), "delete" (DELETE serverId), "settings". */
@Serializable
data class OutboxEntry(val kind: String, val clientId: String? = null, val serverId: String? = null)

@Serializable
data class ApiResponse<T>(val success: Boolean, val data: T? = null, val error: String? = null)
