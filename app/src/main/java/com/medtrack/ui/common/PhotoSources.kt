package com.medtrack.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/** The two ways to add photos: take one with the camera, or choose some from the phone's photos. */
class PhotoSources(val takePhoto: () -> Unit, val choosePhotos: () -> Unit)

/**
 * Launchers for the camera and the photo picker; [onPicked] gets what came back (copy it before the next capture,
 * since the camera always writes to the same cache file). Both screens crop what they get, the same way.
 */
@Composable
fun rememberPhotoSources(onPicked: (List<Uri>) -> Unit): PhotoSources {
    val context = LocalContext.current
    // Saved across activity recreation: the camera app can push MedTrack out of memory while it's open.
    var captureUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) captureUri?.let { onPicked(listOf(it)) }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) onPicked(uris)
    }
    return remember(camera, gallery) {
        PhotoSources(
            takePhoto = {
                val file = File(File(context.cacheDir, "camera").apply { mkdirs() }, "capture.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                captureUri = uri
                camera.launch(uri)
            },
            choosePhotos = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
    }
}

/** "Take a photo" and "Add photos" side by side (wrapping at large text sizes). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoSourceButtons(sources: PhotoSources, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = sources.takePhoto, modifier = Modifier.heightIn(min = 48.dp)) { Text("Take a photo") }
        OutlinedButton(onClick = sources.choosePhotos, modifier = Modifier.heightIn(min = 48.dp)) { Text("Add photos") }
    }
}
