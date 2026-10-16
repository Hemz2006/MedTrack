package com.medtrack.ui.common

import android.util.LruCache
import androidx.compose.ui.platform.LocalDensity
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
import com.medtrack.data.MedForm
import com.medtrack.data.PhotoStore
import com.medtrack.ui.theme.LabelShape
import com.medtrack.ui.theme.medColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The medication's photo, or a foil tile with a picture of its form (or its initial when the form is unknown). */
@Composable
fun PhotoThumb(path: String?, name: String, size: Dp, modifier: Modifier = Modifier, form: MedForm? = null) =
    PhotoBox(path, name, modifier.size(size), initialSize(size), form, size * 0.55f, decodeSide = size)

/** The photo as a full-width header image (16:10). */
@Composable
fun PhotoHeader(path: String?, name: String, modifier: Modifier = Modifier) =
    PhotoBox(path, name, modifier.fillMaxWidth().aspectRatio(16f / 10f), 64.sp, null, 0.dp, decodeSide = 420.dp)

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
private fun PhotoBox(path: String?, name: String, modifier: Modifier, initial: TextUnit, form: MedForm?, iconSize: Dp, decodeSide: Dp) {
    // Decoded at about the size shown, and kept in memory, so scrolling a list doesn't re-read every photo.
    val side = with(LocalDensity.current) { decodeSide.roundToPx() }
    val key = path?.let { "$it#$side" }
    val bitmap by produceState(key?.let(PhotoCache::get), key) {
        if (path != null && value == null) {
            value = withContext(Dispatchers.IO) { PhotoStore.decodeSampled(path, side)?.asImageBitmap() }?.also { PhotoCache.put(key!!, it) }
        }
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
            if (form?.iconRes != null) {
                FormIcon(form, size = iconSize, tint = MaterialTheme.medColors.ink)
            } else {
                Text(
                    name.take(1).uppercase(),
                    color = MaterialTheme.medColors.ink,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = initial, lineHeight = initial),
                )
            }
        }
    }
}

/** Scales the initial with the tile, between body and roughly display size. */
private fun initialSize(size: Dp): TextUnit = (size.value * 0.4f).coerceIn(15f, 48f).sp

/**
 * Decodes [paths] at each of [sides] (pixels) into the cache ahead of time, so the screens that show them open
 * with their photos already in place. Photos that can't be read are skipped; they show their placeholder.
 */
suspend fun preloadPhotos(paths: Collection<String>, sides: Collection<Int>) = withContext(Dispatchers.IO) {
    for (path in paths) for (side in sides) {
        val key = "$path#$side"
        if (PhotoCache.get(key) == null) PhotoStore.decodeSampled(path, side)?.asImageBitmap()?.let { PhotoCache.put(key, it) }
    }
}

/** Recently shown photos, by path and decoded size, up to an eighth of the app's memory. */
private object PhotoCache : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4 / 1024
}
