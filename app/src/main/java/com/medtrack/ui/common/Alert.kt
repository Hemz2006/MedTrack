package com.medtrack.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medtrack.R
import com.medtrack.ui.theme.medColors

/** How serious an alert is; sets its colour and icon. */
enum class AlertTone { NEUTRAL, INFO, SUCCESS, WARNING, DANGER }

/*
 * Colours from EasyUI's alert (Tailwind): light = tone-50 at 70% with a tone-200 border and a tone-600 icon;
 * dark = tone-500 at 12% with a tone-500/30 border and a tone-400 icon. Neutral uses the grey scale.
 */
private class ToneColors(val c50: Long, val c200: Long, val c400: Long, val c500: Long, val c600: Long)

private val SKY = ToneColors(0xFFF0F9FF, 0xFFBAE6FD, 0xFF38BDF8, 0xFF0EA5E9, 0xFF0284C7)
private val EMERALD = ToneColors(0xFFECFDF5, 0xFFA7F3D0, 0xFF34D399, 0xFF10B981, 0xFF059669)
private val AMBER = ToneColors(0xFFFFFBEB, 0xFFFDE68A, 0xFFFBBF24, 0xFFF59E0B, 0xFFD97706)
private val RED = ToneColors(0xFFFEF2F2, 0xFFFECACA, 0xFFF87171, 0xFFEF4444, 0xFFDC2626)

private data class AlertLook(val background: Color, val border: Color, val icon: Color, @DrawableRes val glyph: Int)

@Composable
private fun AlertTone.look(): AlertLook {
    val dark = MaterialTheme.medColors.paper.luminance() < 0.5f
    val base = MaterialTheme.medColors.paperRaised
    if (this == AlertTone.NEUTRAL) {
        return if (dark) {
            AlertLook(Color(0xFF171717).copy(alpha = 0.6f).compositeOver(base), Color(0xFF262626), Color(0xFFA3A3A3), R.drawable.ic_alert_info)
        } else {
            AlertLook(Color(0xFFFAFAFA).copy(alpha = 0.7f).compositeOver(base), Color(0xFFE5E5E5), Color(0xFF737373), R.drawable.ic_alert_info)
        }
    }
    val (t, glyph) = when (this) {
        AlertTone.INFO -> SKY to R.drawable.ic_alert_info
        AlertTone.SUCCESS -> EMERALD to R.drawable.ic_alert_success
        AlertTone.WARNING -> AMBER to R.drawable.ic_alert_warning
        else -> RED to R.drawable.ic_alert_danger
    }
    return if (dark) {
        AlertLook(Color(t.c500).copy(alpha = 0.12f).compositeOver(base), Color(t.c500).copy(alpha = 0.3f), Color(t.c400), glyph)
    } else {
        AlertLook(Color(t.c50).copy(alpha = 0.7f).compositeOver(base), Color(t.c200), Color(t.c600), glyph)
    }
}

private val AlertShape = RoundedCornerShape(12.dp)
private val Collapse = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/**
 * The app's one alert style (after EasyUI's Alert): a tinted, bordered block with the tone's icon, a title and
 * optional description, an optional action on the right and an optional close button. Closing collapses its
 * height and fades it over 250 ms before [onDismiss] runs. Warnings and dangers are announced right away.
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
    if (!visible.targetState && visible.isIdle) LaunchedEffect(Unit) { onDismiss?.invoke() }
    AnimatedVisibility(visible, exit = shrinkVertically(tween(250, easing = Collapse)) + fadeOut(tween(250, easing = Collapse))) {
        val look = tone.look()
        val urgent = tone == AlertTone.DANGER || tone == AlertTone.WARNING
        Surface(
            shape = AlertShape,
            color = look.background,
            border = BorderStroke(1.dp, look.border),
            modifier = modifier.fillMaxWidth().semantics { liveRegion = if (urgent) LiveRegionMode.Assertive else LiveRegionMode.Polite },
        ) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Icon(painterResource(look.glyph), contentDescription = null, tint = look.icon, modifier = Modifier.padding(top = 3.dp).size(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.medColors.ink)
                    description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.medColors.inkMuted) }
                }
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction, modifier = Modifier.align(Alignment.CenterVertically)) { Text(actionLabel) }
                }
                if (onDismiss != null) {
                    // EasyUI's 28dp close button, inside a 48dp touch area.
                    Box(
                        Modifier.size(48.dp).padding(10.dp).clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClickLabel = "Dismiss") { visible.targetState = false },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.Close, "Dismiss", tint = MaterialTheme.medColors.inkMuted, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}
