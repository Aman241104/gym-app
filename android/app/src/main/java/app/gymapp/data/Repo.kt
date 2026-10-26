package app.gymapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import app.gymapp.domain.LibExercise
import app.gymapp.domain.parseLibrary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val Context.store by preferencesDataStore(name = "gym")
private val WORKOUTS = stringPreferencesKey("workouts")
private val SETTINGS = stringPreferencesKey("settings")
private val SERVER = stringPreferencesKey("server")
private val OUTBOX = stringPreferencesKey("outbox")
private val POSTURE = stringPreferencesKey("posture_days")
private val SWAPS = stringPreferencesKey("plan_swaps")
private val RUNS = stringPreferencesKey("runs")

private val workoutList = ListSerializer(Workout.serializer())
private val runList = ListSerializer(Run.serializer())
private val outboxList = ListSerializer(OutboxEntry.serializer())
private val postureList = ListSerializer(String.serializer())
private val swapMap = MapSerializer(String.serializer(), Int.serializer())

/**
 * Local-first state: every write lands on the phone first (the gym has bad signal), then an
 * outbox replays it to the backend in order. The server is the truth once the outbox is empty.
 */
object Repo {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncLock = Mutex()
    private lateinit var appContext: Context

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    private val _runs = MutableStateFlow<List<Run>>(emptyList())
    private val _settings = MutableStateFlow(Settings())
    private val _server = MutableStateFlow(Server())
    private val _outbox = MutableStateFlow<List<OutboxEntry>>(emptyList())
    private val _syncError = MutableStateFlow<String?>(null)
    private val _syncing = MutableStateFlow(false)
    // ponytail: posture history is phone-only; add a backend collection when the Today dashboard goes web too.
    private val _postureDays = MutableStateFlow<Set<String>>(emptySet())
    // ponytail: exercise swaps are phone-only, like posture days.
    private val _swaps = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _library = MutableStateFlow<List<LibExercise>>(emptyList())

