package com.medtrack.reminders

import com.medtrack.data.doseLine
import com.medtrack.data.title
import com.medtrack.data.coverPhoto
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medtrack.AppContainer
import com.medtrack.container
import com.medtrack.data.DoseLogEntity
import com.medtrack.data.MedicationEntity
import com.medtrack.data.displayName
import com.medtrack.ui.common.PhotoHeader
import com.medtrack.ui.common.PhotoThumb
import com.medtrack.ui.common.TIME
import com.medtrack.ui.theme.LightMedColors
import com.medtrack.ui.theme.MedTrackTheme
import com.medtrack.ui.theme.medColors
import com.medtrack.ui.theme.tabular
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Full-screen alarm shown over the lock screen; lists every dose ringing right now. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark text on amber in both modes, so the system bar icons are dark too.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val c = container
        setContent { MedTrackTheme { AlarmScreen(c, onEmpty = ::finish) } }
    }
}

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
    val onAmber = MaterialTheme.medColors.onAmber

    Surface(color = MaterialTheme.medColors.amber, contentColor = onAmber, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 16.dp)) {
            Text(
                "Time for your medication",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp).semantics { heading() },
            )
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                itemsIndexed(items, key = { _, it -> it.first.id }) { index, (log, med) ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 24.dp), thickness = 2.dp, color = onAmber.copy(alpha = 0.3f))
                    AlarmDose(c, log, med, single = items.size == 1)
                }
            }
            if (items.size > 1) {
                AlarmPrimaryButton("Take all", Modifier.padding(bottom = 16.dp)) {
                    c.appScope.launch { c.engine.markAllTaken(items.map { it.first.id }) }
                }
            }
        }
    }
}

@Composable
private fun AlarmDose(c: AppContainer, log: DoseLogEntity, med: MedicationEntity, single: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (single) PhotoHeader(med.coverPhoto, med.title) else PhotoThumb(med.coverPhoto, med.title, 120.dp)
        Text(med.displayName(), style = MaterialTheme.typography.displaySmall)
        med.doseLine?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
        Text("Due ${TIME.format(log.dueAt.atZone(ZoneId.systemDefault()))}", style = MaterialTheme.typography.titleMedium.tabular())
        med.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        AlarmPrimaryButton("Taken") { c.appScope.launch { c.engine.markTaken(log.id) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AlarmSecondaryButton("Snooze 10 min", Modifier.weight(1f)) { c.appScope.launch { c.engine.snooze(log.id) } }
            AlarmSecondaryButton("Skip", Modifier.weight(1f)) { c.appScope.launch { c.engine.markSkipped(log.id) } }
        }
    }
}

/** Ink button with paper text: the dark-on-amber pairing holds in both light and dark mode. */
@Composable
private fun AlarmPrimaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.medColors.onAmber, contentColor = LightMedColors.paper),
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) { Text(label, style = MaterialTheme.typography.titleLarge) }
}

@Composable
private fun AlarmSecondaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val onAmber = MaterialTheme.medColors.onAmber
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(2.dp, onAmber),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = onAmber, containerColor = Color.Transparent),
        modifier = modifier.heightIn(min = 56.dp),
    ) { Text(label, style = MaterialTheme.typography.titleMedium) }
}
