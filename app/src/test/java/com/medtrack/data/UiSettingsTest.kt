package com.medtrack.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiSettingsTest {
    private val prefs = AppPrefs(ApplicationProvider.getApplicationContext<Context>())

    @Test fun defaultsFollowThePhoneAtNormalSize() = assertEquals(UiSettings(), prefs.uiSettings)

    @Test fun themeAndSizesAreRemembered() {
        val chosen = UiSettings(ThemeMode.LIGHT, textScale = 1.3f, sizeScale = 1.2f, alarmColor = AlarmColor.BLUE, alarmTextColor = AlarmTextColor.YELLOW)
        prefs.uiSettings = chosen
        assertEquals(chosen, AppPrefs(ApplicationProvider.getApplicationContext<Context>()).uiSettings)
    }
}
