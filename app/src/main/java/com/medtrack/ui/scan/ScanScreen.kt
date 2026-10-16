package com.medtrack.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medtrack.AppContainer
import com.medtrack.lookup.Brand
import com.medtrack.lookup.PrescriptionMatch
import com.medtrack.ui.common.AlertTone
import com.medtrack.ui.common.MedAlert
import com.medtrack.ui.common.PhotoCropFlow
import com.medtrack.ui.common.PhotoHeader
import com.medtrack.ui.common.PhotoSourceButtons
import com.medtrack.ui.common.ScreenScaffold
import com.medtrack.ui.common.containerViewModel
import com.medtrack.ui.common.rememberPhotoSources
import com.medtrack.ui.theme.medColors

/** Shown before every scan, as worded by the app's owner. */
const val SCAN_WARNING =
    "This feature is in early development, and may not be that accurate, it will work well for neat handwriting, " +
        "E-prescriptions and printed prescription, we plan on perfecting this feature in the updates coming soon, " +
        "thank you for bearing with us"

/**
 * Scan a prescription: a warning first, then a photo (taken or chosen) and its crop, then the medicines found,
 * each with the brands it most likely is. Choosing one opens Add medication filled in with it.
 */
@Composable
fun ScanScreen(c: AppContainer, onBack: () -> Unit, onAdd: (Brand) -> Unit) {
    val vm = containerViewModel { ScanViewModel(c) }
    val step by vm.step.collectAsStateWithLifecycle()
    val sources = rememberPhotoSources(vm::onPicked)

    ScreenScaffold(title = "Scan prescription", isTab = false, onBack = onBack) { padding ->
        when (val s = step) {
            ScanStep.Warning, ScanStep.ChoosePhoto, is ScanStep.Cropping -> ChoosePhoto(padding, sources)
            is ScanStep.Reading -> Column(
                Modifier.padding(padding).fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
                Text("Reading the prescription…", style = MaterialTheme.typography.bodyLarge)
            }
            is ScanStep.Found -> Found(padding, s, onAdd, onScanAgain = vm::scanAnother)
            is ScanStep.Failed -> Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MedAlert(AlertTone.DANGER, s.message)
                PhotoSourceButtons(sources)
            }
        }
    }

    if (step == ScanStep.Warning) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text("Before you scan") },
            text = { Text(SCAN_WARNING) },
            confirmButton = { TextButton(onClick = vm::acceptWarning) { Text("Continue") } },
            dismissButton = { TextButton(onClick = onBack) { Text("Cancel") } },
        )
    }
    (step as? ScanStep.Cropping)?.let { s -> PhotoCropFlow(listOf(s.path), c.photos, vm::onCropped) }
}

@Composable
private fun ChoosePhoto(padding: PaddingValues, sources: com.medtrack.ui.common.PhotoSources) {
    Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Photograph the prescription flat and well lit, with the medicines in view.",
            style = MaterialTheme.typography.bodyLarge,
        )
        PhotoSourceButtons(sources)
    }
}

@Composable
private fun Found(padding: PaddingValues, s: ScanStep.Found, onAdd: (Brand) -> Unit, onScanAgain: () -> Unit) {
    val colors = MaterialTheme.medColors
    LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PhotoHeader(s.path, "Scanned prescription") }
        item {
            val message = when {
                s.text.isEmpty() -> "No writing could be read in this photo. Try again closer, flat and well lit."
                s.matches.isEmpty() -> "No medicines could be matched. You can still add them yourself by name."
                else -> "Check each one against the prescription before adding it. These are suggestions, not a reading you can rely on."
            }
            MedAlert(if (s.matches.isEmpty()) AlertTone.WARNING else AlertTone.INFO, message)
        }
        items(s.matches, key = { it.read + it.brands.first().name }) { match -> MatchCard(match, onAdd) }
        if (s.text.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Text read from the photo", style = MaterialTheme.typography.labelLarge, color = colors.inkMuted, modifier = Modifier.semantics { heading() })
                    Text(s.text, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                }
            }
        }
        item {
            HorizontalDivider(color = colors.foil)
            Spacer(Modifier.size(8.dp))
            OutlinedButton(onClick = onScanAgain, modifier = Modifier.heightIn(min = 48.dp)) { Text("Scan another") }
        }
    }
}

/** One word read from the prescription, and the brands it could be; each can be added. */
@Composable
private fun MatchCard(match: PrescriptionMatch, onAdd: (Brand) -> Unit) {
    val colors = MaterialTheme.medColors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Read as “${match.read}”", style = MaterialTheme.typography.labelLarge, color = colors.inkMuted, modifier = Modifier.semantics { heading() })
        match.brands.forEach { brand ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(brand.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colors.ink)
                    if (brand.saved) Text("One of your medications", style = MaterialTheme.typography.labelMedium, color = colors.taken)
                    Text(brand.composition, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onAdd(brand) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Add") }
            }
        }
    }
}
