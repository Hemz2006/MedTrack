package com.medtrack.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.medtrack.schedule.MealType
import com.medtrack.schedule.Schedule
import com.medtrack.schedule.ScheduleKind
import com.medtrack.schedule.ScheduleRule
import com.medtrack.schedule.kind
import com.medtrack.schedule.mealName
import com.medtrack.schedule.withKind
import com.medtrack.schedule.withStartDate
import com.medtrack.ui.common.DATE
import com.medtrack.ui.common.DatePickDialog
import com.medtrack.ui.common.TIME
import com.medtrack.ui.common.TimePickDialog
import com.medtrack.ui.theme.tabular
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleEditor(schedule: Schedule, onChange: (Schedule) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScheduleKind.entries.forEach { kind ->
                FilterChip(
                    selected = schedule.rule.kind == kind,
                    onClick = { onChange(schedule.withKind(kind)) },
                    label = { Text(kind.label) },
                )
            }
        }
        when (val r = schedule.rule) {
            is ScheduleRule.DailyTimes -> TimesEditor(r.times) { onChange(schedule.copy(rule = r.copy(times = it))) }
            is ScheduleRule.Weekdays -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(
                            selected = day in r.days,
                            onClick = { onChange(schedule.copy(rule = r.copy(days = if (day in r.days) r.days - day else r.days + day))) },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)) },
                        )
                    }
                }
                TimesEditor(r.times) { onChange(schedule.copy(rule = r.copy(times = it))) }
            }
            is ScheduleRule.EveryNHours -> {
                NumberField("Every how many hours", r.intervalHours) { onChange(schedule.copy(rule = r.copy(intervalHours = it))) }
                TimeRow("First dose", r.anchor.toLocalTime()) {
                    onChange(schedule.copy(rule = r.copy(anchor = schedule.startDate.atTime(it))))
                }
            }
            is ScheduleRule.AfterMeal -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.entries.forEach { meal ->
                        FilterChip(
                            selected = r.meal == meal,
                            onClick = { onChange(schedule.copy(rule = r.copy(meal = meal))) },
                            label = { Text(mealName(meal).replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                NumberField("Minutes after the meal", r.delayMinutes) { onChange(schedule.copy(rule = r.copy(delayMinutes = it))) }
                TimeRow("Remind anyway at", r.fallbackTime) { onChange(schedule.copy(rule = r.copy(fallbackTime = it))) }
                Text(
                    "Reminds you after a meal logged in Health Connect, or at this time if no meal is logged.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        DateRow("Start date", schedule.startDate, clearable = false) { it?.let { d -> onChange(schedule.withStartDate(d)) } }
        DateRow("End date (optional)", schedule.endDate, clearable = true) { onChange(schedule.copy(endDate = it)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimesEditor(times: List<LocalTime>, onChange: (List<LocalTime>) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        times.sorted().forEach { time ->
            InputChip(
                selected = false,
                onClick = { onChange(times - time) },
                label = { Text(time.format(TIME)) },
                trailingIcon = { Icon(Icons.Default.Close, "Remove ${time.format(TIME)}", Modifier.size(16.dp)) },
            )
        }
        AssistChip(
            onClick = { picking = true },
            label = { Text("Add time") },
            leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
        )
    }
    if (picking) {
        TimePickDialog(LocalTime.of(8, 0), onDismiss = { picking = false }) { time ->
            picking = false
            if (time !in times) onChange(times + time)
        }
    }
}

@Composable
private fun NumberField(label: String, value: Int, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input.filter(Char::isDigit).take(3)
            text.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = fieldColors(),
        singleLine = true,
    )
}

@Composable
private fun TimeRow(label: String, time: LocalTime, onChange: (LocalTime) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ")
        TextButton(onClick = { picking = true }) { Text(time.format(TIME), style = MaterialTheme.typography.titleMedium.tabular()) }
    }
    if (picking) TimePickDialog(time, onDismiss = { picking = false }) { picking = false; onChange(it) }
}

@Composable
private fun DateRow(label: String, date: LocalDate?, clearable: Boolean, onChange: (LocalDate?) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ")
        TextButton(onClick = { picking = true }) { Text(date?.format(DATE) ?: "None") }
        if (clearable && date != null) TextButton(onClick = { onChange(null) }) { Text("Clear") }
    }
    if (picking) DatePickDialog(date ?: LocalDate.now(), onDismiss = { picking = false }) { picking = false; onChange(it) }
}
