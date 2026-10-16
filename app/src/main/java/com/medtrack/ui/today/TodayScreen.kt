package com.medtrack.ui.today

import com.medtrack.ui.common.islandBottom
import com.medtrack.ui.common.MedAlert
import com.medtrack.ui.common.AlertTone
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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

/** The doses of [date]: today, or a past day being looked back on. */
data class TodayState(val items: List<TodayItem>, val hasMedications: Boolean, val now: Instant, val zone: ZoneId, val date: LocalDate) {
    val today: LocalDate get() = now.atZone(zone).toLocalDate()
    val isToday: Boolean get() = date == today
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val c: AppContainer) : ViewModel() {
    private val ticker = flow { while (true) { emit(Unit); delay(30_000) } }

    /** A past day being looked at, or null for today (which then follows the clock past midnight). */
    private val chosen = MutableStateFlow<LocalDate?>(null)
    val view = MutableStateFlow(TodayView.from(c.prefs.todayView))

    fun showDate(date: LocalDate) {
        chosen.value = date.takeIf { it < LocalDate.now() }
    }

    fun setView(view: TodayView) {
        this.view.value = view
        c.prefs.todayView = view.key
    }

    val state = chosen.flatMapLatest { picked ->
        // A dose belongs to its day by due time, but a late after-dinner dose can fall just past midnight, so read a day either side.
        val logs = if (picked == null) {
            c.db.doseLogDao().observeSince(Instant.now().minus(Duration.ofDays(2)))
        } else {
            val zone = ZoneId.systemDefault()
            c.db.doseLogDao().observeBetween(picked.minusDays(1).atStartOfDay(zone).toInstant(), picked.plusDays(2).atStartOfDay(zone).toInstant())
        }
        combine(c.db.medicationDao().observeAll(), logs, ticker) { meds, logList, _ ->
            val now = Instant.now()
            val zone = ZoneId.systemDefault()
            val date = picked ?: now.atZone(zone).toLocalDate()
            TodayState(buildDay(meds, logList, date, now, zone), meds.isNotEmpty(), now, zone, date)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}


@Composable
fun TodayScreen(
    c: AppContainer,
    onOpenSettings: () -> Unit,
    onAddMedication: () -> Unit,
    onOpenDetails: (Long) -> Unit,
) {
    val vm = containerViewModel { TodayViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val view by vm.view.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    var warnings by remember { mutableStateOf(emptyList<SetupWarning>()) }
    LaunchedEffect(resumes) {
        val status = readSetupStatus(context, c.health)
        val hasMealMeds = c.db.medicationDao().active().any { it.steps.hasMealRule }
        warnings = buildList {
            if (!status.notifications || !status.exactAlarms) {
                add(SetupWarning("Reminders may not ring", "Finish setup in Settings so alarms can go off.", closable = false))
            }
            if (hasMealMeds && status.health != HealthStatus.CONNECTED && !c.prefs.healthWarningHidden) {
                add(SetupWarning("Meal reminders use their fallback times", "Health Connect isn't connected, so logged meals can't trigger them.", closable = true))
            }
        }
    }
    var sheetItem by remember { mutableStateOf<TodayItem?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
        snackbarHost = {
            SnackbarHost(snackbar, Modifier.padding(bottom = islandBottom())) { data ->
                MedAlert(
                    if (data.visuals.message.endsWith("taken")) AlertTone.SUCCESS else AlertTone.NEUTRAL, data.visuals.message, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    actionLabel = data.visuals.actionLabel, onAction = { data.performAction() }, onDismiss = { data.dismiss() },
                )
            }
        },
        contentWindowInsets = WindowInsets.statusBars,
    ) { padding ->
        val s = state ?: return@Scaffold
        val onTick: (TodayItem, Boolean) -> Unit = { item, ticked ->
            if (ticked) record(item, DoseStatus.TAKEN) else c.appScope.launch { untickDose(c, item) }
        }
        Column(Modifier.padding(padding).fillMaxSize()) {
            DayRing(s.date, s.items.progress(), hasDoses = s.items.isNotEmpty())
            DayControls(s.date, s.today, view, onDate = vm::showDate, onView = vm::setView)
            HorizontalDivider(color = MaterialTheme.medColors.foil)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = islandBottom() + 16.dp)) {
                if (s.isToday) {
                    items(warnings, key = { it.title }) { warning ->
                        MedAlert(
                            AlertTone.WARNING, warning.title, Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            description = warning.detail, actionLabel = "Fix", onAction = onOpenSettings,
                            // Closing the Health Connect alert hides it for good; it is optional, unlike working alarms.
                            onDismiss = if (warning.closable) ({ c.prefs.healthWarningHidden = true; warnings = warnings - warning }) else null,
                        )
                    }
                }
                if (s.items.isEmpty()) {
                    item(key = "empty") { EmptyDay(s.hasMedications, s.isToday, onAddMedication) }
                }
                when (view) {
                    TodayView.TIME -> groupToday(s.items, s.zone).forEach { group ->
                        val dueNow = group.isDueNow(s.now)
                        item(key = group.key) { GroupHeader(group, dueNow) }
                        items(group.items, key = { it.stableKey }) { item ->
                            DoseRow(item, s.now, s.zone, dueNow, onTick = { onTick(item, it) }, onMore = { sheetItem = item })
                        }
                    }
                    TodayView.MEDICATION -> {
                        val days = s.items.byMedication()
                        days.forEachIndexed { index, day ->
                            item(key = "med-${day.medication.id}") {
                                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.medColors.foil)
                                MedicationDayCard(day, s.now, s.zone, onTick = onTick, onMore = { sheetItem = it })
                            }
                        }
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
private fun EmptyDay(hasMedications: Boolean, isToday: Boolean, onAddMedication: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!isToday) {
            Text("No doses were recorded on this day.", style = MaterialTheme.typography.bodyLarge)
        } else if (hasMedications) {
            Text("No doses scheduled today.", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text("Add your first medication to see today's doses here.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onAddMedication, modifier = Modifier.heightIn(min = 48.dp)) { Text("Add medication") }
        }
    }
}

/** A setup problem shown at the top of Today. [closable] ones can be hidden for good. */
private data class SetupWarning(val title: String, val detail: String, val closable: Boolean)
