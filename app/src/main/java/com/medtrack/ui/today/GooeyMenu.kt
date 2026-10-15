package com.medtrack.ui.today

import androidx.annotation.RequiresApi
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.medtrack.ui.theme.medColors

private val WIDTH: Dp = 160.dp
private val HEIGHT: Dp = 44.dp
private val GAP: Dp = 8.dp

/**
 * A liquid dropdown: the options squeeze out of the trigger pill on a spring and merge with it like drops of
 * ink (a blur-then-sharpen "metaball" filter, Android 12+; older phones get the same motion without the merge).
 */
@Composable
fun <T> GooeyMenu(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(open) {
        progress.animateTo(
            if (open) 1f else 0f,
            if (open) spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow) else tween(220),
        )
    }
    Box(modifier) {
        Trigger(label(selected), progress.value, Modifier.alpha(if (progress.value > 0f) 0f else 1f)) { open = true }
        if (open || progress.value > 0.001f) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                GooeyStack(options, selected, label, progress.value, onToggle = { open = false }) { option ->
                    open = false
                    onSelect(option)
                }
            }
        }
    }
}

@Composable
private fun Trigger(text: String, progress: Float, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.medColors
    Row(
        modifier
            .width(WIDTH).height(HEIGHT)
            .background(colors.ink, CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Change view", onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = colors.paper, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowDown, null, tint = colors.paper, modifier = Modifier.rotate(180f * progress))
    }
}

@Composable
private fun <T> GooeyStack(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    progress: Float,
    onToggle: () -> Unit,
    onPick: (T) -> Unit,
) {
    val colors = MaterialTheme.medColors
    val density = LocalDensity.current
    val step = HEIGHT + GAP
    val total = step * (options.size + 1)
    Box(Modifier.size(WIDTH, total)) {
        // The ink: every pill drawn as a plain shape, then blurred and sharpened so near pills melt together.
        Box(
            Modifier.size(WIDTH, total).graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) renderEffect = gooey(with(density) { 9.dp.toPx() })
            },
        ) {
            Box(Modifier.size(WIDTH, HEIGHT).background(colors.ink, CircleShape))
            options.forEachIndexed { i, _ ->
                val y = step * (i + 1) * progress
                Box(Modifier.offset { IntOffset(0, with(density) { y.roundToPx() }) }.size(WIDTH, HEIGHT).background(colors.ink, CircleShape))
            }
        }
        // The words, sharp, on top of the ink.
        Trigger(label(selected), progress, onClick = onToggle)
        options.forEachIndexed { i, option ->
            val y = step * (i + 1) * progress
            val isSelected = option == selected
            Row(
                Modifier
                    .offset { IntOffset(0, with(density) { y.roundToPx() }) }
                    .size(WIDTH, HEIGHT)
                    .alpha(((progress - 0.35f) / 0.65f).coerceIn(0f, 1f))
                    .clickable { onPick(option) }
                    .semantics {
                        role = Role.RadioButton
                        this.selected = isSelected
                        contentDescription = label(option)
                    }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label(option), color = colors.paper, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (isSelected) Icon(Icons.Default.Check, null, tint = colors.paper, modifier = Modifier.size(18.dp))
                else Spacer(Modifier.size(18.dp))
            }
        }
    }
}

/** Blur, then push alpha hard through a threshold: soft overlaps become one sharp-edged blob. */
@RequiresApi(Build.VERSION_CODES.S)
private fun gooey(blurPx: Float) = RenderEffect.createChainEffect(
    RenderEffect.createColorFilterEffect(
        ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 22f, -10f * 255f,
                ),
            ),
        ),
    ),
    RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.DECAL),
).asComposeRenderEffect()
