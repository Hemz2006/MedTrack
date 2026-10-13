package com.medtrack.lookup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against the real brand list; skipped on a checkout that hasn't built it (tools/build_brand_index.py). */
@RunWith(AndroidJUnit4::class)
class BrandIndexTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun findsBrandsByPackName() = runBlocking {
        assumeTrue(runCatching { context.assets.open("brands.db").close() }.isSuccess)
        val index = BrandIndex(context)
        val pan = index.search("pan 40").first()
        assertEquals("PAN 40 Tablet" to "Pantoprazole (40mg)", pan.name to pan.composition)
        assertEquals("Valcivir 500 Tablet", index.search("VALCIVIR").first().name)
        assertEquals(emptyList<Brand>(), index.search("zzqqxx"))
    }

    @Test fun missingListMeansNoSuggestions() = runBlocking {
        assumeTrue(runCatching { context.assets.open("brands.db").close() }.isFailure)
        assertEquals(emptyList<Brand>(), BrandIndex(context).search("dolo"))
    }
}
