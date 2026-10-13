package com.medtrack.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medtrack.data.displayName
import com.medtrack.ui.common.Capsule
import com.medtrack.ui.common.TIME
import java.time.Instant
import java.time.ZoneId

/** Today's doses as a blister pack, one capsule per dose in time order. It is the day's progress indicator. */
@Composable
fun BlisterStrip(items: List<TodayItem>, now: Instant, zone: ZoneId, onTap: (TodayItem) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp)) {
        items(items, key = { it.stableKey }) { item ->
            val state = item.capsuleState(now)
            val description = "${item.medication.displayName()} ${TIME.format(item.dueAt.atZone(zone))}, ${state.spoken}"
            Box(
                Modifier.size(48.dp, 56.dp)
                    .semantics { contentDescription = description }
                    .clickable(onClickLabel = "Show this dose") { onTap(item) },
                contentAlignment = Alignment.Center,
            ) { Capsule(state, popWhenTaken = true) }
        }
    }
}

/** Survives the switch from an upcoming dose to its log, so the row and capsule keep their identity. */
val TodayItem.stableKey: String get() = "dose-${medication.id}-${dueAt.toEpochMilli()}"
