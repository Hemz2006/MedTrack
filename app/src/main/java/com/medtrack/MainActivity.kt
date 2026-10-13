package com.medtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.medtrack.ui.MedTrackNav
import com.medtrack.ui.theme.MedTrackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val c = container
        setContent { MedTrackTheme { MedTrackNav(c, startOnSetup = !c.prefs.setupDone) } }
    }

    override fun onResume() {
        super.onResume()
        container.appScope.launch { container.refreshReminders() }
    }
}
