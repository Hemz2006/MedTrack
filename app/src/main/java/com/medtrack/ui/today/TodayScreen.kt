package com.medtrack.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medtrack.AppContainer
import com.medtrack.data.DoseStatus
import com.medtrack.data.displayName
import com.medtrack.health.HealthStatus
import com.medtrack.schedule.ScheduleRule
import com.medtrack.ui.common.PhotoThumb
import com.medtrack.ui.common.ScreenScaffold
import com.medtrack.ui.common.TIME
import com.medtrack.ui.common.containerViewModel
import com.medtrack.ui.settings.readSetupStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class TodayViewModel(c: AppContainer) : ViewModel() {
    private val ticker = flow { while (true) { emit(Unit); delay(30_000) } }
    val items = combine(
        c.db.medicationDao().observeAll(),
        c.db.doseLogDao().observeSince(Instant.now().minus(Duration.ofDays(2))),
        ticker,
    ) { meds, logs, _ -> buildToday(meds, logs, Instant.now(), ZoneId.systemDefault()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun TodayScreen(c: AppContainer, onOpenSettings: () -> Unit) {
    val vm = containerViewModel { TodayViewModel(c) }
    val items by vm.items.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    var warnings by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(resumes) {
        val status = readSetupStatus(context, c.health)
        val hasMealMeds = c.db.medicationDao().active().any { it.schedule.rule is ScheduleRule.AfterMeal }
        warnings = buildList {
            if (!status.notifications || !status.exactAlarms) add("Reminders may not ring. Tap to finish setup in Settings.")
            if (hasMealMeds && status.health != HealthStatus.CONNECTED) {
                add("Health Connect isn't connected, so meal reminders will use their fallback times. Tap to fix.")
            }
        }
    }
    var acting by remember { mutableStateOf<TodayItem?>(null) }

    ScreenScaffold(title = "Today", isTab = true) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(warnings) { warning ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = onOpenSettings),
                ) { Text(warning, Modifier.padding(12.dp)) }
            }
            val list = items
            if (list != null && list.isEmpty()) item { Text("Nothing scheduled for today.", Modifier.padding(16.dp)) }
            items(list.orEmpty(), key = { it.key }) { item ->
                ListItem(
                    leadingContent = { PhotoThumb(item.medication.photoPath, item.medication.genericName, 48.dp) },
                    headlineContent = { Text(item.medication.displayName()) },
                    supportingContent = {
                        val time = TIME.format(item.dueAt.atZone(ZoneId.systemDefault()))
                        Text(item.mealHint?.let { "$it · by $time" } ?: time)
                    },
                    trailingContent = { Text(statusLabel(item), color = statusColor(item.status)) },
                    modifier = Modifier.clickable { acting = item },
                )
            }
        }
    }

    acting?.let { item ->
        AlertDialog(
            onDismissRequest = { acting = null },
            title = { Text(item.medication.displayName()) },
            text = { Text("Due ${TIME.format(item.dueAt.atZone(ZoneId.systemDefault()))}" + (item.medication.notes?.let { "\n$it" } ?: "")) },
            confirmButton = { TextButton(onClick = { act(c, item, DoseStatus.TAKEN); acting = null }) { Text("Taken") } },
            dismissButton = { TextButton(onClick = { act(c, item, DoseStatus.SKIPPED); acting = null }) { Text("Skip") } },
        )
    }
}

private fun act(c: AppContainer, item: TodayItem, status: DoseStatus) {
    c.appScope.launch {
        val logId = item.logId
        when {
            logId == null -> c.engine.recordUpcoming(item.medication.id, item.dueAt, status)
            status == DoseStatus.TAKEN -> c.engine.markTaken(logId)
            else -> c.engine.markSkipped(logId)
        }
    }
}

private fun statusLabel(item: TodayItem): String = when (item.status) {
    null -> "Upcoming"
    DoseStatus.PENDING -> item.snoozedUntil?.let { "Snoozed to ${TIME.format(it.atZone(ZoneId.systemDefault()))}" } ?: "Due now"
    DoseStatus.TAKEN -> "Taken"
    DoseStatus.SKIPPED -> "Skipped"
    DoseStatus.MISSED -> "Missed"
}

@Composable
private fun statusColor(status: DoseStatus?) = when (status) {
    DoseStatus.TAKEN -> MaterialTheme.colorScheme.primary
    DoseStatus.MISSED, DoseStatus.PENDING -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.outline
}
