package com.medtrack.lookup

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.medtrack.data.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** All the text ML Kit can read in the photo at [path], on the phone (offline). Throws if the photo can't be read. */
suspend fun readTextIn(path: String): String {
    val bitmap = withContext(Dispatchers.IO) { PhotoStore.decodeSampled(path, 2000) } ?: error("Could not read the photo")
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        return suspendCancellableCoroutine { cont ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    } finally {
        recognizer.close()
    }
}
