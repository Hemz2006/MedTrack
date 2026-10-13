package com.medtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.medtrack.ui.theme.MedTrackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MedTrackTheme { Text("MedTrack") } }
    }

    override fun onResume() {
        super.onResume()
        container.appScope.launch { container.refreshReminders() }
    }
}
