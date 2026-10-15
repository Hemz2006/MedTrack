package com.medtrack.ui.today

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.medtrack.AppContainer
import com.medtrack.data.DoseStatus
import com.medtrack.reminders.UndoTarget
import com.medtrack.ui.common.TIME
import com.medtrack.ui.theme.medColors
import com.medtrack.ui.theme.tabular
import java.time.Instant
import java.time.ZoneId

/** Takes or skips a dose; returns what undo needs, or null when nothing changed. */
internal suspend fun recordDose(c: AppContainer, item: TodayItem, status: DoseStatus): UndoTarget? {
    val logId = item.logId ?: return c.engine.recordUpcoming(item.medication.id, item.dueAt, status, item.slot)
    val previous = item.status ?: return null
    if (previous == status) return null
    if (status == DoseStatus.TAKEN) c.engine.markTaken(logId) else c.engine.markSkipped(logId)
    return UndoTarget(logId, previous)
}

/**
 * Unticks a taken dose. It goes back to waiting (or, if its time has passed, to due/missed as the engine
 * decides); a dose ticked before it was due becomes upcoming again and still rings.
 */
internal suspend fun untickDose(c: AppContainer, item: TodayItem) {
    item.logId?.let { c.engine.undo(it, null) }
}

/** What the tick box's label says beside an unticked dose. */
private fun TodayItem.statusText(now: Instant, zone: ZoneId): String = when {
    status == DoseStatus.TAKEN -> "Taken" + (actedAt?.let { " " + TIME.format(it.atZone(zone)) } ?: "")
    status == DoseStatus.SKIPPED -> "Skipped"
    status == DoseStatus.MISSED -> "Missed"
    isDueNow(now) -> "Due now"
    else -> "Later"
}

/**
 * A tick box for one dose: ticked = taken. [label] defaults to the dose's status ("Due now", "Taken 2:05 PM");
 * the medications list passes the dose time instead.
 */
@Composable
fun DoseCheck(
    item: TodayItem,
    now: Instant,
    zone: ZoneId,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = item.statusText(now, zone),
) {
    val colors = MaterialTheme.medColors
    val taken = item.status == DoseStatus.TAKEN
    val tone: Color = when {
        taken -> colors.taken
        item.status == DoseStatus.MISSED -> colors.missed
        item.isDueNow(now) -> colors.amber
        else -> colors.inkMuted
    }
    Row(
        modifier
            .heightIn(min = 48.dp)
            .toggleable(value = taken, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = taken,
            onCheckedChange = null, // the whole row toggles, so the label is part of the touch target
            colors = CheckboxDefaults.colors(checkedColor = colors.taken, uncheckedColor = tone, checkmarkColor = colors.paper),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium.tabular(), color = if (taken) colors.taken else if (tone == colors.inkMuted) colors.inkMuted else colors.ink)
    }
}
