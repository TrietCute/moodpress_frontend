package com.example.moodpress.core.utils

import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class DraggableTouchListener(
    private val onClick: () -> Unit
) : View.OnTouchListener {

    private var dX = 0f
    private var dY = 0f
    private var startX = 0f
    private var startY = 0f
    private var isDragging = false
    private val CLICK_DRAG_TOLERANCE = 10f

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dX = view.x - event.rawX
                dY = view.y - event.rawY
                startX = event.rawX
                startY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val newX = event.rawX + dX
                val newY = event.rawY + dY

                view.animate()
                    .x(newX)
                    .y(newY)
                    .setDuration(0)
                    .start()

                if (abs(event.rawX - startX) > CLICK_DRAG_TOLERANCE ||
                    abs(event.rawY - startY) > CLICK_DRAG_TOLERANCE) {
                    isDragging = true
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    onClick()
                } else {
                    val parentWidth = (view.parent as View).width
                    val viewCenterX = view.x + view.width / 2

                    val destX = if (viewCenterX < parentWidth / 2) {
                        0f + 20f
                    } else {
                        parentWidth.toFloat() - view.width - 20f
                    }

                    view.animate()
                        .x(destX)
                        .setDuration(300)
                        .start()
                }
                return true
            }
        }
        return false
    }
}