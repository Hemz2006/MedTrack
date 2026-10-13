package com.medtrack.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhotoStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun sampleSize() {
        assertEquals(2, PhotoStore.sampleSizeFor(4000, 3000, 1200))
        assertEquals(1, PhotoStore.sampleSizeFor(1000, 800, 1200))
        assertEquals(4, PhotoStore.sampleSizeFor(6000, 8000, 1200))
    }

    @Test fun scaledSize() {
        assertEquals(1200 to 900, PhotoStore.scaledSize(4000, 3000, 1200))
        assertEquals(800 to 600, PhotoStore.scaledSize(800, 600, 1200))
        assertEquals(400 to 1200, PhotoStore.scaledSize(1000, 3000, 1200))
    }

    @Test fun deleteOnlyTouchesPhotosDir() {
        val store = PhotoStore(context)
        val inside = File(store.photosDir, "a.jpg").apply { writeText("x") }
        val outside = File(context.filesDir, "keep.txt").apply { writeText("x") }
        store.delete(inside.absolutePath)
        store.delete(outside.absolutePath)
        store.delete(null)
        assertFalse(inside.exists())
        assertTrue(outside.exists())
    }
}
