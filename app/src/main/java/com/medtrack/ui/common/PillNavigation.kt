package com.medtrack.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medtrack.ui.theme.medColors

data class PillNavItem(val id: String, val label: String, val icon: ImageVector)

/**
 * A floating "island" of page buttons. The selected page sits on a dark pill that springs across to whichever
 * page you tap, rather than each button lighting up on its own.
 */
@Composable
fun PillNavigation(items: List<PillNavItem>, selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.medColors
    val selected = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    Box(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = colors.paperRaised, shadowElevation = 10.dp, tonalElevation = 0.dp) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(64.dp).padding(5.dp)) {
                val segment = maxWidth / items.size
                val x by animateDpAsState(
                    segment * selected,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                    label = "pill",
                )
                Box(Modifier.offset(x = x).width(segment).fillMaxHeight().background(colors.ink, CircleShape))
                Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
                    items.forEachIndexed { index, item ->
                        val active = index == selected
                        val tint by animateColorAsState(if (active) colors.paper else colors.inkMuted, label = "tint")
                        Column(
                            Modifier.weight(1f).fillMaxHeight()
                                .selectable(selected = active, role = Role.Tab, onClick = { onSelect(item.id) })
                                .padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                            Text(item.label, color = tint, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
