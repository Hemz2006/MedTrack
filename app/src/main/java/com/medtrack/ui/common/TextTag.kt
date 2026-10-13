package com.medtrack.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.medtrack.ui.theme.LabelShape

/** A small text tag in a thin label-shaped outline (never a capsule: those mean doses). */
@Composable
fun TextTag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = modifier.border(1.dp, color, LabelShape).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
