package com.medtrack.ui.scan

import com.medtrack.data.title
import com.medtrack.lookup.formatComposition
import com.medtrack.lookup.Brand
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.medtrack.AppContainer
import com.medtrack.data.PhotoStore
import com.medtrack.lookup.PrescriptionMatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Where a scan is: the warning, waiting for a photo, cropping it, reading it, or showing what was found. */
sealed interface ScanStep {
    data object Warning : ScanStep
    data object ChoosePhoto : ScanStep
    data class Cropping(val path: String) : ScanStep
    data class Reading(val path: String) : ScanStep
    /** [text] is everything that was read, shown so the user can see why something did or didn't match. */
    data class Found(val path: String, val matches: List<PrescriptionMatch>, val text: String) : ScanStep
    data class Failed(val message: String) : ScanStep
}

/**
 * Reads a photo of a prescription on the phone (ML Kit, offline) and matches its words to the brand list. The
 * photo is only kept while the scan screen is open: it is deleted when the screen closes.
 */
class ScanViewModel(private val c: AppContainer) : ViewModel() {
    private val _step = MutableStateFlow<ScanStep>(ScanStep.Warning)
    val step = _step.asStateFlow()

    /** The photo being scanned, deleted when it is replaced or the screen closes. */
    private var photo: String? = null

    fun acceptWarning() = _step.update { ScanStep.ChoosePhoto }

    fun onPicked(uris: List<Uri>) {
        val uri = uris.firstOrNull() ?: return
        viewModelScope.launch {
            val path = try {
                c.photos.importFrom(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MedTrack", "Couldn't load the prescription photo", e)
                _step.value = ScanStep.Failed("Couldn't load that photo. Try another one.")
                return@launch
            }
            replacePhoto(path)
            _step.value = ScanStep.Cropping(path)
        }
    }

    /** The crop step is done ([paths] holds the cropped or whole photo): read it. */
    fun onCropped(paths: List<String>) {
        val path = paths.firstOrNull() ?: return run { _step.value = ScanStep.ChoosePhoto }
        replacePhoto(path)
        _step.value = ScanStep.Reading(path)
        viewModelScope.launch {
            _step.value = try {
                val text = readText(path)
                val saved = c.db.medicationDao().all()
                    .map { Brand(it.title, it.ingredients.formatComposition().ifBlank { it.genericName }, null, saved = true) }
                ScanStep.Found(path, c.brands.matchPrescription(text, saved), text.trim())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MedTrack", "Reading the prescription failed", e)
                ScanStep.Failed("Couldn't read that photo. Try again with the prescription flat and well lit.")
            }
        }
    }

    fun scanAnother() {
        replacePhoto(null)
        _step.value = ScanStep.ChoosePhoto
    }

    private fun replacePhoto(path: String?) {
        photo?.takeIf { it != path }?.let(c.photos::delete)
        photo = path
    }

    override fun onCleared() {
        photo?.let(c.photos::delete)
    }

    private suspend fun readText(path: String): String {
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
}
