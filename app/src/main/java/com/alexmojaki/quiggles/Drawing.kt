package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.DisplayMetrics
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageButton
import kotlin.math.min

class Drawing {

    val quiggles = ArrayList<Quiggle>()
    var selectedQuiggles: List<Quiggle> = emptyList()
    var selectedQuiggle: Quiggle? = null
    var packing: Packing? = null

    lateinit var metrics: DisplayMetrics
    val swidth by lazy { metrics.widthPixels }
    val sheight by lazy { metrics.heightPixels }
    val scenter by lazy { Point(swidth / 2f, sheight / 2f) }
    lateinit var buttons: List<ImageButton>

    fun draw(canvas: Canvas) {
        canvas.drawColor(DEFAULT_BG_COLOR)

        if (quiggles.isEmpty()) {
            return
        }

        if (selectedQuiggles.isEmpty()) {
            for (quiggle in quiggles) {
                quiggle.draw(canvas)
            }
            return
        }

        if (selectedQuiggle != null) {
            selectedQuiggle!!.draw(canvas)
            return
        }

        for (quiggle in quiggles - selectedQuiggles) {
            quiggle.draw(canvas, brightness = 0.3f)
        }

        for (quiggle in selectedQuiggles) {
            quiggle.draw(canvas)
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

    fun selectNone() {
        packing = null
        selectedQuiggle = null
        selectedQuiggles = emptyList()

        for (quiggle in quiggles) {
            with(quiggle) {
                setPosition(
                    scenter,
                    if (sheight / 2 < outerRadius) randomScaleFactor * sheight / 2 / outerRadius
                    else 1.0,
                    1.2
                )
            }
        }
    }

    fun unselectOne() {
        if (selectedQuiggles.size > 1) {
            selectMany(selectedQuiggles)
        } else {
            selectNone()
        }
    }

    fun selectOne(quiggle: Quiggle?) {
        if (quiggle == null) {
            selectNone()
            return
        }
        selectedQuiggle = quiggle

        quiggle.setPosition(scenter, swidth / 2 / quiggle.outerRadius, 0.7)
    }

    fun selectMany(selection: List<Quiggle>) {
        selectedQuiggles = selection

        if (selectedQuiggles.size == 1) {
            selectOne(selectedQuiggles[0])
            return
        }

        selectedQuiggle = null

        if (selection.isEmpty()) {
            return
        }

        if (packing == null) {
            packing = packing(selectedQuiggles.size)
        }

        val packing = packing!!
        assert(packing.n == selectedQuiggles.size)

        val scale = min(
            swidth / packing.width.toFloat(),
            sheight / packing.height.toFloat()
        )

        val matrix = Matrix()
        (scenter - packing.boxCenter).translate(matrix)
        scenter.scale(matrix, scale)
        packing.centers.zip(selectedQuiggles).map { (c, quiggle) ->
            quiggle.setPosition(
                matrix * c,
                scale / quiggle.outerRadius,
                0.7
            )
        }

    }

    fun touchUp(point: Point) {
        if (selectedQuiggles.isEmpty()) {
            val quiggle = quiggles.last()
            if (quiggle.points.size < 5) {
                quiggles.remove(quiggle)

                val dist = point.distance(scenter)
                selectMany(quiggles.filter {
                    val d = dist / it.scaleAnimation.currentValue()
                    -50 + it.innerRadius <= d && d <= it.outerRadius + 50
                })
            } else {
                quiggle.finishDrawing(swidth, sheight)
            }
        } else if (selectedQuiggle == null) {
            selectOne(selectedQuiggles.singleOrNull { it.isSelected(point) })
        } else {
            unselectOne()
        }

        updateButtons()
    }

    fun updateButtons() {
        for (button in buttons) {
            button.visibility = if (selectedQuiggle == null) INVISIBLE else VISIBLE
        }
    }

    fun update() {
        for (quiggle in quiggles) {
            quiggle.update()
        }
    }

    fun deleteSelectedQuiggle() {
        quiggles.remove(selectedQuiggle)
        selectNone()
        updateButtons()
    }

    companion object {
        const val DEFAULT_BG_COLOR = Color.BLACK
        const val TOUCH_TOLERANCE = 8f
    }
}