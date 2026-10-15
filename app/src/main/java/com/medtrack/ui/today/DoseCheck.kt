package com.medtrack.ui.today

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
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

/** What the tick box's label says; [separator] goes between "Taken" and its time. */
private fun TodayItem.statusText(now: Instant, zone: ZoneId, separator: String = " "): String = when {
    status == DoseStatus.TAKEN -> "Taken" + (actedAt?.let { separator + TIME.format(it.atZone(zone)) } ?: "")
    status == DoseStatus.SKIPPED -> "Skipped"
    status == DoseStatus.MISSED -> "Missed"
    isDueNow(now) -> "Due now"
    else -> "Later"
}

/**
 * A tick box for one dose: ticked = taken. [label] defaults to the dose's status ("Due now", "Taken 2:05 PM");
 * the medications list passes the dose time instead. [stacked] puts a small label under the box, for narrow rows.
 */
@Composable
fun DoseCheck(
    item: TodayItem,
    now: Instant,
    zone: ZoneId,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
    label: String = item.statusText(now, zone, separator = if (stacked) "\n" else " "),
) {
    val colors = MaterialTheme.medColors
    val taken = item.status == DoseStatus.TAKEN
    val tone: Color = when {
        taken -> colors.taken
        item.status == DoseStatus.MISSED -> colors.missed
        item.isDueNow(now) -> colors.amber
        else -> colors.inkMuted
    }
    val labelColor = if (taken) colors.taken else if (tone == colors.inkMuted) colors.inkMuted else colors.ink
    val box = @Composable {
        Checkbox(
            checked = taken,
            onCheckedChange = null, // the whole control toggles, so the label is part of the touch target
            colors = CheckboxDefaults.colors(checkedColor = colors.taken, uncheckedColor = tone, checkmarkColor = colors.paper),
        )
    }
    val toggle = modifier.heightIn(min = 48.dp).toggleable(value = taken, role = Role.Checkbox, onValueChange = onCheckedChange)
    if (stacked) {
        Column(toggle.widthIn(min = 56.dp, max = 88.dp).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            box()
            Text(label, style = MaterialTheme.typography.labelMedium.tabular(), color = labelColor, textAlign = TextAlign.Center)
        }
    } else {
        Row(toggle.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            box()
            Text(label, style = MaterialTheme.typography.bodyMedium.tabular(), color = labelColor)
        }
    }
}
