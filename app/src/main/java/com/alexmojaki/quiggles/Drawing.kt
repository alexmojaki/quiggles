package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Color
import android.util.DisplayMetrics
import java.util.*

class Drawing {

    val quiggles = ArrayList<Quiggle>()
    lateinit var metrics: DisplayMetrics


    fun draw(canvas: Canvas) {
        canvas.drawColor(DEFAULT_BG_COLOR)

        for (quiggle in quiggles) {
            canvas.save()
            if (quiggle.state != Quiggle.State.Drawing) {
                val center = quiggle.center().toFloat()
                canvas.translate(
                    metrics.widthPixels / 2 - center.x,
                    metrics.heightPixels / 2 - center.y
                )
            }
            quiggle.draw(canvas)
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