package com.medtrack

import android.app.Application
import android.content.Context
import com.medtrack.reminders.DoseNotifier
import com.medtrack.ui.theme.applyToSystem

class MedTrackApp : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        DoseNotifier(this).ensureChannel()
        // So the launch screen and splash video open in the app's own light or dark, not just the phone's.
        container.uiSettings.value.theme.applyToSystem(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as MedTrackApp).container
