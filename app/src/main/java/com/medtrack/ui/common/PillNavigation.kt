package com.medtrack.ui.common

import androidx.compose.foundation.layout.asPaddingValues
import com.medtrack.ui.theme.medColors
import androidx.compose.ui.graphics.luminance
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

/** The island's colours: see-through so the page shows behind it. */
private class IslandColors(val bar: Color, val border: Color, val pill: Color, val active: Color, val idle: Color)

@Composable
private fun islandColors(): IslandColors {
    val m = MaterialTheme.medColors
    return if (m.paper.luminance() < 0.5f) {
        // EasyUI's palette (near-black bar, white pill, grey labels), with the bar made translucent.
        IslandColors(Color(0xFF050505).copy(alpha = 0.55f), Color(0xFF1F1F1F).copy(alpha = 0.7f), Color(0xFFFAFAFA), Color(0xFF050505), Color(0xFFA1A1A1))
    } else {
        // Light theme: frosted white with the green ink as the pill.
        IslandColors(Color.White.copy(alpha = 0.6f), m.foil.copy(alpha = 0.9f), m.ink, Color.White, m.inkMuted)
    }
}

/** EasyUI's "springMorph" (stiffness 220, damping 32, mass 1): a long, smooth settle with no overshoot. */
private fun <T> morph() = spring<T>(dampingRatio = 1.08f, stiffness = 220f)

/** Room to leave at the bottom of a page so its last item can scroll clear of the island (plus the system bar). */
val NavIslandClearance: Dp = 72.dp

/** [NavIslandClearance] plus the system navigation bar, for a tab page's bottom padding. */
@Composable
fun islandBottom(): Dp = NavIslandClearance + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * A floating, see-through segmented "island" of pages; the page scrolls underneath it. The white pill morphs across — sliding and resizing — to the page
 * you tap; each tab shrinks slightly while pressed.
 */
@Composable
fun PillNavigation(items: List<PillNavItem>, selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val selected = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val density = LocalDensity.current
    // Each tab's left edge and width inside the bar, measured as laid out, so the pill can fit any label.
    val bounds = remember { mutableStateMapOf<Int, Triple<Dp, Dp, Dp>>() }
    val colors = islandColors()
    Box(
        modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = colors.bar, border = BorderStroke(1.dp, colors.border)) {
            Box(Modifier.padding(4.dp)) {
                bounds[selected]?.let { (left, width, height) ->
                    val x by animateDpAsState(left, morph(), label = "pill x")
                    val w by animateDpAsState(width, morph(), label = "pill width")
                    Box(
                        Modifier.offset(x = x).size(w, height)
                            .shadow(4.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                            .background(colors.pill, CircleShape),
                    )
                }
                Row(Modifier.selectableGroup()) {
                    items.forEachIndexed { index, item ->
                        val active = index == selected
                        val interaction = remember { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()
                        val press by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120), label = "press")
                        val tint by animateColorAsState(if (active) colors.active else colors.idle, tween(200), label = "tint")
                        Box(
                            Modifier
                                .onPlaced { c ->
                                    with(density) { bounds[index] = Triple(c.positionInParent().x.toDp(), c.size.width.toDp(), c.size.height.toDp()) }
                                }
                                .scale(press)
                                .heightIn(min = 40.dp)
                                .selectable(selected = active, interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(item.id) }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
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
