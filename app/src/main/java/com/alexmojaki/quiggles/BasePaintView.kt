package com.alexmojaki.quiggles

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View

abstract class BasePaintView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    lateinit var drawing: Drawing

    open fun init(activity: MainActivity) {
        val metrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(metrics)
        drawing = Drawing(Point(metrics.widthPixels / 2, metrics.heightPixels / 2))
        drawing.activity = activity
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val point = Point(event.x, event.y)

        if (when (event.action) {
                MotionEvent.ACTION_DOWN -> drawing.touchStart(point)
                MotionEvent.ACTION_MOVE -> drawing.touchMove(point)
                MotionEvent.ACTION_UP -> drawing.touchUp(point)
                else -> null
            } != null
        ) {
            invalidate()
        }

        return true
    }

    override fun onDraw(canvas: Canvas) {
        doDraw(canvas)

        postDelayed({
            drawing.update()
            invalidate()
        }, 16)
    }

    abstract fun doDraw(canvas: Canvas)

}