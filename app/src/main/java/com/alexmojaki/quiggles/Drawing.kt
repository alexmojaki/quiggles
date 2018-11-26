package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.DisplayMetrics
import kotlin.math.min

class Drawing {

    val quiggles = ArrayList<Quiggle>()
    var selectedQuiggles: List<Quiggle> = emptyList()
    var selectedQuiggle: Quiggle? = null
    var packing: ScreenPacking? = null

    lateinit var metrics: DisplayMetrics
    val swidth by lazy { metrics.widthPixels }
    val sheight by lazy { metrics.heightPixels }
    val scenter by lazy { Point(swidth / 2f, sheight / 2f) }

    inner class ScreenPacking(packing: Packing) {
        val scale = min(
            metrics.widthPixels / packing.width.toFloat(),
            metrics.heightPixels / packing.height.toFloat()
        )

        val centersAndQuiggles: List<Pair<Point, Quiggle>>

        init {
            val matrix = Matrix()
            (scenter - packing.boxCenter).translate(matrix)
            scenter.scale(matrix, scale)
            centersAndQuiggles = packing.centers.zip(selectedQuiggles).map { (c, quiggle) ->
                Pair(matrix * c, quiggle)
            }
        }
    }

    fun draw(canvas: Canvas) {
        canvas.drawColor(DEFAULT_BG_COLOR)

        if (quiggles.isEmpty()) {
            return
        }

        if (selectedQuiggles.isEmpty()) {
            for (quiggle in quiggles) {
                quiggle.draw(canvas, scenter, sheight)
            }
            return
        }

        if (selectedQuiggle != null) {
            selectedQuiggle!!.draw(canvas, scenter, sheight, scale = swidth / 2f)
            return
        }

        for (quiggle in quiggles - selectedQuiggles) {
            quiggle.draw(canvas, scenter, sheight, brightness = 0.3f)
        }

        for ((c, quiggle) in packing!!.centersAndQuiggles) {
            quiggle.draw(canvas, c, sheight, scale = packing!!.scale)
        }
    }

    fun touchStart(point: Point) {
        if (selectedQuiggles.isNotEmpty())
            return
        val quiggle = Quiggle()
        quiggle.start(point)
        quiggles.add(quiggle)
    }

    fun touchMove(point: Point) {
        if (selectedQuiggles.isNotEmpty())
            return
        quiggles.last().addPoint(point)
    }

    fun touchUp(point: Point) {
        if (selectedQuiggles.isEmpty()) {
            val quiggle = quiggles.last()
            if (quiggle.points.size < 5) {
                quiggles.remove(quiggle)

                val dist = point.distance(scenter)
                selectedQuiggles = quiggles.filter { -50 + it.innerRadius <= dist && dist <= it.outerRadius + 50 }
                if (selectedQuiggles.size == 1) {
                    selectedQuiggle = selectedQuiggles[0]
                }
                if (selectedQuiggles.isNotEmpty()) {
                    packing = ScreenPacking(packing(selectedQuiggles.size))
                }
            } else {
                quiggle.finishDrawing()
            }
        } else if (selectedQuiggle == null) {
            selectedQuiggle = packing!!.centersAndQuiggles.asSequence().mapNotNull { (center, quiggle) ->
                if (point.distance(center) <= packing!!.scale) quiggle else null
            }.singleOrNull()
            if (selectedQuiggle == null) {
                selectedQuiggles = emptyList()
            }
        } else {
            selectedQuiggle = null
            if (selectedQuiggles.size == 1) {
                selectedQuiggles = emptyList()
            }
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