package com.medtrack.ui.meds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medtrack.AppContainer
import com.medtrack.data.displayName
import com.medtrack.schedule.describe
import com.medtrack.ui.common.PhotoThumb
import com.medtrack.ui.common.ScreenScaffold

@Composable
fun MedicationsScreen(c: AppContainer, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val meds by remember { c.db.medicationDao().observeAll() }.collectAsStateWithLifecycle(emptyList())
    ScreenScaffold(
        title = "Medications",
        isTab = true,
        fab = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add medication") } },
    ) { padding ->
        if (meds.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No medications yet. Tap + to add one.")
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(meds, key = { it.id }) { med ->
                    ListItem(
                        headlineContent = { Text(med.displayName()) },
                        supportingContent = { Text(med.schedule.describe()) },
                        leadingContent = { PhotoThumb(med.photoPath, med.genericName, 48.dp) },
                        trailingContent = {
                            if (!med.active) Text("Paused", color = MaterialTheme.colorScheme.outline)
                        },
                        modifier = Modifier.clickable { onOpen(med.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
