package com.medtrack.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medtrack.AppContainer
import com.medtrack.data.Ingredient
import com.medtrack.data.MedicationEntity
import com.medtrack.data.SourceStatus
import com.medtrack.ui.common.PhotoThumb
import com.medtrack.ui.common.ScreenScaffold
import com.medtrack.ui.common.containerViewModel
import java.io.File

@Composable
fun EditMedicationScreen(c: AppContainer, id: Long, onDone: () -> Unit) {
    val vm = containerViewModel { EditViewModel(c, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    val context = LocalContext.current
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let(vm::setPhoto)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::setPhoto)
    }
    fun launchCamera() {
        val file = File(File(context.cacheDir, "camera").apply { mkdirs() }, "capture.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        cameraUri = uri
        camera.launch(uri)
    }

    val med = state.med
    ScreenScaffold(
        title = if (id == 0L) "Add medication" else "Edit medication",
        isTab = false,
        onBack = onDone,
        actions = { TextButton(onClick = vm::save, enabled = state.loaded) { Text("Save") } },
    ) { padding ->
        if (!state.loaded) return@ScreenScaffold
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            OutlinedTextField(
                value = med.genericName,
                onValueChange = vm::onNameChanged,
                label = { Text("Generic name (e.g. paracetamol)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            state.suggestions.forEach { s ->
                TextButton(onClick = { vm.pickSuggestion(s) }) { Text(s.name) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { vm.lookUp(onlyMissing = false) }, enabled = med.genericName.isNotBlank() && !state.lookingUp) {
                    Text("Look up info")
                }
                if (state.lookingUp) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
            LookupStatus(med, state.lookingUp) { vm.lookUp(onlyMissing = true) }

            OutlinedTextField(med.brandName.orEmpty(), { v -> vm.update { it.copy(brandName = v) } }, Modifier.fillMaxWidth(), label = { Text("Brand name (optional)") }, singleLine = true)
            OutlinedTextField(med.doseText.orEmpty(), { v -> vm.update { it.copy(doseText = v) } }, Modifier.fillMaxWidth(), label = { Text("Dose (e.g. 1 tablet, 40 mg)") }, singleLine = true)

            Text("Active ingredients", style = MaterialTheme.typography.titleMedium)
            IngredientsEditor(med.ingredients) { list -> vm.update { it.copy(ingredients = list) } }
            if (med.rxnormStatus == SourceStatus.OK) SourceNote("Ingredients from RxNorm")
            if (med.pubchemStatus == SourceStatus.OK) SourceNote("Formulas from PubChem")

            OutlinedTextField(med.usedFor.orEmpty(), { v -> vm.update { it.copy(usedFor = v) } }, Modifier.fillMaxWidth(), label = { Text("Used for") }, minLines = 2)
            if (med.medlineStatus == SourceStatus.OK) SourceNote("From MedlinePlus")
            SourceNote("General information, not medical advice.")

            OutlinedTextField(med.notes.orEmpty(), { v -> vm.update { it.copy(notes = v) } }, Modifier.fillMaxWidth(), label = { Text("My notes (how I take it)") }, minLines = 2)

            HorizontalDivider()
            Text("Photo", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoThumb(med.photoPath, med.genericName.ifBlank { "?" }, 72.dp)
                OutlinedButton(onClick = ::launchCamera) { Text("Camera") }
                OutlinedButton(onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Gallery") }
                if (med.photoPath != null) TextButton(onClick = vm::removePhoto) { Text("Remove") }
            }

            HorizontalDivider()
            Text("Schedule", style = MaterialTheme.typography.titleMedium)
            ScheduleEditor(med.schedule) { s -> vm.update { it.copy(schedule = s) } }

            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}

@Composable
private fun LookupStatus(med: MedicationEntity, lookingUp: Boolean, onRetry: () -> Unit) {
    if (lookingUp) return
    val problems = buildList {
        when (med.rxnormStatus) {
            SourceStatus.NOT_FOUND -> add("Couldn't find this name – check the spelling or enter details manually.")
            SourceStatus.FAILED -> add("Not looked up yet – tap to retry")
            else -> Unit
        }
        if (med.pubchemStatus == SourceStatus.FAILED) add("Formula lookup failed – tap to retry")
        if (med.medlineStatus == SourceStatus.FAILED) add("Usage note lookup failed – tap to retry")
    }
    problems.forEach { message ->
        if (message.endsWith("retry")) {
            TextButton(onClick = onRetry) { Text(message, color = MaterialTheme.colorScheme.error) }
        } else {
            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SourceNote(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

@Composable
private fun IngredientsEditor(ingredients: List<Ingredient>, onChange: (List<Ingredient>) -> Unit) {
    ingredients.forEachIndexed { index, ingredient ->
        fun set(updated: Ingredient) = onChange(ingredients.toMutableList().also { it[index] = updated })
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(ingredient.name, { set(ingredient.copy(name = it)) }, Modifier.weight(1f), label = { Text("Ingredient") }, singleLine = true)
                    IconButton(onClick = { onChange(ingredients.filterIndexed { i, _ -> i != index }) }) {
                        Icon(Icons.Default.Delete, "Remove ingredient")
                    }
                }
                OutlinedTextField(ingredient.strength.orEmpty(), { set(ingredient.copy(strength = it.ifEmpty { null })) }, Modifier.fillMaxWidth(), label = { Text("Strength") }, singleLine = true)
                OutlinedTextField(ingredient.formula.orEmpty(), { set(ingredient.copy(formula = it.ifEmpty { null })) }, Modifier.fillMaxWidth(), label = { Text("Chemical formula") }, singleLine = true)
            }
        }
    }
    TextButton(onClick = { onChange(ingredients + Ingredient("")) }) { Text("Add ingredient") }
}
