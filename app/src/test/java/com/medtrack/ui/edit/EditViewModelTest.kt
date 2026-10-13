package com.medtrack.ui.edit

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medtrack.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class EditViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val container = AppContainer(context)

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        WorkManager.initialize(context, Configuration.Builder().build()) // saving refreshes reminders, which schedules work
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        container.db.close()
    }

    @Test fun doubleSaveInsertsOnce() = runBlocking {
        val vm = EditViewModel(container, 0L)
        vm.update { it.copy(genericName = "metformin") }
        vm.save()
        vm.save()
        withTimeout(10_000) { vm.state.first { it.saved } }
        vm.save() // after completion: still a no-op
        assertEquals(1, container.db.medicationDao().active().size)
        assertTrue(vm.state.value.error == null)
    }
}
