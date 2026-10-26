package app.gymapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import java.net.HttpURLConnection
import java.net.URL

/** Tiny client for the Next.js API routes. Plain HttpURLConnection: four calls don't need a library. */
object Api {
    suspend fun <T> call(server: Server, method: String, path: String, out: KSerializer<T>, body: String? = null): T =
        withContext(Dispatchers.IO) {
            val conn = URL(server.url.trimEnd('/') + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Authorization", "Bearer ${server.token}")
                conn.setRequestProperty("Content-Type", "application/json")
                if (body != null) {
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray()) }
                }
                val ok = conn.responseCode in 200..299
                val text = (if (ok) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val parsed = runCatching { json.decodeFromString(ApiResponse.serializer(out), text) }.getOrNull()
                if (!ok || parsed?.success != true || parsed.data == null) {
                    error(parsed?.error ?: "HTTP ${conn.responseCode}")
                }
                parsed.data
            } finally {
                conn.disconnect()
            }
        }
}
