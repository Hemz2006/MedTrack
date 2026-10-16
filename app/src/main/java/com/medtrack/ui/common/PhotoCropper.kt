package com.medtrack.ui.common

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.medtrack.data.PhotoStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * After photos are added, offers to crop each one in turn. Cropping replaces the photo's file; "Use whole photo"
 * keeps it as taken. [onFinished] gets the final paths, in order, once every photo has been seen.
 * The [pending] files must be new (not yet saved anywhere), since a cropped original is deleted.
 */
@Composable
fun PhotoCropFlow(pending: List<String>, photos: PhotoStore, onFinished: (List<String>) -> Unit) {
    var index by remember(pending) { mutableIntStateOf(0) }
    val done = remember(pending) { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }

    LaunchedEffect(index, pending) { if (index >= pending.size) onFinished(done.toList()) }
    val path = pending.getOrNull(index) ?: return

    CropScreen(
        path = path,
        title = if (pending.size > 1) "Crop photo ${index + 1} of ${pending.size}" else "Crop photo",
        working = working,
        onUseWhole = { done += path; index++ },
        onCrop = { frame ->
            working = true
            scope.launch {
                val cropped = try {
                    photos.crop(path, frame.left, frame.top, frame.right, frame.bottom).also { photos.delete(path) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("MedTrack", "Cropping failed; keeping the whole photo", e)
                    path
                }
                done += cropped
                working = false
                index++
            }
        },
    )
}

@Composable
private fun CropScreen(path: String, title: String, working: Boolean, onUseWhole: () -> Unit, onCrop: (CropFrame) -> Unit) {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { PhotoStore.decodeSampled(path, 1600)?.asImageBitmap() }
    }
    var frame by remember(path) { mutableStateOf(CropFrame.START) }

    Dialog(onDismissRequest = onUseWhole, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(16.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(
                "Drag the frame to move it, or its corners to resize.",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val image = bitmap
                if (image == null) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    // Fit the photo in the space, keeping its shape.
                    val scale = minOf(constraints.maxWidth.toFloat() / image.width, constraints.maxHeight.toFloat() / image.height)
                    val density = LocalDensity.current
                    val w = with(density) { (image.width * scale).toDp() }
                    val h = with(density) { (image.height * scale).toDp() }
                    Box(Modifier.size(w, h)) {
                        Image(image, contentDescription = "Photo to crop", modifier = Modifier.fillMaxSize())
                        CropOverlay(frame, onChange = { frame = it })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onUseWhole, enabled = !working) { Text("Use whole photo", color = Color.White) }
                Button(
                    onClick = { onCrop(frame) },
                    enabled = bitmap != null && !working,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Crop") }
            }
        }
    }
}

/** The dimmed surround, the frame with rule-of-thirds lines, and corner handles; drags move or resize it. */
@Composable
private fun CropOverlay(frame: CropFrame, onChange: (CropFrame) -> Unit) {
    val reach = with(LocalDensity.current) { 28.dp.toPx() }
    var handle by remember { mutableStateOf<CropHandle?>(null) }
    var current by remember { mutableStateOf(frame) }
    current = frame
    Canvas(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Crop frame" }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { at ->
                        handle = current.handleAt(at.x / size.width, at.y / size.height, reach / size.width, reach / size.height)
                    },
                    onDragEnd = { handle = null },
                    onDragCancel = { handle = null },
                ) { change, drag ->
                    val h = handle ?: return@detectDragGestures
                    change.consume()
                    onChange(current.resized(h, drag.x / size.width, drag.y / size.height))
                }
            },
    ) {
        val l = frame.left * size.width
        val t = frame.top * size.height
        val r = frame.right * size.width
        val b = frame.bottom * size.height
        val dim = Color.Black.copy(alpha = 0.55f)
        drawRect(dim, Offset.Zero, Size(size.width, t))
        drawRect(dim, Offset(0f, b), Size(size.width, size.height - b))
        drawRect(dim, Offset(0f, t), Size(l, b - t))
        drawRect(dim, Offset(r, t), Size(size.width - r, b - t))
        drawRect(Color.White, Offset(l, t), Size(r - l, b - t), style = Stroke(2.dp.toPx()))
        val thirds = Color.White.copy(alpha = 0.4f)
        for (i in 1..2) {
            val x = l + (r - l) * i / 3
            val y = t + (b - t) * i / 3
            drawLine(thirds, Offset(x, t), Offset(x, b), 1.dp.toPx())
            drawLine(thirds, Offset(l, y), Offset(r, y), 1.dp.toPx())
        }
        val arm = 18.dp.toPx()
        val stroke = 4.dp.toPx()
        for ((cx, cy, sx, sy) in listOf(Quad(l, t, 1f, 1f), Quad(r, t, -1f, 1f), Quad(l, b, 1f, -1f), Quad(r, b, -1f, -1f))) {
            drawLine(Color.White, Offset(cx, cy), Offset(cx + arm * sx, cy), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(cx, cy), Offset(cx, cy + arm * sy), stroke, StrokeCap.Round)
        }
    }
}

private data class Quad(val x: Float, val y: Float, val dx: Float, val dy: Float)
