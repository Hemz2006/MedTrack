package com.medtrack.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.medtrack.data.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private val placeholderColors = listOf(
    Color(0xFF2E7D6B), Color(0xFF5C6BC0), Color(0xFFEF6C00), Color(0xFF8E24AA), Color(0xFF00838F), Color(0xFFC62828),
)

/** The medication's photo, or a coloured tile with its initial. */
@Composable
fun PhotoThumb(path: String?, name: String, size: Dp) {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = path?.let { withContext(Dispatchers.IO) { PhotoStore.decodeSampled(it, 512)?.asImageBitmap() } }
    }
    val shape = RoundedCornerShape(12)
    val current = bitmap
    if (current != null) {
        Image(current, "Photo of $name", Modifier.size(size).clip(shape), contentScale = ContentScale.Crop)
    } else {
        Box(
            Modifier.size(size).clip(shape).background(placeholderColors[abs(name.hashCode()) % placeholderColors.size]),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }
}
