package com.medtrack.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.medtrack.ui.theme.medColors
import com.medtrack.ui.theme.tabular
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_TITLE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val SPOKEN_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH)
private val CELL: Dp = 42.dp
private val WEEKDAYS = listOf("S", "M", "T", "W", "T", "F", "S")

/** Day of the week as a grid column, Sunday first. */
private fun LocalDate.column(): Int = dayOfWeek.value % 7

/**
 * The app's calendar: a month grid where a pill glides to the day you tap, and months slide in the direction
 * you move. Dates outside [minDate]..[maxDate] are greyed out. Choose confirms; Cancel changes nothing.
 */
@Composable
fun DatePickDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    onPick: (LocalDate) -> Unit,
) {
    var selected by remember { mutableStateOf(initial) }
    var month by remember { mutableStateOf(YearMonth.from(initial)) }
    val today = remember { LocalDate.now() }
    val allowed = { date: LocalDate -> (minDate == null || date >= minDate) && (maxDate == null || date <= maxDate) }
    val colors = MaterialTheme.medColors

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = colors.paperRaised) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(MONTH_TITLE.format(month), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { month = month.minusMonths(1) },
                        enabled = minDate == null || month.atDay(1) > minDate,
                    ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
                    IconButton(
                        onClick = { month = month.plusMonths(1) },
                        enabled = maxDate == null || month.atEndOfMonth() < maxDate,
                    ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
                }
                Row {
                    WEEKDAYS.forEach {
                        Text(it, Modifier.size(CELL, 24.dp), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
                    }
                }
                AnimatedContent(
                    targetState = month,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally(tween(260)) { if (forward) it else -it } + fadeIn(tween(200)))
                            .togetherWith(slideOutHorizontally(tween(260)) { if (forward) -it else it } + fadeOut(tween(160)))
                    },
                    label = "month",
                ) { shown ->
                    MonthGrid(shown, selected, today, allowed) { selected = it }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onPick(selected) }, enabled = allowed(selected)) { Text("Choose") }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, selected: LocalDate, today: LocalDate, allowed: (LocalDate) -> Boolean, onSelect: (LocalDate) -> Unit) {
    val colors = MaterialTheme.medColors
    val leading = month.atDay(1).column()
    val rows = (leading + month.lengthOfMonth() + 6) / 7
    Box(Modifier.size(CELL * 7, CELL * rows)) {
        // The selection pill glides from day to day within the month.
        if (YearMonth.from(selected) == month) {
            val index = leading + selected.dayOfMonth - 1
            val motion = spring<Dp>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)
            val x by animateDpAsState(CELL * (index % 7), motion, label = "pill x")
            val y by animateDpAsState(CELL * (index / 7), motion, label = "pill y")
            Box(Modifier.offset(x, y).size(CELL).padding(3.dp).background(colors.ink, CircleShape))
        }
        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            val index = leading + day - 1
            val isSelected = date == selected
            val enabled = allowed(date)
            Box(
                Modifier
                    .offset(CELL * (index % 7), CELL * (index / 7))
                    .size(CELL)
                    .padding(3.dp)
                    .then(if (date == today && !isSelected) Modifier.border(1.5.dp, colors.amber, CircleShape) else Modifier)
                    .clickable(enabled = enabled, role = Role.Button) { onSelect(date) }
                    .semantics {
                        contentDescription = SPOKEN_DATE.format(date) + if (date == today) ", today" else ""
                        this.selected = isSelected
                    }
                    .alpha(if (enabled) 1f else 0.35f),
                contentAlignment = Alignment.Center,
            ) {
                Text(day.toString(), style = MaterialTheme.typography.bodyLarge.tabular(), color = if (isSelected) colors.paper else colors.ink)
            }
        }
    }
}
