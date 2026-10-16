package com.medtrack.ui.common

/** Which part of the crop frame a drag started on. */
enum class CropHandle { MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/**
 * A crop rectangle as fractions (0..1) of the photo's width and height, so it doesn't depend on screen size.
 * Moves keep its size; resizes keep it at least [MIN] on each side; both keep it inside the photo.
 */
data class CropFrame(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun moved(dx: Float, dy: Float): CropFrame {
        val x = (left + dx).coerceIn(0f, 1f - width)
        val y = (top + dy).coerceIn(0f, 1f - height)
        return CropFrame(x, y, x + width, y + height)
    }

    fun resized(handle: CropHandle, dx: Float, dy: Float): CropFrame = when (handle) {
        CropHandle.MOVE -> moved(dx, dy)
        CropHandle.TOP_LEFT -> copy(left = (left + dx).coerceIn(0f, right - MIN), top = (top + dy).coerceIn(0f, bottom - MIN))
        CropHandle.TOP_RIGHT -> copy(right = (right + dx).coerceIn(left + MIN, 1f), top = (top + dy).coerceIn(0f, bottom - MIN))
        CropHandle.BOTTOM_LEFT -> copy(left = (left + dx).coerceIn(0f, right - MIN), bottom = (bottom + dy).coerceIn(top + MIN, 1f))
        CropHandle.BOTTOM_RIGHT -> copy(right = (right + dx).coerceIn(left + MIN, 1f), bottom = (bottom + dy).coerceIn(top + MIN, 1f))
    }

    /**
     * The handle under a touch at ([x], [y]) (fractions), or null when it's outside the frame. Corners win within
     * [reachX]/[reachY] (a finger's width, as fractions), so small frames can still be resized.
     */
    fun handleAt(x: Float, y: Float, reachX: Float, reachY: Float): CropHandle? {
        fun near(a: Float, b: Float, reach: Float) = kotlin.math.abs(a - b) <= reach
        return when {
            near(x, left, reachX) && near(y, top, reachY) -> CropHandle.TOP_LEFT
            near(x, right, reachX) && near(y, top, reachY) -> CropHandle.TOP_RIGHT
            near(x, left, reachX) && near(y, bottom, reachY) -> CropHandle.BOTTOM_LEFT
            near(x, right, reachX) && near(y, bottom, reachY) -> CropHandle.BOTTOM_RIGHT
            x in left..right && y in top..bottom -> CropHandle.MOVE
            else -> null
        }
    }

    companion object {
        const val MIN = 0.1f

        /** Where the frame starts: most of the photo, with a margin to show it can be dragged in. */
        val START = CropFrame(0.08f, 0.08f, 0.92f, 0.92f)
    }
}
