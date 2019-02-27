package com.alexmojaki.quiggles

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class PaintView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    lateinit var drawing: Drawing

    fun init(activity: CommonActivity) {
        val metrics = activity.metrics
        drawing = Drawing(Point(metrics.widthPixels / 2, metrics.heightPixels / 2))
        drawing.activity = activity
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (drawing.activity !is MainActivity) return false

        val point = Point(event.x, event.y)

        if (when (event.action) {
                MotionEvent.ACTION_DOWN -> drawing.touchStart(point)
                MotionEvent.ACTION_MOVE -> drawing.touchMove(point)
                MotionEvent.ACTION_UP -> drawing.touchUp(point)
                MotionEvent.ACTION_CANCEL -> drawing.touchCancel()
                else -> null
            } != null
        ) {
            invalidate()
        }

        return true
    }

    override fun onDraw(canvas: Canvas) {
        drawing.draw(canvas)
        drawing.update()
        invalidate()
    }

}