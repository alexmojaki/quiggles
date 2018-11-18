package com.alexmojaki.quiggles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.DisplayMetrics

class BitmapPaintView(context: Context, attrs: AttributeSet?) : BasePaintView(context, attrs) {
    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mBitmapPaint = Paint(Paint.DITHER_FLAG)

    override fun init(metrics: DisplayMetrics) {
        super.init(metrics)
        val height = metrics.heightPixels
        val width = metrics.widthPixels

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap!!)
    }

    override fun doDraw(canvas: Canvas) {
        drawing.draw(mCanvas!!)
        canvas.drawBitmap(mBitmap!!, 0f, 0f, mBitmapPaint)
    }
}