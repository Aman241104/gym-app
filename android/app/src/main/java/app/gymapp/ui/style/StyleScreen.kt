package app.gymapp.ui.style

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.gymapp.data.Item
import app.gymapp.data.Repo
import app.gymapp.data.json
import app.gymapp.domain.Garment
import app.gymapp.domain.GarmentKinds
import app.gymapp.domain.Outfit
import app.gymapp.domain.colourName
import app.gymapp.domain.harmonyHint
import app.gymapp.ui.Muted
import app.gymapp.ui.Panel
import app.gymapp.ui.ScreenHeader
import app.gymapp.ui.design.Accent
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.coroutines.resume

private fun wardrobeDir(context: Context) = File(context.filesDir, "wardrobe").apply { mkdirs() }

/** Cuts the garment out on-device (ML Kit subject segmentation); falls back to the plain photo. */
private suspend fun cutOut(source: Bitmap): Bitmap = suspendCancellableCoroutine { cont ->
    val segmenter = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundBitmap().build())
    segmenter.process(InputImage.fromBitmap(source, 0))
        .addOnSuccessListener { cont.resume(it.foregroundBitmap ?: source) }
        .addOnFailureListener { cont.resume(source) }
        .addOnCompleteListener { segmenter.close() }
}

/** Average colour of the visible (non-transparent) pixels, sampled on a grid. */
private fun mainColour(bmp: Bitmap): Int {
    var r = 0L; var g = 0L; var b = 0L; var n = 0
    val step = maxOf(1, minOf(bmp.width, bmp.height) / 60)
    for (y in 0 until bmp.height step step) for (x in 0 until bmp.width step step) {
        val p = bmp.getPixel(x, y)
        if ((p ushr 24) < 200) continue
        r += p shr 16 and 255; g += p shr 8 and 255; b += p and 255; n++
    }
    if (n == 0) return 0x888888
    return ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleScreen(onClose: () -> Unit) {
    val violet = Accent.Style.color
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val all by Repo.items.collectAsState()
    val docs = all["style"].orEmpty()
    val garments = remember(docs) { docs.filter { it.clientId.startsWith("g-") }.mapNotNull { d -> runCatching { d.clientId to json.decodeFromJsonElement(Garment.serializer(), d.data) }.getOrNull() } }
    val outfits = remember(docs) { docs.filter { it.clientId.startsWith("o-") }.mapNotNull { d -> runCatching { d.clientId to json.decodeFromJsonElement(Outfit.serializer(), d.data) }.getOrNull() } }
    var kind by rememberSaveable { mutableStateOf("Top") }
    var busy by remember { mutableStateOf(false) }
    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    val picked = remember { mutableStateMapOf<String, String>() } // kind -> garment id
    val chip = FilterChipDefaults.filterChipColors(selectedContainerColor = violet.copy(alpha = 0.25f))

    fun add(load: () -> Bitmap) {
        busy = true
        scope.launch {
            val garment = withContext(Dispatchers.Default) {
                runCatching {
                    val cut = cutOut(load())
                    val name = "${UUID.randomUUID()}.png"
                    File(wardrobeDir(context), name).outputStream().use { cut.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    val rgb = mainColour(cut)
                    Garment(kind, colourName(rgb), rgb, name)
                }.getOrNull()
            }
            garment?.let { Repo.putItem("style", Item("g-" + UUID.randomUUID(), json.encodeToJsonElement(Garment.serializer(), it).jsonObject)) }
            busy = false
        }
    }
    fun decode(uri: Uri): Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
        d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val longest = maxOf(info.size.width, info.size.height)
        if (longest > 1024) d.setTargetSampleSize(longest / 1024)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { add { decode(it) } } }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = cameraPath?.let(::File) ?: return@rememberLauncherForActivityResult
        if (ok) add { decode(Uri.fromFile(file)).also { file.delete() } } else file.delete()
    }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) { ScreenHeader("Style", "Your", "wardrobe", violet) }
                IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(top = 20.dp)) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }
        }
        item {
            Panel(accent = violet) {
                Text("Add a piece", style = MaterialTheme.typography.titleLarge)
                Muted("Lay it flat or hang it on a plain wall. The background is removed on your phone.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { GarmentKinds.forEach { k -> FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k) }, colors = chip) } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Button(
                        onClick = {
                            val f = File(wardrobeDir(context), "camera_${System.currentTimeMillis()}.jpg")
                            cameraPath = f.path
                            camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", f))
                        },
                        enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = violet), modifier = Modifier.weight(1f),
                    ) { Text("Camera") }
                    OutlinedButton(onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Gallery") }
                }
                if (busy) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = violet, strokeWidth = 2.dp)
                    Muted("Cutting it out…")
                }
            }
        }
        // Outfit builder: one row per kind, tap to pick.
        item { Text("Build an outfit", style = MaterialTheme.typography.titleLarge) }
        GarmentKinds.forEach { k ->
            val pieces = garments.filter { it.second.kind == k }
            if (pieces.isNotEmpty()) item(key = "row-$k") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Muted(k)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(pieces, key = { it.first }) { (id, g) ->
                            GarmentThumb(context, g, selected = picked[k] == id, onClick = { if (picked[k] == id) picked.remove(k) else picked[k] = id }, onLongClick = { Repo.deleteItem("style", id) })
                        }
                    }
                }
            }
        }
        if (garments.isEmpty()) item { Muted("Your pieces show up here. Long-press a piece later to remove it.") }
        if (picked.isNotEmpty()) item {
            val chosen = picked.values.mapNotNull { id -> garments.find { it.first == id }?.second }
            Panel(accent = violet) {
                Text(chosen.joinToString(" · ") { "${it.colour} ${it.kind.lowercase()}" }, style = MaterialTheme.typography.titleMedium)
                Muted(harmonyHint(chosen))
                TextButton(onClick = {
                    Repo.putItem("style", Item("o-" + UUID.randomUUID(), json.encodeToJsonElement(Outfit.serializer(), Outfit(picked.toMap(), LocalDate.now().toString())).jsonObject))
                    picked.clear()
                }) { Text("Save outfit", color = violet) }
            }
        }
        if (outfits.isNotEmpty()) item { Text("Saved outfits", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp)) }
        items(outfits.sortedByDescending { it.second.savedOn }, key = { it.first }) { (id, o) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GarmentKinds.mapNotNull { k -> o.pieces[k]?.let { gid -> garments.find { it.first == gid }?.second } }.forEach { g ->
                    GarmentThumb(context, g, selected = false, onClick = {}, onLongClick = {}, size = 56)
                }
                Box(Modifier.weight(1f))
                TextButton(onClick = { Repo.deleteItem("style", id) }) { Text("Remove") }
            }
        }
        item { Muted("Photos of your clothes stay on this phone; only their labels sync.", Modifier.padding(top = 8.dp)) }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun GarmentThumb(context: Context, g: Garment, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, size: Int = 92) {
    val bmp = remember(g.photo) { File(wardrobeDir(context), g.photo).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path, BitmapFactory.Options().apply { inSampleSize = 4 }) } }
    val violet = Accent.Style.color
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(size.dp).clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(if (selected) 3.dp else 0.dp, if (selected) violet else Color.Transparent, RoundedCornerShape(18.dp))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center,
        ) {
            bmp?.let { Image(it.asImageBitmap(), contentDescription = "${g.colour} ${g.kind}", contentScale = ContentScale.Fit, modifier = Modifier.size((size - 8).dp)) }
        }
        if (size > 60) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF000000 or g.rgb.toLong())))
            Muted(g.colour)
        }
    }
}
