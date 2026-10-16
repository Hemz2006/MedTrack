package com.medtrack.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CropFrameTest {
    private val frame = CropFrame(0.2f, 0.2f, 0.6f, 0.6f)
    private fun assertFrame(expected: CropFrame, actual: CropFrame) {
        listOf(expected.left to actual.left, expected.top to actual.top, expected.right to actual.right, expected.bottom to actual.bottom)
            .forEach { (e, a) -> assertEquals(e, a, 0.0001f) }
    }

    @Test fun movingKeepsSizeAndStaysInsideThePhoto() {
        assertFrame(CropFrame(0.3f, 0.1f, 0.7f, 0.5f), frame.moved(0.1f, -0.1f))
        assertFrame(CropFrame(0.6f, 0f, 1f, 0.4f), frame.moved(5f, -5f)) // pushed to the edge, same size
    }

    @Test fun cornersResizeButNotBelowTheMinimumOrPastTheEdge() {
        assertFrame(CropFrame(0.2f, 0.2f, 0.8f, 0.7f), frame.resized(CropHandle.BOTTOM_RIGHT, 0.2f, 0.1f))
        assertFrame(CropFrame(0f, 0f, 0.6f, 0.6f), frame.resized(CropHandle.TOP_LEFT, -1f, -1f))
        assertFrame(CropFrame(0.5f, 0.2f, 0.6f, 0.6f), frame.resized(CropHandle.TOP_LEFT, 0.9f, 0f)) // stops at MIN wide
    }

    @Test fun touchesPickACornerFirstThenTheInside() {
        assertEquals(CropHandle.TOP_LEFT, frame.handleAt(0.21f, 0.19f, 0.03f, 0.03f))
        assertEquals(CropHandle.BOTTOM_RIGHT, frame.handleAt(0.6f, 0.61f, 0.03f, 0.03f))
        assertEquals(CropHandle.MOVE, frame.handleAt(0.4f, 0.4f, 0.03f, 0.03f))
        assertNull(frame.handleAt(0.9f, 0.9f, 0.03f, 0.03f))
    }
}
