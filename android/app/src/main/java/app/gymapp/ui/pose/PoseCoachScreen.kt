package app.gymapp.ui.pose

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.gymapp.domain.Joint
import app.gymapp.domain.PoseCheck
import app.gymapp.domain.Poses
import app.gymapp.domain.Pt
import app.gymapp.domain.Side
import app.gymapp.domain.checkPose
import app.gymapp.ui.Muted
import app.gymapp.ui.design.Accent
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.io.File
import java.util.concurrent.Executors

private val bones = listOf(11 to 13, 13 to 15, 12 to 14, 14 to 16, 11 to 12, 11 to 23, 12 to 24, 23 to 24, 23 to 25, 25 to 27, 24 to 26, 26 to 28)
private const val HOLD_MS = 1200L

/** One analysed frame: upright (and mirrored for the selfie camera) so it matches what you see. */
private class Frame(val bitmap: Bitmap, val points: List<Pt>)

@Composable
fun PoseCoachScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) Coach(onClose) else Column(Modifier.statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Camera needed", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Muted("The posing coach looks at your pose live. Frames are analysed on this phone and never uploaded.")
            Button(onClick = { ask.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
            Button(onClick = onClose) { Text("Back") }
        }
    }
}

@Composable
private fun Coach(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val accent = Accent.Train.color
    var poseIndex by rememberSaveable { mutableIntStateOf(1) }
    var front by rememberSaveable { mutableStateOf(true) }
    var frame by remember { mutableStateOf<Frame?>(null) }
    var goodSince by remember { mutableLongStateOf(0L) }
    var lastShot by remember { mutableLongStateOf(0L) }
    var flash by remember { mutableStateOf(false) }
    var gallery by remember { mutableIntStateOf(0) } // bump to reload thumbnails
    val pose = Poses[poseIndex]
    val check: PoseCheck? = frame?.let { checkPose(it.points, pose) }
    val dir = remember { File(context.filesDir, "poses").apply { mkdirs() } }

    val detector = remember {
        PoseLandmarker.createFromOptions(
            context,
            PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build())
                .setRunningMode(RunningMode.VIDEO)
                .setNumPoses(1)
                .build(),
        )
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Bind once per camera choice (not per frame); release the camera and model on leave.
    LaunchedEffect(front) {
        providerFuture.addListener({
            val provider = providerFuture.get()
            val selector = ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build()
            val preview = Preview.Builder().setResolutionSelector(selector).build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(selector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            val mirror = front
            analysis.setAnalyzer(executor) { proxy ->
                proxy.use {
                    val m = Matrix().apply {
                        postRotate(proxy.imageInfo.rotationDegrees.toFloat())
                        if (mirror) postScale(-1f, 1f)
                    }
                    val raw = proxy.toBitmap()
                    val upright = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                    val result = runCatching { detector.detectForVideo(BitmapImageBuilder(upright).build(), SystemClock.uptimeMillis()) }.getOrNull()
                    val pts = result?.landmarks()?.firstOrNull()?.map { Pt(it.x() * upright.width, it.y() * upright.height, it.visibility().orElse(0f)) }.orEmpty()
                    ContextCompat.getMainExecutor(context).execute { frame = Frame(upright, pts) }
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycle, if (mirror) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(context))
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
            runCatching { executor.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS) }
            detector.close()
        }
    }

    // Auto-shutter: a good score held for HOLD_MS saves the frame, then a short cooldown.
    LaunchedEffect(check?.score) {
        val now = SystemClock.elapsedRealtime()
        if ((check?.score ?: 0) >= 90) {
            if (goodSince == 0L) goodSince = now
            if (now - goodSince >= HOLD_MS && now - lastShot > 4000) {
                frame?.bitmap?.let { bmp ->
                    File(dir, "${pose.name.replace(' ', '_')}_${System.currentTimeMillis()}.jpg").outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                }
                lastShot = now
                flash = true
                gallery++
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            goodSince = 0L
        }
    }
    LaunchedEffect(flash) { if (flash) { kotlinx.coroutines.delay(180); flash = false } }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Skeleton overlay, mapped with the same centre-crop the preview uses.
        frame?.let { f ->
            val okJoints = check?.readings.orEmpty().associate { r ->
                val idx = when (r.target.joint) {
                    Joint.Elbow -> if (r.target.side == Side.Left) 13 else 14
                    Joint.Shoulder -> if (r.target.side == Side.Left) 11 else 12
                }
                idx to r.ok
            }
            val good = Accent.Eat.color
            val off = Accent.Skin.color
            Canvas(Modifier.fillMaxSize()) {
                if (f.points.size < 29) return@Canvas
                val scale = maxOf(size.width / f.bitmap.width, size.height / f.bitmap.height)
                val dx = (size.width - f.bitmap.width * scale) / 2
                val dy = (size.height - f.bitmap.height * scale) / 2
                fun o(p: Pt) = Offset(p.x * scale + dx, p.y * scale + dy)
                bones.forEach { (a, b) ->
                    val color = when {
                        okJoints[a] == false || okJoints[b] == false -> off
                        okJoints[a] == true || okJoints[b] == true -> good
                        else -> Color.White.copy(alpha = 0.7f)
                    }
                    drawLine(color, o(f.points[a]), o(f.points[b]), 6.dp.toPx(), cap = StrokeCap.Round)
                }
                listOf(11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28).forEach { i ->
                    drawCircle(Color.White, 5.dp.toPx(), o(f.points[i]))
                }
            }
        }
        val flashAlpha by animateFloatAsState(if (flash) 0.7f else 0f, tween(150), label = "flash")
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAlpha)))

        // Top: close, camera switch, pose picker.
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f))) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White) }
                Text("Posing coach", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f).padding(start = 8.dp))
                IconButton(onClick = { front = !front; frame = null }, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.4f))) {
                    Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch camera", tint = Color.White)
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Poses.size) { i ->
                    FilterChip(
                        selected = i == poseIndex, onClick = { poseIndex = i; goodSince = 0L },
                        label = { Text(Poses[i].name, color = Color.White) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = Color.Black.copy(alpha = 0.4f), selectedContainerColor = accent.copy(alpha = 0.7f)),
                    )
                }
            }
        }

        // Bottom: score, tip, latest shots for this pose.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp)
                .clip(RoundedCornerShape(28.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(check?.let { "${it.score}" } ?: "—", style = MaterialTheme.typography.displayMedium, color = if ((check?.score ?: 0) >= 90) Accent.Eat.color else Color.White)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(check?.tip ?: "Step back until your whole upper body and hips are in frame.", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Muted(pose.how)
                }
            }
            Muted("Hold 90+ for a moment and it takes the photo. Photos stay on this phone.")
            val shots = remember(gallery, poseIndex) {
                dir.listFiles { f -> f.name.startsWith(pose.name.replace(' ', '_')) }.orEmpty().sortedByDescending { it.name }.take(6)
            }
            if (shots.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                shots.forEach { file ->
                    val thumb = remember(file) { BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = 8 }) }
                    thumb?.let { Image(it.asImageBitmap(), contentDescription = "Saved ${pose.name} photo", contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))) }
                }
            }
        }
    }
}
