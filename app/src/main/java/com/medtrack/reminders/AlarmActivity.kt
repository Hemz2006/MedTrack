package com.medtrack.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medtrack.AppContainer
import com.medtrack.container
import com.medtrack.data.displayName
import com.medtrack.ui.common.PhotoThumb
import com.medtrack.ui.theme.MedTrackTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Full-screen alarm shown over the lock screen; lists every dose ringing right now. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val c = container
        setContent { MedTrackTheme { AlarmScreen(c, onEmpty = ::finish) } }
    }
}

private val HM = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun AlarmScreen(c: AppContainer, onEmpty: () -> Unit) {
    val due by remember {
        combine(c.db.doseLogDao().observePending(), c.db.medicationDao().observeAll()) { logs, meds ->
            val now = Instant.now()
            val byId = meds.associateBy { it.id }
            logs.filter { (it.snoozedUntil ?: it.dueAt) <= now }
                .mapNotNull { log -> byId[log.medicationId]?.let { log to it } }
        }
    }.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(due) { if (due?.isEmpty() == true) onEmpty() }
    val items = due ?: return

    Scaffold { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Time for your medication", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.first.id }) { (log, med) ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PhotoThumb(med.photoPath, med.genericName, 72.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(med.displayName(), style = MaterialTheme.typography.titleMedium)
                                    med.doseText?.takeIf { it.isNotBlank() }?.let { Text(it) }
                                    Text("Due ${HM.format(log.dueAt.atZone(ZoneId.systemDefault()))}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            med.notes?.takeIf { it.isNotBlank() }?.let { Text(it, Modifier.padding(top = 8.dp)) }
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { c.appScope.launch { c.engine.markTaken(log.id) } }) { Text("Taken") }
                                OutlinedButton(onClick = { c.appScope.launch { c.engine.markSkipped(log.id) } }) { Text("Skip") }
                                TextButton(onClick = { c.appScope.launch { c.engine.snooze(log.id) } }) { Text("Snooze 10 min") }
                            }
                        }
                    }
                }
            }
            if (items.size > 1) {
                Button(
                    onClick = { c.appScope.launch { c.engine.markAllTaken(items.map { it.first.id }) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Take all") }
            }
        }
    }
}
