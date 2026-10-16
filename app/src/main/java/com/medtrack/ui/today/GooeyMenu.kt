package com.medtrack.ui.today

import com.medtrack.ui.theme.medColors
import androidx.compose.ui.graphics.luminance
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// EasyUI Gooey Menu, scaled to sit in Today's header: pill, gap, row height and panel padding.
private val WIDTH: Dp = 148.dp
private val PILL_H: Dp = 38.dp
private val GAP: Dp = 8.dp
private val ROW_H: Dp = 40.dp
private val PANEL_PAD: Dp = 8.dp
private val PANEL_RADIUS: Dp = 22.dp

// Its colours: black ink, light text, a darker row under the highlighted option and a blue dot for the chosen one.
private val Dot = Color(0xFF3B82F6)
private val TriggerText = Color(0xFFF4F4F4)
private val RowText = Color(0xFFDCDCDC)
private val Chevron = Color(0xFFCFCFCF)

/** The ink and highlighted-row colours: EasyUI's near-black in dark mode, the app's deep green in light mode. */
private val Ink: Color @Composable get() = if (MaterialTheme.medColors.paper.luminance() < 0.5f) Color(0xFF0E0E0E) else MaterialTheme.medColors.ink
private val RowActive: Color @Composable get() = if (MaterialTheme.medColors.paper.luminance() < 0.5f) Color(0xFF232323) else Color(0xFF1F5A45)

// Its framer-motion springs, converted: Compose stiffness = k / m, damping ratio = c / (2·√(k·m)).
private fun drop() = spring<Float>(dampingRatio = 0.55f, stiffness = 240f) //   240 / 17 / 1
private fun grow() = spring<Float>(dampingRatio = 0.76f, stiffness = 210f) //   210 / 22 / 1
private fun snap() = spring<Float>(dampingRatio = 0.68f, stiffness = 267f) //   240 / 20 / 0.9
private fun row() = spring<Float>(dampingRatio = 0.63f, stiffness = 420f) //    420 / 26 / 1

/**
 * A liquid dropdown, after EasyUI's Gooey Menu: a dark panel drops out of the trigger pill and stretches open on
 * springs, joined to it by an ink-like bridge (blur 10 then a sharp alpha threshold — Android 12+; older phones
 * get the same motion without the bridge). Options fade down into it one after another; the chosen one has a
 * blue dot. Tap outside or the trigger to close.
 */
@Composable
fun <T> GooeyMenu(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val panelH = ROW_H * options.size + PANEL_PAD * 2
    val panelY = with(density) { (PILL_H + GAP).toPx() }
    val closedScale = PILL_H / panelH

    var open by remember { mutableStateOf(false) }
    var showing by remember { mutableStateOf(false) }
    val y = remember { Animatable(0f) }
    val scale = remember { Animatable(closedScale) }
    val rows = remember(options.size) { List(options.size) { Animatable(0f) } }

    LaunchedEffect(open) {
        if (open) {
            showing = true
            coroutineScope {
                launch { y.animateTo(panelY, drop()) }
                launch { delay(50); scale.animateTo(1f, grow()) }
                rows.forEachIndexed { i, r -> launch { delay(120L + 35L * i); r.animateTo(1f, row()) } }
            }
        } else if (showing) {
            coroutineScope {
                rows.reversed().forEachIndexed { i, r -> launch { delay(15L * i); r.animateTo(0f, snap()) } }
                launch { y.animateTo(0f, snap()) }
                launch { scale.animateTo(closedScale, snap()) }
            }
            showing = false
        }
    }

    Box(modifier) {
        Trigger(label(selected), open, Modifier.alpha(if (showing) 0f else 1f)) { open = true }
        if (showing) {
            Popup(alignment = Alignment.TopEnd, onDismissRequest = { open = false }, properties = PopupProperties(focusable = true)) {
                Box(Modifier.size(WIDTH, PILL_H + GAP + panelH)) {
                    // The ink: the pill and the panel as plain shapes, blurred and thresholded so they melt together.
                    Box(
                        Modifier.size(WIDTH, PILL_H + GAP + panelH).graphicsLayer {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) renderEffect = goo(10.dp.toPx())
                        },
                    ) {
                        Box(Modifier.size(WIDTH, PILL_H).background(Ink, CircleShape))
                        Box(
                            Modifier
                                .graphicsLayer {
                                    translationY = y.value
                                    scaleY = scale.value
                                    transformOrigin = TransformOrigin(0.5f, 0f)
                                }
                                .size(WIDTH, panelH)
                                .background(Ink, RoundedCornerShape(PANEL_RADIUS)),
                        )
                    }
                    // The words, sharp, on top of the ink.
                    Trigger(label(selected), open) { open = !open }
                    Column(Modifier.offset(y = PILL_H + GAP).padding(vertical = PANEL_PAD)) {
                        options.forEachIndexed { i, option ->
                            val chosen = option == selected
                            val shown = rows[i].value
                            Row(
                                Modifier
                                    .graphicsLayer {
                                        alpha = shown.coerceIn(0f, 1f)
                                        translationY = (1f - shown) * -10.dp.toPx()
                                    }
                                    .padding(horizontal = 8.dp)
                                    .width(WIDTH - 16.dp).height(ROW_H)
                                    .background(if (chosen) RowActive else Color.Transparent, RoundedCornerShape(14.dp))
                                    .clickable { open = false; onSelect(option) }
                                    .semantics {
                                        role = Role.RadioButton
                                        this.selected = chosen
                                        contentDescription = label(option)
                                    }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(label(option), color = if (chosen) Color.White else RowText, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                if (chosen) Box(Modifier.size(6.dp).background(Dot, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Trigger(text: String, open: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val turn by animateFloatAsState(if (open) 180f else 0f, spring(dampingRatio = 0.58f, stiffness = 300f), label = "chevron")
    Row(
        modifier
            .size(WIDTH, PILL_H)
            .background(Ink, CircleShape)
            .clickable(role = Role.Button, onClickLabel = if (open) "Close view menu" else "Change view", onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = TriggerText, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f), maxLines = 1)
        Icon(Icons.Default.KeyboardArrowDown, null, tint = Chevron, modifier = Modifier.size(18.dp).rotate(turn))
    }
}

/** EasyUI's SVG filter: feGaussianBlur(10) then alpha × 22 − 14 — soft overlaps become one sharp-edged blob. */
@RequiresApi(Build.VERSION_CODES.S)
private fun goo(blurPx: Float) = RenderEffect.createChainEffect(
    RenderEffect.createColorFilterEffect(
        ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 22f, -14f * 255f,
                ),
            ),
        ),
    ),
    RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.DECAL),
).asComposeRenderEffect()
