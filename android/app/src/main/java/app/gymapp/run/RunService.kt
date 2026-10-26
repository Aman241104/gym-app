package app.gymapp.run

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.gymapp.MainActivity
import app.gymapp.R
import app.gymapp.data.Repo
import app.gymapp.data.Run
import app.gymapp.domain.C25k
import app.gymapp.domain.Interval
import app.gymapp.domain.Phase
import app.gymapp.domain.RunSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

data class RunState(
    val active: Boolean = false,
    val session: RunSession? = null, // null = free run
    val index: Int = 0,
    val phaseLeftMs: Long = 0,
    val elapsedMs: Long = 0,
    val distanceM: Double = 0.0,
    val paused: Boolean = false,
    val gps: Boolean = false,
    /** Set once the run is saved, so the screen can show a summary. */
    val finished: Run? = null,
) {
    val interval: Interval? get() = session?.intervals?.getOrNull(index)
    val next: Interval? get() = session?.intervals?.getOrNull(index + 1)
}

/**
 * Owns a run while it happens. A foreground service (type location) keeps the timer, GPS and
 * voice going with the screen off and the phone in a pocket; the UI only observes [state].
 */
class RunService : Service(), LocationListener {
    companion object {
        private val _state = MutableStateFlow(RunState())
        val state: StateFlow<RunState> = _state

        fun start(context: Context, planCode: String?) {
            _state.value = RunState()
            ContextCompat.startForegroundService(context, Intent(context, RunService::class.java).putExtra("plan", planCode))
        }

        fun togglePause() = _state.update { it.copy(paused = !it.paused) }

        fun stop(context: Context) {
            context.startService(Intent(context, RunService::class.java).setAction("stop"))
        }

        fun dismissSummary() = _state.update { RunState() }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null
    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastFix: Location? = null
    private var startedAt: Instant = Instant.now()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "stop") {
            finish(completed = false)
            return START_NOT_STICKY
        }
        if (_state.value.active) return START_NOT_STICKY
        Repo.init(applicationContext)

        val session = intent?.getStringExtra("plan")?.let { code -> C25k.find { it.code == code } }
        startedAt = Instant.now()
        _state.value = RunState(active = true, session = session, phaseLeftMs = (session?.intervals?.first()?.seconds ?: 0) * 1000L)

        startForeground(1, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "gymapp:run").apply { acquire(3 * 60 * 60 * 1000L) }
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) announce() }
        startGps()
        ticker = scope.launch { tick() }
        return START_NOT_STICKY
    }

    private suspend fun tick() {
        var last = SystemClock.elapsedRealtime()
        var lastNotified = 0L
        while (scope.isActive) {
            delay(250)
            val now = SystemClock.elapsedRealtime()
            val dt = now - last
            last = now
            val s = _state.value
            if (!s.active || s.paused) continue
            _state.value = s.copy(elapsedMs = s.elapsedMs + dt, phaseLeftMs = s.phaseLeftMs - dt)
            val session = s.session
            if (session != null && _state.value.phaseLeftMs <= 0) {
                if (s.index == session.intervals.lastIndex) {
                    say("Workout complete. Great job.")
                    buzz(longArrayOf(0, 400, 200, 400, 200, 400))
                    finish(completed = true)
                    return
                }
                _state.update { it.copy(index = it.index + 1, phaseLeftMs = session.intervals[it.index + 1].seconds * 1000L) }
                buzz(if (_state.value.interval?.phase == Phase.Run) longArrayOf(0, 500) else longArrayOf(0, 200, 150, 200))
                announce()
            }
            if (now - lastNotified > 1000) {
                lastNotified = now
                getSystemService(NotificationManager::class.java).notify(1, notification())
            }
        }
    }

    private fun announce() {
        val s = _state.value
        val i = s.interval ?: return say("Free run started")
        val mins = i.seconds / 60
        val length = if (mins >= 1 && i.seconds % 60 == 0) "$mins minute${if (mins > 1) "s" else ""}" else if (i.seconds < 60) "${i.seconds} seconds" else "%.1f minutes".format(i.seconds / 60f)
        say("${i.phase.label} for $length")
    }

    private fun finish(completed: Boolean) {
        val s = _state.value
        if (s.active) {
            val run = Run(
                clientId = UUID.randomUUID().toString(),
                date = startedAt.toString(),
                plan = s.session?.code,
                durationSec = (s.elapsedMs / 1000).toInt(),
                distanceM = s.distanceM.toInt(),
                // A plan run counts toward Couch-to-5K progress only if it reached the end; free runs always count.
                completed = completed || s.session == null,
            )
            // A few seconds of fiddling isn't a run worth keeping.
            if (run.durationSec >= 60) Repo.saveRun(run)
            _state.value = s.copy(active = false, finished = run)
        }
        ticker?.cancel()
        getSystemService(LocationManager::class.java).removeUpdates(this)
        wakeLock?.takeIf { it.isHeld }?.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        // Let the last spoken line finish before tearing the engine down.
        scope.launch {
            delay(4000)
            stopSelf()
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        scope.cancel()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission") // checked right here; without it the run is timer-only
    private fun startGps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        getSystemService(LocationManager::class.java).requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, this, Looper.getMainLooper())
    }

    override fun onLocationChanged(location: Location) {
        _state.update { it.copy(gps = true) }
        // ponytail: plain accuracy gate; add Kalman smoothing if distances drift on twisty routes.
        if (!location.hasAccuracy() || location.accuracy > 25f) return
        val prev = lastFix
        lastFix = location
        val s = _state.value
        if (prev == null || s.paused || !s.active) return
        val d = prev.distanceTo(location)
        if (d >= 2f && d < 100f) _state.update { it.copy(distanceM = it.distanceM + d) }
    }

    private fun say(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "run")
    }

    private fun buzz(pattern: LongArray) {
        getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel("run") == null) {
            nm.createNotificationChannel(NotificationChannel("run", "Run in progress", NotificationManager.IMPORTANCE_LOW))
        }
        val s = _state.value
        val left = s.phaseLeftMs.coerceAtLeast(0) / 1000
        val title = s.interval?.let { "${it.phase.label} · %d:%02d left".format(left / 60, left % 60) } ?: "Free run"
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "run")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("%.2f km · %s".format(s.distanceM / 1000, formatClock(s.elapsedMs / 1000)))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .build()
    }
}

fun formatClock(seconds: Long): String = if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60) else "%d:%02d".format(seconds / 60, seconds % 60)

/** min/km, or "—" until there's enough distance to mean anything. */
fun formatPace(seconds: Long, meters: Double): String {
    if (meters < 50) return "—"
    val perKm = (seconds / (meters / 1000)).toLong()
    return "%d:%02d /km".format(perKm / 60, perKm % 60)
}
