package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import com.alexmojaki.quiggles.Tutorial.State.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.min

class Drawing(val scenter: Point) {

    var filename: String? = null
    val quiggles = ArrayList<Quiggle>()
    var selectedQuiggles: List<Quiggle> = emptyList()
    var selectedQuiggle: Quiggle? = null
    var packing: Packing? = null
    var tutorialQuiggle: TutorialQuiggle? = null
    val tutorial: Tutorial? get() {
        if (::activity.isInitialized) {
            return activity.tutorial
        }
        return null
    }
    var starField: StarField? = null
    var maxQuiggles = 10

    lateinit var activity: MainActivity
    var edited = false

    fun draw(canvas: Canvas) {
        canvas.drawColor(DEFAULT_BG_COLOR)
        starField?.draw(canvas)

        for (quiggle in quiggles.sortedBy { it.brightness() }) {
            quiggle.draw(canvas, tutorial?.state == Select)
        }
        tutorialQuiggle?.draw(canvas)
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
        val quiggle = quiggles.last()
        quiggle.addPoint(point)
    }

    fun selectNone() {
        packing = null
        selectedQuiggle = null
        selectedQuiggles = emptyList()
        activity.seekBar.visibility = INVISIBLE
        edited = false
        updateButtons()

        for (quiggle in quiggles) {
            val period = 1.2
            resetQuigglePosition(quiggle, period)
            quiggle.setBrightness(1.0, period)
        }

        if (SelectedOne.visited) {
            tutorial?.state = PressBackButton
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
        quiggle.setPosition(scenter, scenter.x / quiggle.outerRadius, period)

        for (other in quiggles - quiggle) {
            other.setBrightness(0.0, period)
        }

        tutorial?.state = SelectedOne
    }

    fun selectMany(selection: List<Quiggle>) {
        selectedQuiggles = selection

        val n = selectedQuiggles.size
        if (n == 1) {
            selectOne(selectedQuiggles[0])
            return
        }

        selectedQuiggle = null

        if (selection.isEmpty()) {
            return
        }

        if (packing == null || packing!!.n != n) {
            packing = packing(n)
        }

        val packing = packing!!

        val scale = min(
            scenter.x * 2 / packing.width,
            scenter.y * 2 / packing.height
        )

        val period = 0.7

        val matrix = Matrix()
        (scenter - packing.boxCenter).translate(matrix)
        scenter.scale(matrix, scale.toFloat())

        val oldCenters = selectedQuiggles.map { it.centerAnimation.currentValue() }
        var newCenters = packing.centers.map { matrix * it }
        if (n <= 7) {
            val permutations: Iterable<IntArray> = Permutations(n)
            newCenters = permutations.asSequence().map { it.map { i -> newCenters[i] } }.minBy {
                val distances = it.zip(oldCenters).map { (newC, oldC) -> newC.distance(oldC) }
                distances.average() * distances.max()!!
            }!!
        }

        newCenters.zip(selectedQuiggles).map { (center, quiggle) ->
            quiggle.setPosition(
                center,
                scale / quiggle.outerRadius,
                period
            )
            quiggle.setBrightness(1.0, period)
        }

        for (quiggle in quiggles - selectedQuiggles) {
            quiggle.setBrightness(0.3, period)
        }

        tutorial?.state = SelectedMany
    }

    fun edit() {
        edited = true
        for (quiggle in quiggles) {
            val period = 0.7
            var scalePeriod = period
            if (quiggle != selectedQuiggle) {
                scalePeriod = 0.0
                quiggle.setBrightness(0.5, period)
            }
            resetQuigglePosition(quiggle, scalePeriod)
        }
    }

    fun touchUp(point: Point) {
        if (edited) {
            selectNone()
            return
        }

        val quiggle = quiggles.last()
        if (!quiggle.isLongEnough()) {
            quiggles.remove(quiggle)

            when {
                selectedQuiggles.isEmpty() -> {
                    val d = point.distance(scenter)
                    val fullyVisible = nonTransitioning(includeCompleting = true).second
                    val buffer = activity.dp(25f)
                    selectMany(fullyVisible.filter {
                        val s = it.scaleAnimation.currentValue()
                        -buffer + it.innerRadius * s <= d && d <= s * it.outerRadius + buffer
                    })
                }
                selectedQuiggle == null ->
                    selectOne(selectedQuiggles.singleOrNull { it.isSelected(point) })
                else ->
                    unselectOne()
            }
        } else if (selectedQuiggle == null) {
            quiggle.finishDrawing(scenter)
            if (selectedQuiggles.isNotEmpty()) {
                selectMany(selectedQuiggles + quiggle)
            }
        } else {
            unselectOne()
        }

        updateButtons()
    }

    fun touchCancel() {
        val quiggle = quiggles.last()
        if (quiggle.state == Quiggle.State.Drawing) {
            quiggles.remove(quiggle)
        }
    }

    fun updateButtons() {
        activity.buttons.visibility =
                if (selectedQuiggle == null) INVISIBLE else VISIBLE
        activity.buttons2.visibility = INVISIBLE
        activity.resetButtons()
    }

    fun nonTransitioning(includeCompleting: Boolean): Triple<List<Quiggle>, List<Quiggle>, List<Quiggle>> {
        val notTransitioning = quiggles.filter {
            (it.state == Quiggle.State.Complete ||
                    it.state == Quiggle.State.Completing &&
                    includeCompleting) &&
                    it.visibilityAnimation.elapsedRatio() >= 1
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
        tutorialQuiggle?.update()
        starField?.update()

        if (selectedQuiggles.isNotEmpty()) return

        updateVisibility()

        if (!SelectedOne.visited) {
            val numComplete = quiggles.filter { it.state == Quiggle.State.Complete }.size
            tutorial?.state = when {
                quiggles.isEmpty() -> DrawOne
                numComplete == 1 -> DrawMore
                numComplete >= 3 -> Select
                else -> Hidden
            }
        }

        if (quiggles.firstOrNull()?.isLongEnough() == true) {
            tutorialQuiggle = null
            activity.finger.visibility = INVISIBLE
        }
    }

    private fun updateVisibility() {
        val (notTransitioning, fullyVisible, fullyInvisible) = nonTransitioning(includeCompleting = false)

        val diff = fullyVisible.size - maxQuiggles
        val quick = diff.absoluteValue > 1

        fun switchOne(part: List<Quiggle>, visibility: Double) {
            part
                .sortedBy { it.visibilityAnimation.startTime }
                .take(Math.ceil(part.size / 2.0).toInt())
                .shuffled()[0]
                .setVisibility(
                    visibility,
                    period = if (quick) 0.0 else 2.5
                )
        }

        fun hideOne() = switchOne(fullyVisible, 0.0)
        fun showOne() = switchOne(fullyInvisible, 1.0)

        if (diff > 0) {
            hideOne()
        } else if (
            (notTransitioning.size == quiggles.size || quick)
            && fullyInvisible.isNotEmpty()
        ) {
            if (diff == 0) {
                hideOne()
            }
            showOne()
        }
    }

    fun deleteSelectedQuiggle() {
        quiggles.remove(selectedQuiggle)
        selectNone()
    }

    fun resetQuigglePosition(quiggle: Quiggle, period: Double) {
        with(quiggle) {
            setPosition(scenter, usualScale, period)
            if (oscillationPeriod != Double.POSITIVE_INFINITY) {
                oscillate(scenter)
            }
        }
    }

    companion object {
        const val DEFAULT_BG_COLOR = Color.BLACK
        const val TOUCH_TOLERANCE = 8f
    }
}