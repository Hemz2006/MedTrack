package com.medtrack.ui.today

import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.LinearProgressIndicator
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.medtrack.reminders.UndoTarget
import com.medtrack.schedule.hasMealRule
import com.medtrack.ui.common.containerViewModel
import com.medtrack.ui.settings.readSetupStatus
import com.medtrack.ui.theme.LabelShape
import com.medtrack.ui.theme.medColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TodayState(val items: List<TodayItem>, val hasMedications: Boolean, val now: Instant, val zone: ZoneId) {
    val date: LocalDate get() = now.atZone(zone).toLocalDate()
}

class TodayViewModel(c: AppContainer) : ViewModel() {
    private val ticker = flow { while (true) { emit(Unit); delay(30_000) } }
    val state = combine(
        c.db.medicationDao().observeAll(),
        c.db.doseLogDao().observeSince(Instant.now().minus(Duration.ofDays(2))),
        ticker,
    ) { meds, logs, _ ->
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        TodayState(buildToday(meds, logs, now, zone), meds.isNotEmpty(), now, zone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

private val DAY_HEADING: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)

@Composable
fun TodayScreen(
    c: AppContainer,
    onOpenSettings: () -> Unit,
    onAddMedication: () -> Unit,
    onOpenDetails: (Long) -> Unit,
) {
    val vm = containerViewModel { TodayViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    var warnings by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(resumes) {
        val status = readSetupStatus(context, c.health)
        val hasMealMeds = c.db.medicationDao().active().any { it.steps.hasMealRule }
        warnings = buildList {
            if (!status.notifications || !status.exactAlarms) add("Reminders may not ring. Tap to finish setup in Settings.")
            if (hasMealMeds && status.health != HealthStatus.CONNECTED) {
                add("Health Connect isn't connected, so meal reminders will use their fallback times. Tap to fix.")
            }
        }
    }
    var sheetItem by remember { mutableStateOf<TodayItem?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    /** Records the choice at once, then offers Undo. */
    fun record(item: TodayItem, status: DoseStatus) {
        scope.launch {
            // The write runs in the app scope so leaving the screen can't cancel it half way.
            val target = try {
                c.appScope.async { recordDose(c, item, status) }.await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MedTrack", "Recording a dose failed", e)
                null
            } ?: return@launch
            snackbar.currentSnackbarData?.dismiss()
            val verb = if (status == DoseStatus.TAKEN) "taken" else "skipped"
            val result = snackbar.showSnackbar("${item.medication.displayName()} $verb", actionLabel = "Undo", duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) c.appScope.launch { c.engine.undo(target.logId, target.previous) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets.statusBars,
    ) { padding ->
        val s = state ?: return@Scaffold
        val groups = remember(s) { groupToday(s.items, s.zone) }
        // LazyColumn index of each dose: its group header if it opens the group, so the time shows too.
        val scrollIndex = remember(groups, warnings) {
            buildMap {
                var index = warnings.size
                groups.forEach { group ->
                    val header = index++
                    group.items.forEachIndexed { i, item -> put(item.stableKey, if (i == 0) header else index); index++ }
                }
            }
        }
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (s.items.isNotEmpty()) DayProgressBar(s.items.progress())
            Text(
                DAY_HEADING.format(s.date),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp).semantics { heading() },
            )
            if (s.items.isNotEmpty()) {
                BlisterStrip(s.items, s.now, s.zone, onTap = { item ->
                    scrollIndex[item.stableKey]?.let { scope.launch { listState.animateScrollToItem(it) } }
                })
            }
            HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.medColors.foil)
            LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                items(warnings) { warning -> SetupWarning(warning, onOpenSettings) }
                if (s.items.isEmpty()) {
                    item(key = "empty") { EmptyToday(s.hasMedications, onAddMedication) }
                }
                groups.forEach { group ->
                    val dueNow = group.isDueNow(s.now)
                    item(key = group.key) { GroupHeader(group, dueNow) }
                    items(group.items, key = { it.stableKey }) { item ->
                        DoseRow(
                            item, s.now, s.zone, dueNow,
                            onTick = { ticked -> if (ticked) record(item, DoseStatus.TAKEN) else c.appScope.launch { untickDose(c, item) } },
                            onMore = { sheetItem = item },
                        )
                    }
                }
            }
        }
    }

    sheetItem?.let { item ->
        DoseSheet(
            item,
            state?.zone ?: ZoneId.systemDefault(),
            onDismiss = { sheetItem = null },
            onTake = { sheetItem = null; record(item, DoseStatus.TAKEN) },
            onSkip = { sheetItem = null; record(item, DoseStatus.SKIPPED) },
            onOpenDetails = { sheetItem = null; onOpenDetails(item.medication.id) },
        )
    }
}

@Composable
private fun SetupWarning(text: String, onClick: () -> Unit) {
    Surface(
        shape = LabelShape,
        color = MaterialTheme.medColors.paperRaised,
        border = BorderStroke(2.dp, MaterialTheme.medColors.amber),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(role = Role.Button, onClickLabel = "Open Settings", onClick = onClick),
    ) {
        Row(Modifier.heightIn(min = 48.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.medColors.ink)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun EmptyToday(hasMedications: Boolean, onAddMedication: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (hasMedications) {
            Text("No doses scheduled today.", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text("Add your first medication to see today's doses here.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onAddMedication, modifier = Modifier.heightIn(min = 48.dp)) { Text("Add medication") }
        }
    }
}

/** "3 of 5 taken" with a bar that fills as doses are ticked; green when everything is taken. */
@Composable
private fun DayProgressBar(progress: DayProgress) {
    val colors = MaterialTheme.medColors
    val done = progress.total > 0 && progress.taken == progress.total
    val label = if (done) "All ${progress.total} doses taken" else "${progress.taken} of ${progress.total} doses taken"
    Column(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp).clearAndSetSemantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (done) colors.taken else colors.amber,
            trackColor = colors.foil,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (done) colors.taken else colors.inkMuted)
    }
}
