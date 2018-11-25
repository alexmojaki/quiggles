package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.DisplayMetrics

class Drawing {

    val quiggles = ArrayList<Quiggle>()
    lateinit var metrics: DisplayMetrics


    fun draw(canvas: Canvas) {
        canvas.drawColor(DEFAULT_BG_COLOR)
        if (quiggles.isEmpty()) {
            return
        }

        val packing = packing(quiggles.size)

        val swidth = metrics.widthPixels
        val sheight = metrics.heightPixels
        val scenter = Point(swidth / 2f, sheight / 2f)
        val scale = packing.scale(metrics)

        for ((c, quiggle) in packing.centers.zip(quiggles)) {
            val matrix = Matrix()
            val tc = (scenter - packing.boxCenter).toFloat()
            matrix.postTranslate(tc.x, tc.y)
            matrix.postScale(scale, scale, scenter.xf, scenter.yf)

            canvas.save()
            quiggle.draw(canvas, scale, matrix.transform(c.toFloat()).toDouble())
            canvas.restore()
        }

    }

    fun touchStart(x: Float, y: Float) {
        val quiggle = Quiggle()
        quiggle.start(x, y)
        quiggles.add(quiggle)
    }

    fun touchMove(x: Float, y: Float) {
        quiggles.last().addPoint(x, y)
    }

    fun touchUp() {
        val quiggle = quiggles.last()
        if (quiggle.points.size < 5) {
            quiggles.remove(quiggle)
        } else {
            quiggle.finishDrawing()
        }
    }

    fun update() {
        for (quiggle in quiggles) {
            quiggle.update()
        }
    }

    companion object {
        const val DEFAULT_BG_COLOR = Color.BLACK
        const val TOUCH_TOLERANCE = 8f
    }
}