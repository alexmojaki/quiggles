package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.DisplayMetrics

class Drawing {

    val quiggles = ArrayList<Quiggle>()
    var selectedQuiggles: List<Quiggle> = emptyList()

    lateinit var metrics: DisplayMetrics
    val swidth by lazy { metrics.widthPixels }
    val sheight by lazy { metrics.heightPixels }
    val scenter by lazy { Point(swidth / 2f, sheight / 2f) }

    fun draw(canvas: Canvas) {
        canvas.drawColor(DEFAULT_BG_COLOR)
        if (quiggles.isEmpty()) {
            return
        }

        if (selectedQuiggles.isEmpty()) {
            for (quiggle in quiggles) {
                quiggle.draw(canvas, scenter, sheight)
            }
        } else {
            for (quiggle in quiggles - selectedQuiggles) {
                quiggle.draw(canvas, scenter, sheight, brightness = 0.3f)
            }

            val packing = packing(selectedQuiggles.size)
            val scale = packing.scale(metrics)

            for ((c, quiggle) in packing.centers.zip(selectedQuiggles)) {
                val matrix = Matrix()
                (scenter - packing.boxCenter).translate(matrix)
                scenter.scale(matrix, scale)
                quiggle.draw(canvas, matrix * c, sheight, scale = scale)
            }
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

    fun touchUp(x: Float, y: Float) {
        val quiggle = quiggles.last()
        if (quiggle.points.size < 5) {
            quiggles.remove(quiggle)
            selectedQuiggles = if (selectedQuiggles.isEmpty()) {
                val dist = Point(x, y).distance(scenter)
                quiggles.filter { -50 + it.innerRadius <= dist && dist <= it.outerRadius + 50 }
            } else {
                emptyList()
            }
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