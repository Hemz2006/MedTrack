package com.medtrack.ui.common

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.medtrack.ui.theme.medColors
import com.medtrack.ui.today.CapsuleState

/**
 * One dose as a blister-pack pocket. Capsule shapes are reserved for doses.
 * Callers give it a contentDescription; the drawing itself carries no semantics.
 */
@Composable
fun Capsule(
    state: CapsuleState,
    modifier: Modifier = Modifier,
    width: Dp = 28.dp,
    height: Dp = 44.dp,
    popWhenTaken: Boolean = false,
) {
    val colors = MaterialTheme.medColors
    val scale = remember { Animatable(1f) }
    var last by remember { mutableStateOf(state) }
    val animate = popWhenTaken && rememberAnimationsEnabled()
    LaunchedEffect(state) {
        if (animate && state == CapsuleState.TAKEN && last != CapsuleState.TAKEN) {
            scale.animateTo(1.2f, tween(75))
            scale.animateTo(1f, tween(75))
        }
        last = state
    }
    Canvas(
        modifier.size(width, height).graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
    ) {
        val stroke = 2.dp.toPx()
        val inset = stroke / 2
        val outline = Size(size.width - stroke, size.height - stroke)
        val outlineRadius = CornerRadius(outline.width / 2)
        when (state) {
            CapsuleState.TAKEN -> {
                drawRoundRect(colors.foil, cornerRadius = CornerRadius(size.width / 2))
                val check = Path().apply {
                    moveTo(size.width * 0.28f, size.height * 0.52f)
                    lineTo(size.width * 0.45f, size.height * 0.62f)
                    lineTo(size.width * 0.73f, size.height * 0.40f)
                }
                drawPath(check, colors.ink, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            CapsuleState.DUE -> drawRoundRect(colors.amber, cornerRadius = CornerRadius(size.width / 2))
            CapsuleState.MISSED -> {
                drawRoundRect(colors.missed, Offset(inset, inset), outline, outlineRadius, style = Stroke(stroke))
                val clip = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(size.width / 2))) }
                clipPath(clip) {
                    drawLine(colors.missed, Offset(0f, size.height * 0.8f), Offset(size.width, size.height * 0.2f), stroke)
                }
            }
            CapsuleState.SKIPPED -> drawRoundRect(
                colors.foil, Offset(inset, inset), outline, outlineRadius,
                style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
            )
            CapsuleState.UPCOMING -> drawRoundRect(colors.ink, Offset(inset, inset), outline, outlineRadius, style = Stroke(1.5.dp.toPx()))
        }
    }
}

/** False when the user turned animations off (animator duration scale 0). */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }
}
