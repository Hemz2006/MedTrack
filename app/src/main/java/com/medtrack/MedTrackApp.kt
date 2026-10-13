package com.medtrack

import android.app.Application
import android.content.Context
import com.medtrack.reminders.DoseNotifier

class MedTrackApp : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        DoseNotifier(this).ensureChannel()
    }
}

val Context.container: AppContainer get() = (applicationContext as MedTrackApp).container
