package com.medtrack.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medtrack.data.PhotoStore
import com.medtrack.ui.theme.LabelShape
import com.medtrack.ui.theme.medColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The medication's photo, or a foil tile with its initial. */
@Composable
fun PhotoThumb(path: String?, name: String, size: Dp, modifier: Modifier = Modifier) =
    PhotoBox(path, name, modifier.size(size), initialSize(size))

/** The photo as a full-width header image (16:10). */
@Composable
fun PhotoHeader(path: String?, name: String, modifier: Modifier = Modifier) =
    PhotoBox(path, name, modifier.fillMaxWidth().aspectRatio(16f / 10f), 64.sp)

/** Several photos as a swipeable header, with "2 of 3" beneath when there's more than one. */
@Composable
fun PhotoGallery(paths: List<String>, name: String, modifier: Modifier = Modifier) {
    if (paths.size == 1) return PhotoHeader(paths.first(), name, modifier)
    val pager = rememberPagerState { paths.size }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(pager, pageSpacing = 12.dp) { page -> PhotoHeader(paths[page], "$name, photo ${page + 1} of ${paths.size}") }
        Text(
            "Photo ${pager.currentPage + 1} of ${paths.size}, swipe for more",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.medColors.inkMuted,
        )
    }
}

@Composable
private fun PhotoBox(path: String?, name: String, modifier: Modifier, initial: TextUnit) {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = path?.let { withContext(Dispatchers.IO) { PhotoStore.decodeSampled(it, 512)?.asImageBitmap() } }
    }
    val current = bitmap
    if (current != null) {
        Image(current, "Photo of $name", modifier.clip(LabelShape), contentScale = ContentScale.Crop)
    } else {
        Box(
            modifier.clip(LabelShape).background(MaterialTheme.medColors.foil)
                .clearAndSetSemantics { contentDescription = "No photo of $name" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.take(1).uppercase(),
                color = MaterialTheme.medColors.ink,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = initial, lineHeight = initial),
            )
        }
    }
}

/** Scales the initial with the tile, between body and roughly display size. */
private fun initialSize(size: Dp): TextUnit = (size.value * 0.4f).coerceIn(15f, 48f).sp
