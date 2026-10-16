package com.medtrack.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medtrack.ui.common.PhotoSourceButtons
import com.medtrack.ui.common.PhotoSources
import com.medtrack.ui.common.PhotoThumb
import com.medtrack.ui.theme.medColors

/** The medication's photos; the first is the main one. Each can be removed or made main. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotosEditor(
    photos: List<String>,
    name: String,
    sources: PhotoSources,
    onRemove: (String) -> Unit,
    onMakeMain: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (photos.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.forEachIndexed { index, path ->
                    Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            PhotoThumb(path, "$name, photo ${index + 1}", 96.dp)
                            FilledIconButton(
                                onClick = { onRemove(path) },
                                modifier = Modifier.align(Alignment.TopEnd),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    contentColor = MaterialTheme.medColors.ink,
                                ),
                            ) { Icon(Icons.Default.Close, "Remove photo ${index + 1}") }
                        }
                        if (index == 0) {
                            Text(
                                "Main photo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.medColors.inkMuted,
                                modifier = Modifier.heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically),
                            )
                        } else {
                            TextButton(onClick = { onMakeMain(path) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Make main") }
                        }
                    }
                }
            }
        }
        PhotoSourceButtons(sources)
    }
}
