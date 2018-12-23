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

        for (quiggle in quiggles.sortedBy { it.brightness() }) {
            quiggle.draw(canvas)
        }
    }

    fun touchStart(point: Point) {
        if (selectedQuiggle != null)
            return
        val quiggle = Quiggle()
        quiggle.start(point)
        quiggles.add(quiggle)
    }

    fun touchMove(point: Point) {
        if (selectedQuiggle != null)
            return
        quiggles.last().addPoint(point)
    }

    fun selectNone() {
        packing = null
        selectedQuiggle = null
        selectedQuiggles = emptyList()

        for (quiggle in quiggles) {
            val period = 1.2
            with(quiggle) {
                setPosition(
                    scenter,
                    if (sheight / 2 < outerRadius) randomScaleFactor * sheight / 2 / outerRadius
                    else 1.0,
                    period
                )
            }
            quiggle.setBrightness(1.0, period)
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

        val period = 0.7
        quiggle.setPosition(scenter, swidth / 2 / quiggle.outerRadius, period)

        for (other in quiggles - quiggle) {
            other.setBrightness(0.0, period)
        }
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

        if (packing == null || packing!!.n != selectedQuiggles.size) {
            packing = packing(selectedQuiggles.size)
        }

        val packing = packing!!

        val scale = min(
            swidth / packing.width.toFloat(),
            sheight / packing.height.toFloat()
        )

        val period = 0.7

        val matrix = Matrix()
        (scenter - packing.boxCenter).translate(matrix)
        scenter.scale(matrix, scale)
        packing.centers.zip(selectedQuiggles).map { (c, quiggle) ->
            quiggle.setPosition(
                matrix * c,
                scale / quiggle.outerRadius,
                period
            )
            quiggle.setBrightness(1.0, period)
        }

        for (quiggle in quiggles - selectedQuiggles) {
            quiggle.setBrightness(0.3, period)
        }
    }

    fun touchUp(point: Point) {
        val quiggle = quiggles.last()
        if (quiggle.points.size < 5) {
            quiggles.remove(quiggle)

            when {
                selectedQuiggles.isEmpty() -> {
                    val dist = point.distance(scenter)
                    val fullyVisible = nonTransitioning(includeCompleting = true).second
                    selectMany(fullyVisible.filter {
                        val d = dist / it.scaleAnimation.currentValue()
                        -50 + it.innerRadius <= d && d <= it.outerRadius + 50
                    })
                }
                selectedQuiggle == null ->
                    selectOne(selectedQuiggles.singleOrNull { it.isSelected(point) })
                else ->
                    unselectOne()
            }
        } else if (selectedQuiggle == null) {
            quiggle.finishDrawing(swidth, sheight)
            if (selectedQuiggles.isNotEmpty()) {
                selectMany(selectedQuiggles + quiggle)
            }
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

    fun nonTransitioning(includeCompleting: Boolean): Triple<List<Quiggle>, List<Quiggle>, List<Quiggle>> {
        val notTransitioning = quiggles.filter {
            (it.state == Quiggle.State.Complete ||
                    it.state == Quiggle.State.Completing &&
                    includeCompleting) &&
                    it.visibilityAnimation.elapsedRatio() > 1
        }

        val (fullyVisible, fullyInvisible) = notTransitioning
            .partition {
                it.visibilityAnimation.endValue == 1.0
            }

        return Triple(notTransitioning, fullyVisible, fullyInvisible)
    }

    fun update() {
        for (quiggle in quiggles) {
            quiggle.update()
        }

        if (selectedQuiggles.isNotEmpty()) return

        val (notTransitioning, fullyVisible, fullyInvisible) = nonTransitioning(includeCompleting = false)

        fun switchOne(part: List<Quiggle>, visibility: Double) {
            part
                .sortedBy { it.visibilityAnimation.startTime }
                .take(Math.ceil(part.size / 2.0).toInt())
                .shuffled()[0]
                .setVisibility(visibility, 2.5)
        }

        fun hideOne() = switchOne(fullyVisible, 0.0)
        fun showOne() = switchOne(fullyInvisible, 1.0)

        if (fullyVisible.size > 10) {
            hideOne()
        } else {
            if (notTransitioning.size == quiggles.size && fullyInvisible.isNotEmpty()) {
                hideOne()
                showOne()
            }
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