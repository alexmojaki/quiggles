package com.alexmojaki.quiggles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet

class BitmapPaintView(context: Context, attrs: AttributeSet?) : BasePaintView(context, attrs) {
    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mBitmapPaint = Paint(Paint.DITHER_FLAG)

    override fun init(activity: MainActivity) {
        super.init(activity)
        val height = drawing.metrics.heightPixels
        val width = drawing.metrics.widthPixels

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap!!)
    }

    override fun doDraw(canvas: Canvas) {
        drawing.draw(mCanvas!!)
        canvas.drawBitmap(mBitmap!!, 0f, 0f, mBitmapPaint)
    }
}