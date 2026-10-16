package com.medtrack.lookup

import java.io.IOException
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HttpTest {
    @Test
    fun readsAReplyWithinTheLimit() {
        val body = "x".repeat(20_000)
        assertEquals(body, readCapped(StringReader(body), max = 20_000))
    }

    @Test
    fun refusesAReplyOverTheLimit() {
        assertThrows(IOException::class.java) { readCapped(StringReader("x".repeat(20_001)), max = 20_000) }
    }

    @Test
    fun readsAnEmptyReply() {
        assertEquals("", readCapped(StringReader("")))
    }
}