    val workouts: StateFlow<List<Workout>> = _workouts
    val runs: StateFlow<List<Run>> = _runs
    val settings: StateFlow<Settings> = _settings
    val server: StateFlow<Server> = _server
    val outbox: StateFlow<List<OutboxEntry>> = _outbox
    val syncError: StateFlow<String?> = _syncError
    val syncing: StateFlow<Boolean> = _syncing
    /** ISO dates (yyyy-mm-dd) the posture routine was finished. */
    val postureDays: StateFlow<Set<String>> = _postureDays
    /** Per planner slot, how many times "swap" was tapped. */
    val swaps: StateFlow<Map<String, Int>> = _swaps
    /** free-exercise-db, loaded once from assets. */
    val library: StateFlow<List<LibExercise>> = _library

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        scope.launch {
            val prefs = appContext.store.data.first()
            _workouts.value = prefs.read(WORKOUTS, workoutList) ?: emptyList()
            _runs.value = prefs.read(RUNS, runList) ?: emptyList()
            _settings.value = prefs.read(SETTINGS, Settings.serializer()) ?: Settings()
            _server.value = prefs.read(SERVER, Server.serializer()) ?: Server()
            _outbox.value = prefs.read(OUTBOX, outboxList) ?: emptyList()
            _postureDays.value = prefs.read(POSTURE, postureList)?.toSet() ?: emptySet()
            _swaps.value = prefs.read(SWAPS, swapMap) ?: emptyMap()
            _library.value = parseLibrary(appContext.assets.open("exercises.json").bufferedReader().use { it.readText() })
            if (_settings.value.planStart == null) {
                updateSettings { it.copy(planStart = LocalDate.now().with(DayOfWeek.MONDAY).toString()) }
            }
            sync()
        }
    }

    /** Saves a new session, or replaces [editing] when the user edited an old one. */
    fun saveWorkout(exercises: List<Exercise>, editing: Workout? = null) = scope.launch {
        val workout = editing?.copy(exercises = exercises)
            ?: Workout(clientId = UUID.randomUUID().toString(), date = Instant.now().toString(), exercises = exercises)
        _workouts.update { list -> if (editing == null) listOf(workout) + list else list.map { if (it.key == workout.key) workout else it } }
        val entry = if (workout.serverId != null) OutboxEntry("update", serverId = workout.serverId) else OutboxEntry("workout", workout.clientId)
        _outbox.update { box -> if (entry in box) box else box + entry }
        persist()
        sync()
    }

    fun saveRun(run: Run) = scope.launch {
        _runs.update { listOf(run) + it }
        _outbox.update { it + OutboxEntry("run", run.clientId) }
        persist()
        sync()
    }

    fun deleteWorkout(workout: Workout) = scope.launch {
        _workouts.update { list -> list.filterNot { it.key == workout.key } }
        _outbox.update { box ->
            // A never-uploaded session just leaves the queue; an uploaded one needs a server delete.
            val rest = box.filterNot { (it.clientId != null && it.clientId == workout.clientId) || (it.serverId != null && it.serverId == workout.serverId) }
            if (workout.serverId != null) rest + OutboxEntry("delete", serverId = workout.serverId) else rest
        }
        persist()
        sync()
    }

    fun updateSettings(transform: (Settings) -> Settings) = scope.launch {
        _settings.update(transform)
        // Settings are last-writer-wins, so one queued entry is enough.
        _outbox.update { box -> box.filterNot { it.kind == "settings" } + OutboxEntry("settings") }
        persist()
        sync()
    }

    fun setServer(server: Server) = scope.launch {
        _server.value = server.copy(url = server.url.trim(), token = server.token.trim())
        persist()
        sync()
    }

    fun logPosture() = scope.launch {
        _postureDays.update { it + LocalDate.now().toString() }
        persist()
    }

    fun swap(slotKey: String) = scope.launch {
        _swaps.update { it + (slotKey to (it[slotKey] ?: 0) + 1) }
        persist()
    }

    fun syncNow() = scope.launch { sync() }

    private suspend fun sync() = syncLock.withLock {
        val server = _server.value
        if (!server.configured) return
        _syncing.value = true
        try {
            // Replay in order and stop at the first failure, so nothing is skipped or reordered.
            for (entry in _outbox.value) {
                when (entry.kind) {
                    "workout" -> _workouts.value.find { it.clientId == entry.clientId }?.let { local ->
                        val saved = Api.call(server, "POST", "/api/workouts", Workout.serializer(), json.encodeToString(Workout.serializer(), local.copy(serverId = null)))
                        // Remember the server id now, so a delete before the next pull still reaches the server.
                        _workouts.update { list -> list.map { if (it.clientId == local.clientId) it.copy(serverId = saved.serverId) else it } }
                        // Deleted while the upload was in flight: chase it with a server delete.
                        if (_workouts.value.none { it.clientId == local.clientId } && saved.serverId != null) {
                            _outbox.update { it + OutboxEntry("delete", serverId = saved.serverId) }
                        }
                    }
                    "update" -> _workouts.value.find { it.serverId == entry.serverId }?.let {
                        Api.call(server, "PUT", "/api/workouts/${entry.serverId}", Workout.serializer(), json.encodeToString(Workout.serializer(), it.copy(serverId = null)))
                    }
                    "delete" -> Api.call(server, "DELETE", "/api/workouts/${entry.serverId}", JsonObject.serializer())
                    "run" -> _runs.value.find { it.clientId == entry.clientId }?.let {
                        Api.call(server, "POST", "/api/runs", Run.serializer(), json.encodeToString(Run.serializer(), it.copy(serverId = null)))
                    }
                    "settings" -> Api.call(server, "POST", "/api/settings", Settings.serializer(), json.encodeToString(Settings.serializer(), _settings.value))
                }
                _outbox.update { it - entry }
                persist()
            }
            val remote = Api.call(server, "GET", "/api/workouts", workoutList)
            val remoteSettings = Api.call(server, "GET", "/api/settings", Settings.serializer())
            val remoteRuns = Api.call(server, "GET", "/api/runs", runList)
            // Anything saved while this sync ran is still queued: keep it rather than let the pull erase it.
            val pending = _outbox.value
            _workouts.update { local -> merge(local, remote, pending) }
            _runs.update { local ->
                val queued = pending.filter { it.kind == "run" }.mapNotNull { it.clientId }.toSet()
                local.filter { it.clientId in queued && remoteRuns.none { r -> r.clientId == it.clientId } } + remoteRuns
            }
            if (pending.none { it.kind == "settings" }) _settings.value = remoteSettings
            persist()
            _syncError.value = null
        } catch (t: Throwable) {
            _syncError.value = t.message ?: t::class.simpleName
        } finally {
            _syncing.value = false
        }
    }

    private suspend fun persist() {
        appContext.store.edit {
            it[WORKOUTS] = json.encodeToString(workoutList, _workouts.value)
            it[RUNS] = json.encodeToString(runList, _runs.value)
            it[SETTINGS] = json.encodeToString(Settings.serializer(), _settings.value)
            it[SERVER] = json.encodeToString(Server.serializer(), _server.value)
            it[OUTBOX] = json.encodeToString(outboxList, _outbox.value)
            it[POSTURE] = json.encodeToString(postureList, _postureDays.value.toList())
            it[SWAPS] = json.encodeToString(swapMap, _swaps.value)
        }
    }

    private fun <T> Preferences.read(key: Preferences.Key<String>, serializer: KSerializer<T>): T? =
        this[key]?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }

    /** Server list, but with this phone's still-queued creates, edits and deletes applied on top. */
    internal fun merge(local: List<Workout>, remote: List<Workout>, pending: List<OutboxEntry>): List<Workout> {
        val creates = pending.filter { it.kind == "workout" }.mapNotNull { it.clientId }.toSet()
        val updates = pending.filter { it.kind == "update" }.mapNotNull { it.serverId }.toSet()
        val deletes = pending.filter { it.kind == "delete" }.mapNotNull { it.serverId }.toSet()
        val newOnes = local.filter { it.clientId in creates && remote.none { r -> r.clientId == it.clientId } }
        val merged = remote.filterNot { it.serverId in deletes }.map { r ->
            if (r.serverId in updates) local.find { it.serverId == r.serverId } ?: r else r
        }
        return newOnes + merged
    }
}
