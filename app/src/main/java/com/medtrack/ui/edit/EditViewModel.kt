package com.medtrack.ui.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtrack.AppContainer
import com.medtrack.data.MedicationEntity
import com.medtrack.data.newMedication
import com.medtrack.lookup.Suggestion
import com.medtrack.lookup.applyLookup
import com.medtrack.schedule.validationError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class EditState(
    val loaded: Boolean = false,
    val med: MedicationEntity,
    val suggestions: List<Suggestion> = emptyList(),
    val lookingUp: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

class EditViewModel(private val c: AppContainer, private val id: Long) : ViewModel() {
    private val _state = MutableStateFlow(EditState(med = newMedication(LocalDate.now(), Instant.now())))
    val state = _state.asStateFlow()

    /** The photo stored in the database; a new photo replaces it only on save. */
    private var savedPhoto: String? = null
    private var suggestJob: Job? = null

    init {
        if (id == 0L) {
            _state.update { it.copy(loaded = true) }
        } else {
            viewModelScope.launch {
                val existing = c.db.medicationDao().get(id)
                savedPhoto = existing?.photoPath
                _state.update { s -> if (existing == null) s.copy(loaded = true) else s.copy(med = existing, loaded = true) }
            }
        }
    }

    fun update(transform: (MedicationEntity) -> MedicationEntity) =
        _state.update { it.copy(med = transform(it.med), error = null) }

    fun onNameChanged(name: String) {
        update { it.copy(genericName = name, rxcui = null) } // a typed name no longer matches a picked suggestion
        suggestJob?.cancel()
        if (name.trim().length < 3) {
            _state.update { it.copy(suggestions = emptyList()) }
            return
        }
        suggestJob = viewModelScope.launch {
            delay(400)
            val suggestions = c.lookup.suggest(name)
            _state.update { it.copy(suggestions = suggestions) }
        }
    }

    fun pickSuggestion(suggestion: Suggestion) {
        suggestJob?.cancel()
        _state.update { it.copy(suggestions = emptyList(), med = it.med.copy(genericName = suggestion.name, rxcui = suggestion.rxcui)) }
        lookUp(onlyMissing = false)
    }

    /** [onlyMissing] retries failed sources without overwriting fields that already came back OK. */
    fun lookUp(onlyMissing: Boolean) {
        val med = _state.value.med
        val name = med.genericName.trim()
        if (name.isEmpty() || _state.value.lookingUp) return
        _state.update { it.copy(lookingUp = true, suggestions = emptyList()) }
        viewModelScope.launch {
            try {
                c.lookup.lookup(name, med.rxcui) { partial ->
                    _state.update { s -> s.copy(med = s.med.applyLookup(partial, onlyMissing)) }
                }
            } finally {
                _state.update { it.copy(lookingUp = false) }
            }
        }
    }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val path = try {
                c.photos.importFrom(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Couldn't load that photo") }
                return@launch
            }
            discardUnsavedPhoto()
            update { it.copy(photoPath = path) }
        }
    }

    fun removePhoto() {
        discardUnsavedPhoto()
        update { it.copy(photoPath = null) }
    }

    fun save() {
        val med = _state.value.med
        val error = if (med.genericName.isBlank()) "Enter the medication name" else med.schedule.validationError()
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            c.medications.save(
                med.copy(
                    genericName = med.genericName.trim(),
                    brandName = med.brandName?.trim()?.ifEmpty { null },
                    doseText = med.doseText?.trim()?.ifEmpty { null },
                    notes = med.notes?.trim()?.ifEmpty { null },
                ),
            )
            if (savedPhoto != med.photoPath) c.photos.delete(savedPhoto)
            savedPhoto = med.photoPath
            _state.update { it.copy(saved = true) }
        }
    }

    private fun discardUnsavedPhoto() {
        val current = _state.value.med.photoPath
        if (current != savedPhoto) c.photos.delete(current)
    }

    override fun onCleared() {
        if (!_state.value.saved) discardUnsavedPhoto()
    }
}
