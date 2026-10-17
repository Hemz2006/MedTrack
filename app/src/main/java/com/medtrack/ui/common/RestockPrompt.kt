package com.medtrack.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medtrack.AppContainer
import com.medtrack.data.courseEnd
import com.medtrack.data.restockDue
import com.medtrack.data.restockKey
import com.medtrack.data.title
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val END_DAY = DateTimeFormatter.ofPattern("EEE, d MMM")

/**
 * Asks whether to buy more of a medication whose course ends in the next few days, with the Order button's choice
 * of pharmacies. Each course end is asked about once: choosing a pharmacy or "Not now" both count as answered.
 */
@Composable
fun RestockPrompt(c: AppContainer) {
    val meds by remember { c.db.medicationDao().observeAll() }.collectAsStateWithLifecycle(null)
    var asked by remember { mutableStateOf(c.prefs.restockAsked) }
    val today = remember { LocalDate.now() }
    val med = meds?.let { restockDue(it, today, asked) } ?: return
    val end = med.courseEnd ?: return
    val answer = {
        val key = restockKey(med, end)
        c.prefs.markRestockAsked(key)
        asked = asked + key
    }
    val ends = if (end == today) "ends today" else "ends on ${end.format(END_DAY)}"
    AlertDialog(
        onDismissRequest = answer,
        title = { Text("Running out soon") },
        text = { Text("${med.title} $ends. Want to buy more?") },
        confirmButton = { OrderButton(med, onOrdered = answer) },
        dismissButton = { TextButton(onClick = answer) { Text("Not now") } },
    )
}
