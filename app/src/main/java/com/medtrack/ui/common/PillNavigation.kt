package com.medtrack.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PillNavItem(val id: String, val label: String)

// EasyUI Pill Navigation's colours: a near-black bar, grey labels, and a white pill under the selected one.
private val Bar = Color(0xFF050505)
private val BarBorder = Color(0xFF1F1F1F)
private val Pill = Color(0xFFFAFAFA)
private val Idle = Color(0xFFA1A1A1)

/** EasyUI's "springMorph" (stiffness 220, damping 32, mass 1): a long, smooth settle with no overshoot. */
private fun <T> morph() = spring<T>(dampingRatio = 1.08f, stiffness = 220f)

/**
 * A floating segmented "island" of pages. The white pill morphs across — sliding and resizing — to the page
 * you tap; each tab shrinks slightly while pressed.
 */
@Composable
fun PillNavigation(items: List<PillNavItem>, selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val selected = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val density = LocalDensity.current
    // Each tab's left edge and width inside the bar, measured as laid out, so the pill can fit any label.
    val bounds = remember { mutableStateMapOf<Int, Pair<Dp, Dp>>() }
    Box(
        modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = Bar, border = BorderStroke(1.dp, BarBorder), shadowElevation = 8.dp) {
            Box(Modifier.padding(4.dp)) {
                bounds[selected]?.let { (left, width) ->
                    val x by animateDpAsState(left, morph(), label = "pill x")
                    val w by animateDpAsState(width, morph(), label = "pill width")
                    Box(
                        Modifier.offset(x = x).size(w, 44.dp)
                            .shadow(4.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                            .background(Pill, CircleShape),
                    )
                }
                Row(Modifier.selectableGroup()) {
                    items.forEachIndexed { index, item ->
                        val active = index == selected
                        val interaction = remember { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()
                        val press by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120), label = "press")
                        val tint by animateColorAsState(if (active) Bar else Idle, tween(200), label = "tint")
                        Box(
                            Modifier
                                .onPlaced { c ->
                                    with(density) { bounds[index] = c.positionInParent().x.toDp() to c.size.width.toDp() }
                                }
                                .scale(press)
                                .heightIn(min = 44.dp)
                                .selectable(selected = active, interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(item.id) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                item.label,
                                color = tint,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
