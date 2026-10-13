package com.medtrack.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medtrack.AppContainer
import com.medtrack.data.DoseStatus
import com.medtrack.data.DoseTrigger
import com.medtrack.data.MedicationEntity
import com.medtrack.data.displayName
import com.medtrack.lookup.applyLookup
import com.medtrack.schedule.describe
import com.medtrack.ui.common.DAY_TIME
import com.medtrack.ui.common.PhotoThumb
import com.medtrack.ui.common.ScreenScaffold
import kotlinx.coroutines.launch
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(c: AppContainer, id: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val med by remember(id) { c.db.medicationDao().observe(id) }.collectAsStateWithLifecycle(null)
    val logs by remember(id) { c.db.doseLogDao().observeForMedication(id) }.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val m = med ?: return

    ScreenScaffold(
        title = m.displayName(),
        isTab = false,
        onBack = onBack,
        actions = { IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") } },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { PhotoThumb(m.photoPath, m.genericName, 160.dp) }
            item { Info(m) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { c.appScope.launch { c.medications.setActive(id, !m.active) } }) {
                        Text(if (m.active) "Pause" else "Resume")
                    }
                    OutlinedButton(
                        enabled = !refreshing,
                        onClick = {
                            scope.launch {
                                refreshing = true
                                try {
                                    val result = c.lookup.lookup(m.genericName, m.rxcui)
                                    c.db.medicationDao().get(id)?.let { c.medications.save(it.applyLookup(result, onlyMissing = false)) }
                                } finally {
                                    refreshing = false
                                }
                            }
                        },
                    ) { Text(if (refreshing) "Refreshing…" else "Refresh info") }
                    OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete") }
                }
            }
            item { Text("History", style = MaterialTheme.typography.titleMedium) }
            if (logs.isEmpty()) item { Text("No doses recorded yet.") }
            items(logs, key = { it.id }) { log ->
                ListItem(
                    headlineContent = { Text(DAY_TIME.format(log.dueAt.atZone(ZoneId.systemDefault()))) },
                    supportingContent = {
                        Text(when (log.trigger) { DoseTrigger.SCHEDULE -> "Scheduled"; DoseTrigger.MEAL -> "After a meal"; DoseTrigger.FALLBACK -> "Meal fallback" })
                    },
                    trailingContent = {
                        Text(when (log.status) { DoseStatus.PENDING -> "Due"; DoseStatus.TAKEN -> "Taken"; DoseStatus.SKIPPED -> "Skipped"; DoseStatus.MISSED -> "Missed" })
                    },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${m.displayName()}?") },
            text = { Text("This also deletes its photo and dose history. To keep the history, pause it instead.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    c.appScope.launch { c.medications.delete(id) }
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Info(m: MedicationEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(m.genericName, style = MaterialTheme.typography.titleLarge)
        if (!m.active) Text("Paused", color = MaterialTheme.colorScheme.error)
        m.brandName?.let { Text("Brand: $it") }
        m.doseText?.let { Text("Dose: $it") }
        if (m.ingredients.isNotEmpty()) {
            Text("Active ingredients", style = MaterialTheme.typography.titleSmall)
            m.ingredients.forEach { i ->
                Text("• " + listOfNotNull(i.name, i.strength, i.formula?.let { "($it)" }).joinToString(" "))
            }
        }
        m.usedFor?.let {
            Text("Used for", style = MaterialTheme.typography.titleSmall)
            Text(it)
            Text("General information, not medical advice.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        m.notes?.let {
            Text("My notes", style = MaterialTheme.typography.titleSmall)
            Text(it)
        }
        Text("Schedule", style = MaterialTheme.typography.titleSmall)
        Text(m.schedule.describe())
    }
}
