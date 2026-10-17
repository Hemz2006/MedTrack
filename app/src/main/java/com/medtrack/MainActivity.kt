package com.medtrack

import com.medtrack.ui.common.RestockPrompt
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.medtrack.data.coverPhoto
import com.medtrack.ui.MedTrackNav
import com.medtrack.ui.common.VideoSplash
import com.medtrack.ui.common.preloadPhotos
import com.medtrack.ui.theme.MedTrackTheme
import com.medtrack.ui.theme.medColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val c = container
        // The video plays when MedTrack is opened from the launcher, not when coming back to it or from an alarm.
        val playSplash = savedInstanceState == null && intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        setContent {
            MedTrackTheme {
                var splash by remember { mutableStateOf(playSplash) }
                // List thumbnails, in pixels at the current size setting, so Today and Medications open with photos in place.
                val thumbSides = with(LocalDensity.current) { listOf(40.dp.roundToPx(), 44.dp.roundToPx()) }
                val ready by produceState(!playSplash) {
                    try {
                        // Opens the database and reads the medications while the video plays.
                        val meds = c.db.medicationDao().observeAll().first()
                        preloadPhotos(meds.mapNotNull { it.coverPhoto }.toSet(), thumbSides)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.e("MedTrack", "Warm-up before the splash ended failed", e)
                    }
                    value = true
                }
                Box(Modifier.fillMaxSize()) {
                    // The app is composed straight away underneath, so its screens and data are ready when the video ends.
                    MedTrackNav(c, startOnSetup = !c.prefs.setupDone)
                    // Asked only once the app is showing, never over the opening video.
                    if (!splash) RestockPrompt(c)
                    if (splash) {
                        VideoSplash(dark = MaterialTheme.medColors.paper.luminance() < 0.5f, ready = ready) { splash = false }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.appScope.launch { container.refreshReminders() }
    }
}
