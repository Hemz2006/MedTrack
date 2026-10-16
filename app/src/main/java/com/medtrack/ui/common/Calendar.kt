package com.medtrack.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
private val CELL_SHAPE = RoundedCornerShape(9.dp)
private val WEEKDAYS = listOf("S", "M", "T", "W", "T", "F", "S")

/** EasyUI's calendar spring (stiffness 460, damping 38, mass 0.7) in Compose terms: stiffness k/m, ratio c/(2√(km)). */
private fun <T> glide(): FiniteAnimationSpec<T> = spring(dampingRatio = 1.06f, stiffness = 657f)

/** Day of the week as a grid column, Sunday first. */
private fun LocalDate.column(): Int = dayOfWeek.value % 7

/**
 * The app's calendar, after EasyUI's Calendar: the month name between two arrows, a fixed six-row grid of
 * rounded-square days, a dark pill that glides to the day you tap, a small dot under today, and months that
 * slide 24dp in the direction you move. Dates outside [minDate]..[maxDate] are faded and can't be picked.
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
    var direction by remember { mutableIntStateOf(1) }
    val today = remember { LocalDate.now() }
    val allowed = { date: LocalDate -> (minDate == null || date >= minDate) && (maxDate == null || date <= maxDate) }
    val canPrev = minDate == null || month.atDay(1) > minDate
    val canNext = maxDate == null || month.atEndOfMonth() < maxDate
    val slidePx = with(LocalDensity.current) { 24.dp.roundToPx() }
    val colors = MaterialTheme.medColors
    fun go(step: Long) {
        direction = if (step > 0) 1 else -1
        month = month.plusMonths(step)
    }
    val slide: () -> ContentTransform = {
        (slideInHorizontally(glide<IntOffset>()) { direction * slidePx } + fadeIn(glide()))
            .togetherWith(slideOutHorizontally(glide<IntOffset>()) { -direction * slidePx } + fadeOut(tween(120)))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = colors.paperRaised) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.size(CELL * 7, 44.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { go(-1) }, enabled = canPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
                    AnimatedContent(month, Modifier.weight(1f), transitionSpec = { slide() }, label = "month title") { shown ->
                        Text(MONTH_TITLE.format(shown), Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    }
                    IconButton(onClick = { go(1) }, enabled = canNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
                }
                Row {
                    WEEKDAYS.forEach {
                        Text(it, Modifier.size(CELL, 28.dp).padding(top = 6.dp), style = MaterialTheme.typography.labelMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
                    }
                }
                AnimatedContent(month, transitionSpec = { slide() }, label = "month grid") { shown ->
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
    // Always six rows, so the dialog doesn't change height from month to month.
    Box(Modifier.size(CELL * 7, CELL * 6)) {
        if (YearMonth.from(selected) == month) {
            val index = leading + selected.dayOfMonth - 1
            val x by animateDpAsState(CELL * (index % 7), glide(), label = "pill x")
            val y by animateDpAsState(CELL * (index / 7), glide(), label = "pill y")
            Box(Modifier.offset { IntOffset(x.roundToPx(), y.roundToPx()) }.size(CELL).padding(2.dp).background(colors.ink, CELL_SHAPE))
        }
        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            val index = leading + day - 1
            DayCell(date, picked = date == selected, isToday = date == today, enabled = allowed(date), Modifier.offset(CELL * (index % 7), CELL * (index / 7))) {
                onSelect(date)
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, picked: Boolean, isToday: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.medColors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.9f else 1f, tween(150), label = "press")
    Box(
        modifier
            .size(CELL)
            .padding(2.dp)
            .clip(CELL_SHAPE)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = SPOKEN_DATE.format(date) + if (isToday) ", today" else ""
                selected = picked
            }
            .alpha(if (enabled) 1f else 0.3f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            Modifier.scale(press),
            style = MaterialTheme.typography.bodyMedium.tabular(),
            color = if (picked) colors.paper else colors.ink,
        )
        if (isToday && !picked) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(4.dp).background(colors.inkMuted, CircleShape))
        }
    }
}
