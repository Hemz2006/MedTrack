package com.medtrack.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medtrack.ui.theme.medColors

/** How serious an alert is; sets its colour and icon. */
enum class AlertTone { NEUTRAL, INFO, SUCCESS, WARNING, DANGER }

private val AlertShape = RoundedCornerShape(12.dp)

@Composable
private fun AlertTone.color(): Color = MaterialTheme.medColors.let {
    when (this) {
        AlertTone.NEUTRAL -> it.inkMuted
        AlertTone.INFO -> it.ink
        AlertTone.SUCCESS -> it.taken
        AlertTone.WARNING -> it.amber
        AlertTone.DANGER -> it.missed
    }
}

private fun AlertTone.icon(): ImageVector = when (this) {
    AlertTone.SUCCESS -> Icons.Default.CheckCircle
    AlertTone.WARNING, AlertTone.DANGER -> Icons.Default.Warning
    AlertTone.NEUTRAL, AlertTone.INFO -> Icons.Default.Info
}

/**
 * The app's one alert style: a softly tinted block with a coloured edge, an icon for the tone, a title and an
 * optional description, an optional action on the right, and an optional close button. Closing collapses it
 * in height and fades it out before [onDismiss] runs. Screen readers announce it when it appears.
 */
@Composable
fun MedAlert(
    tone: AlertTone,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    // A new message gets a fresh alert, even after the previous one at this spot was closed.
    val visible = remember(title, description) { MutableTransitionState(true) }
    // Runs onDismiss once the collapse has finished.
    if (!visible.targetState && visible.isIdle) LaunchedEffect(Unit) { onDismiss?.invoke() }
    AnimatedVisibility(visible, exit = shrinkVertically(tween(220)) + fadeOut(tween(180))) {
        val accent = tone.color()
        Surface(
            shape = AlertShape,
            color = accent.copy(alpha = 0.12f).compositeOver(MaterialTheme.medColors.paperRaised),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
            modifier = modifier.fillMaxWidth().semantics {
                liveRegion = if (tone == AlertTone.DANGER) LiveRegionMode.Assertive else LiveRegionMode.Polite
            },
        ) {
            Row(Modifier.heightIn(min = 48.dp).padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.Top) {
                Icon(tone.icon(), contentDescription = null, tint = if (tone == AlertTone.WARNING) MaterialTheme.medColors.ink else accent, modifier = Modifier.padding(top = 2.dp).size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.medColors.ink)
                    description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.medColors.inkMuted) }
                }
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp).align(Alignment.CenterVertically)) { Text(actionLabel) }
                }
                if (onDismiss != null) {
                    IconButton(onClick = { visible.targetState = false }, modifier = Modifier.align(Alignment.CenterVertically)) {
                        Icon(Icons.Default.Close, "Dismiss", tint = MaterialTheme.medColors.inkMuted)
                    }
                }
            }
        }
    }
}
