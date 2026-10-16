package com.medtrack.ui.today

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medtrack.ui.common.DatePickDialog
import com.medtrack.ui.theme.medColors
import com.medtrack.ui.theme.tabular
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** How Today lists the day's doses. */
enum class TodayView(val key: String, val label: String) {
    TIME("time", "By time"),
    MEDICATION("medication", "By medication");

    companion object {
        fun from(key: String): TodayView = entries.firstOrNull { it.key == key } ?: TIME
    }
}

private val DAY_HEADING: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

/** "Today", "Yesterday" or "Mon 12 Oct". */
fun dayName(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    today.minusDays(1) -> "Yesterday"
    else -> SHORT_DATE.format(date)
}

/**
 * The day's controls under the ring: step back and forward through past days (history), pick any date,
 * and choose how the doses are listed.
 */
@Composable
internal fun DayControls(
    date: LocalDate,
    today: LocalDate,
    view: TodayView,
    onDate: (LocalDate) -> Unit,
    onView: (TodayView) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onDate(date.minusDays(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous day")
        }
        TextButton(onClick = { picking = true }, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(dayName(date, today), maxLines = 1)
        }
        IconButton(onClick = { onDate(date.plusDays(1)) }, enabled = date < today.plusYears(1)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next day")
        }
        Spacer(Modifier.weight(1f))
        GooeyMenu(TodayView.entries, view, label = { it.label }, onSelect = onView)
    }
    if (picking) {
        // Past days show what was recorded; future days (up to a year ahead) show what's planned.
        DatePickDialog(date, onDismiss = { picking = false }, maxDate = today.plusYears(1)) { picked -> picking = false; onDate(picked) }
    }
}

/**
 * The date inside a ring that fills as today's doses are taken: amber while some are left, green when all are.
 * Beside it, the full date and "3 of 5 doses taken".
 */
@Composable
internal fun DayRing(date: LocalDate, progress: DayProgress, hasDoses: Boolean, future: Boolean = false) {
    val colors = MaterialTheme.medColors
    val done = hasDoses && progress.taken == progress.total
    val fill by animateFloatAsState(progress.fraction, animationSpec = tween(400), label = "day ring")
    val status = when {
        !hasDoses -> null
        future -> if (progress.total == 1) "1 dose planned" else "${progress.total} doses planned"
        done -> "All ${progress.total} doses taken"
        else -> "${progress.taken} of ${progress.total} doses taken"
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(80.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { if (hasDoses) fill else 0f },
                modifier = Modifier.fillMaxSize(),
                color = if (done) colors.taken else colors.amber,
                trackColor = colors.foil,
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(WEEKDAY.format(date), style = MaterialTheme.typography.labelSmall, color = colors.inkMuted)
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.headlineMedium.tabular(), color = colors.ink)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                DAY_HEADING.format(date),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            status?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = if (done) colors.taken else colors.inkMuted) }
        }
    }
}

private val WEEKDAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
