package com.medtrack.ui.today

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medtrack.data.DoseStatus
import com.medtrack.data.displayName
import com.medtrack.ui.common.PhotoThumb
import com.medtrack.ui.common.TIME
import com.medtrack.ui.theme.medColors
import com.medtrack.ui.theme.tabular
import java.time.Instant
import java.time.ZoneId

/** The amber 4dp rule down the left edge of the group that is due now. */
@Composable
fun Modifier.dueRule(active: Boolean): Modifier {
    if (!active) return this
    val amber = MaterialTheme.medColors.amber
    return drawBehind { drawRect(amber, size = Size(4.dp.toPx(), size.height)) }
}

/** Header of a time group: the time (or meal hint) in title size with tabular figures. */
@Composable
fun GroupHeader(group: TodayGroup, dueNow: Boolean) {
    Text(
        group.label,
        style = MaterialTheme.typography.titleLarge.tabular(),
        color = MaterialTheme.medColors.ink,
        modifier = Modifier.fillMaxWidth().dueRule(dueNow).padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DoseRow(
    item: TodayItem,
    now: Instant,
    zone: ZoneId,
    dueNow: Boolean,
    onTake: () -> Unit,
    onMore: () -> Unit,
) {
    val hasAction = item.isDueNow(now) || item.status == DoseStatus.MISSED
    // At large font sizes there is no room beside the name, so the status/actions go underneath it.
    val stacked = LocalDensity.current.fontScale >= 1.5f
    Row(
        Modifier.fillMaxWidth()
            .dueRule(dueNow)
            .combinedClickable(
                onClick = { if (!hasAction) onMore() },
                onClickLabel = if (hasAction) null else "More actions",
                onLongClick = onMore,
                onLongClickLabel = "More actions",
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhotoThumb(item.medication.photoPath, item.medication.genericName, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.medication.displayName(), style = MaterialTheme.typography.titleMedium)
            detailLine(item, zone)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.medColors.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (stacked) {
                Spacer(Modifier.height(4.dp))
                DoseStatusAction(item, now, zone, onTake, alignEnd = false)
            }
        }
        if (!stacked) {
            Spacer(Modifier.width(12.dp))
            DoseStatusAction(item, now, zone, onTake, alignEnd = true)
        }
    }
}

/** Dose text plus a short excerpt of the user's notes, and a snooze if there is one. */
private fun detailLine(item: TodayItem, zone: ZoneId): String? {
    val med = item.medication
    val note = med.notes?.trim()?.takeIf { it.isNotEmpty() }?.lineSequence()?.first()?.let {
        if (it.length > 40) it.take(39).trimEnd() + "…" else it
    }
    val snoozed = item.snoozedUntil?.takeIf { item.status == DoseStatus.PENDING }?.let { "Snoozed to ${TIME.format(it.atZone(zone))}" }
    return listOfNotNull(med.doseText?.takeIf { it.isNotBlank() }, note, snoozed).joinToString(" · ").ifEmpty { null }
}

@Composable
private fun DoseStatusAction(item: TodayItem, now: Instant, zone: ZoneId, onTake: () -> Unit, alignEnd: Boolean) {
    val colors = MaterialTheme.medColors
    val labelStyle = MaterialTheme.typography.bodyMedium
    when {
        item.isDueNow(now) -> Button(
            onClick = onTake,
            colors = ButtonDefaults.buttonColors(containerColor = colors.amber, contentColor = colors.onAmber),
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text("Take", style = MaterialTheme.typography.titleMedium) }

        item.status == DoseStatus.MISSED -> Column(
            horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        ) {
            Text("Missed", style = labelStyle, color = colors.missed)
            OutlinedButton(onClick = onTake, modifier = Modifier.heightIn(min = 48.dp)) { Text("Take now") }
        }

        item.status == DoseStatus.TAKEN -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = colors.taken, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            val at = item.actedAt?.let { " " + TIME.format(it.atZone(zone)) }.orEmpty()
            Text("Taken$at", style = labelStyle.tabular(), color = colors.taken)
        }

        item.status == DoseStatus.SKIPPED -> Text("Skipped", style = labelStyle, color = colors.inkMuted)

        else -> Text("Later", style = labelStyle, color = colors.inkMuted)
    }
}
