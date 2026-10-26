package app.gymapp.ui.posture

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import app.gymapp.ui.design.Accent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.gymapp.data.PostureScan
import app.gymapp.data.Repo
import app.gymapp.domain.Pt
import app.gymapp.domain.ScanResult
import app.gymapp.domain.analyzeFront
import app.gymapp.domain.analyzeSide
import app.gymapp.domain.headForwardLabel
import app.gymapp.domain.tiltLabel
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Decodes upright (ImageDecoder applies EXIF rotation), capped near 1280 px, as a software bitmap MediaPipe can read. */
private fun decode(file: File): Bitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val longest = maxOf(info.size.width, info.size.height)
        if (longest > 1280) decoder.setTargetSampleSize(longest / 1280)
    }

private fun landmarks(context: Context, bitmap: Bitmap): List<Pt> =
    PoseLandmarker.createFromFile(context, "pose_landmarker_lite.task").use { detector ->
        val pose = detector.detect(BitmapImageBuilder(bitmap).build()).landmarks().firstOrNull().orEmpty()
        pose.map { Pt(it.x() * bitmap.width, it.y() * bitmap.height, it.visibility().orElse(0f)) }
    }

@Composable
fun PostureScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scans by Repo.scans.collectAsState()
    // Saveable: the camera app can push this process out of memory while the photo is taken.
    var kind by rememberSaveable { mutableStateOf("side") }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<ScanResult?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun analyze(file: File) {
        busy = true
        result = null
        scope.launch {
            val (bmp, r) = withContext(Dispatchers.Default) {
                runCatching {
                    val bmp = decode(file)
                    val pts = landmarks(context, bmp)
                    bmp to if (kind == "side") analyzeSide(pts) else analyzeFront(pts)
                }.getOrElse { null to ScanResult.Unclear("Couldn't read that photo: ${it.message}") }
            }
            bitmap = bmp
            result = r
            busy = false
            when (r) {
                is ScanResult.Side -> Repo.addScan(PostureScan(Instant.now().toString(), "side", headForward = r.headForward, upperBack = r.upperBack, photo = file.name))
                is ScanResult.Front -> Repo.addScan(PostureScan(Instant.now().toString(), "front", shoulderTilt = r.shoulderTilt, hipTilt = r.hipTilt, photo = file.name))
                else -> file.delete()
            }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = photoPath?.let(::File) ?: return@rememberLauncherForActivityResult
        if (ok) analyze(file) else file.delete()
    }
    fun shoot(which: String) {
        kind = which
        val file = File(File(context.filesDir, "scans").apply { mkdirs() }, "${which}_${System.currentTimeMillis()}.jpg")
        photoPath = file.path
        camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", file))
    }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                Text("Posture scan", style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            Panel {
                Text("How to take the photo", style = MaterialTheme.typography.titleMedium)
                Muted("Phone at chest height, 2-3 m away, whole body in frame. Use the camera's timer or ask someone. Stand relaxed, the way you normally stand. Fitted clothes help.")
                Muted("Side photo: turn 90° so one shoulder points at the camera. Front photo: face the camera.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Button(onClick = { shoot("side") }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Side photo") }
                    OutlinedButton(onClick = { shoot("front") }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Front photo") }
                }
                Muted("Photos stay on this phone. Only the angles are saved.")
            }
        }
        if (busy) item {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        result?.let { r ->
            item {
                val bmp = bitmap
                if (bmp != null && r !is ScanResult.Unclear) {
                    val accent = MaterialTheme.colorScheme.primary
                    Box(Modifier.fillMaxWidth().aspectRatio(bmp.width / bmp.height.toFloat()).clip(RoundedCornerShape(20.dp))) {
                        Image(bmp.asImageBitmap(), contentDescription = "Your posture photo", modifier = Modifier.fillMaxSize())
                        Canvas(Modifier.fillMaxSize()) {
                            fun o(p: Pt) = Offset(p.x / bmp.width * size.width, p.y / bmp.height * size.height)
                            val stroke = 4.dp.toPx()
                            when (r) {
                                is ScanResult.Side -> {
                                    // Measured line plus a plumb line from the shoulder for comparison.
                                    drawLine(Color.White.copy(alpha = 0.6f), o(r.shoulder), o(r.shoulder).copy(y = 0f), stroke / 2)
                                    drawLine(accent, o(r.hip), o(r.shoulder), stroke)
                                    drawLine(accent, o(r.shoulder), o(r.ear), stroke)
                                    listOf(r.ear, r.shoulder, r.hip).forEach { drawCircle(Color.White, stroke * 1.6f, o(it)) }
                                }
                                is ScanResult.Front -> {
                                    val (ls, rs, lh, rh) = r.points
                                    drawLine(accent, o(ls), o(rs), stroke)
                                    drawLine(accent, o(lh), o(rh), stroke)
                                    r.points.forEach { drawCircle(Color.White, stroke * 1.6f, o(it)) }
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
            item {
                Panel {
                    when (r) {
                        is ScanResult.Side -> {
                            val first = scans.filter { it.kind == "side" }.minByOrNull { it.date }?.headForward
                            Text("Head forward %.0f°".format(r.headForward), style = MaterialTheme.typography.headlineSmall)
                            Muted(headForwardLabel(r.headForward))
                            Text("Upper body lean %.0f°".format(r.upperBack), style = MaterialTheme.typography.titleMedium)
                            Muted("How far your shoulders sit ahead of your hips. Lower is more upright.")
                            if (first != null && scans.count { it.kind == "side" } > 1) {
                                val diff = first - r.headForward
                                Muted(if (diff >= 0) "%.0f° better than your first scan.".format(diff) else "%.0f° more forward than your first scan.".format(-diff))
                            }
                        }
                        is ScanResult.Front -> {
                            Text("Symmetry", style = MaterialTheme.typography.headlineSmall)
                            Muted(tiltLabel("shoulder", r.shoulderTilt))
                            Muted(tiltLabel("hip", r.hipTilt))
                        }
                        is ScanResult.Unclear -> {
                            Text("Try again", style = MaterialTheme.typography.headlineSmall)
                            Muted(r.reason)
                        }
                    }
                    Muted("Angles come from on-device pose detection. Use them as a trend, not a diagnosis.", Modifier.padding(top = 6.dp))
                }
            }
        }
        val sides = scans.filter { it.kind == "side" }.sortedBy { it.date }
        if (sides.size >= 2) item { ThenAndNow(context, sides.first(), sides.last()) }
        if (sides.size >= 2) item {
            // Head-forward trend: lower is better, so a falling line is progress.
            val values = sides.mapNotNull { it.headForward }
            val accent = Accent.Posture.color
            Panel {
                Text("Head-forward trend", style = MaterialTheme.typography.titleMedium)
                Canvas(Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp)) {
                    val lo = values.min() - 2
                    val hi = values.max() + 2
                    val pts = values.mapIndexed { i, v -> Offset(i * size.width / (values.size - 1).coerceAtLeast(1), size.height * (v - lo) / (hi - lo)) }
                    pts.zipWithNext { a, b -> drawLine(accent, a, b, 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round) }
                    pts.forEach { drawCircle(accent, 5.dp.toPx(), it) }
                }
                Muted("%.0f° first · %.0f° now · lower is better".format(values.first(), values.last()))
            }
        }
        if (scans.isNotEmpty()) {
            item { Text("History", style = MaterialTheme.typography.titleLarge) }
            items(scans.sortedByDescending { it.date }) { s ->
                Row {
                    Muted(Instant.parse(s.date).atZone(ZoneId.systemDefault()).format(dayFormat), Modifier.weight(1f))
                    Text(
                        if (s.kind == "side") "Head %.0f° · lean %.0f°".format(s.headForward, s.upperBack)
                        else "Shoulders %.1f° · hips %.1f°".format(s.shoulderTilt, s.hipTilt),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

/** First vs latest side photo, the "progress gallery" view. Photos are read from app storage only. */
@Composable
private fun ThenAndNow(context: Context, first: PostureScan, latest: PostureScan) {
    var images by remember { mutableStateOf<Pair<Bitmap?, Bitmap?>>(null to null) }
    LaunchedEffect(first.photo, latest.photo) {
        images = withContext(Dispatchers.IO) {
            fun load(name: String) = File(File(context.filesDir, "scans"), name).takeIf { it.exists() }
                ?.let { runCatching { ImageDecoder.decodeBitmap(ImageDecoder.createSource(it)) { d, info, _ -> d.setTargetSampleSize(maxOf(1, maxOf(info.size.width, info.size.height) / 700)) } }.getOrNull() }
            load(first.photo) to load(latest.photo)
        }
    }
    Panel(accent = Accent.Posture.color) {
        Text("Then and now", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(first to images.first, latest to images.second).forEachIndexed { i, (scan, bmp) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        bmp?.let { Image(it.asImageBitmap(), contentDescription = if (i == 0) "First posture photo" else "Latest posture photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    }
                    Text(if (i == 0) "First" else "Latest", style = MaterialTheme.typography.labelLarge, color = Accent.Posture.color)
                    Muted(Instant.parse(scan.date).atZone(ZoneId.systemDefault()).format(dayFormat))
                    Text("Head %.0f°".format(scan.headForward), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
