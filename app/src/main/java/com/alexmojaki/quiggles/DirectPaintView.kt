package com.alexmojaki.quiggles

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet

class DirectPaintView(context: Context, attrs: AttributeSet?) : BasePaintView(context, attrs) {

    override fun doDraw(canvas: Canvas) {
        drawing.draw(canvas)
    }
}