package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.view.View
import com.alexmojaki.quiggles.Tutorial.State.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.min

class Drawing(val scenter: Point) {

    var filename: String? = null
    val quiggles = ArrayList<Quiggle>()
    private var selectedQuiggles: List<Quiggle> = emptyList()
    private var selectedQuiggle: Quiggle? = null
    private var packing: Packing? = null
    var tutorialQuiggle: TutorialQuiggle? = null
    private val tutorial: Tutorial?
        get() =
            if (!::activity.isInitialized) null
            else (activity as? MainActivity)?.tutorial
    var starField: StarField? = null
    var maxQuiggles = 10
    var allGlow = false
    val selectedQuiggleChecked: Quiggle
        get() = selectedQuiggle ?: throw NoSelectedQuiggle()

    lateinit var activity: CommonActivity

    /**
     * When true, unselecting a single quiggle always exits selection completely
     * instead of returning to multiple selection mode. */
    var selectedQuiggleEdited = false

    fun draw(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        starField?.draw(canvas)
        for (quiggle in quiggles.sortedBySafe { it.brightness() }) {
            quiggle.draw(canvas, tutorial?.state == Select)
        }
        tutorialQuiggle?.draw(canvas)
    }

    fun touchStart(point: Point) {
        if (selectedQuiggle != null)
            return
        val quiggle = Quiggle()
        quiggle.drawing = this
        quiggle.start(point)
        quiggles.add(quiggle)
        if (allGlow) {
            quiggle.glowRandomly()
        }
    }

    fun touchMove(point: Point) {
        if (selectedQuiggle != null || quiggles.isEmpty())
            return
        val quiggle = quiggles.last()
        quiggle.addPoint(point)
    }

    fun selectNone() {
        packing = null
        selectedQuiggle = null
        selectedQuiggles = emptyList()
        selectedQuiggleEdited = false
        updateButtons()

        for (quiggle in quiggles) {
            val period = 1.2
            resetQuigglePosition(quiggle, period)
            quiggle.setBrightness(1.0, period)
        }

        if (SelectedOne.visited) {
            tutorial?.state = Hidden
        }
    }

    /** One quiggle is currently selected. Return to the previous selection state. */
    private fun unselectOne() {
        if (selectedQuiggles.size > 1 && !selectedQuiggleEdited) {
            selectMany(selectedQuiggles)
        } else {
            selectNone()
        }
    }

    fun selectOne(quiggle: Quiggle) {
        selectedQuiggle = quiggle

        val period = 0.7
        quiggle.setPosition(scenter, scenter.x / quiggle.outerRadius, period)

        for (other in quiggles - quiggle) {
            other.setBrightness(0.0, period)
        }

        tutorial?.state = SelectedOne
    }

    private fun selectMany(selection: List<Quiggle>) {
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

        // Packings are defined in a small coordinate system
        // independent of the screen. This is the factor that makes
        // it fill the screen.
        val scale = min(
            scenter.x * 2 / packing.width,
            scenter.y * 2 / packing.height
        )

        val period = 0.7

        // Transformation to center packing and fill the screen
        val matrix = Matrix()
        (scenter - packing.boxCenter).translate(matrix)
        scenter.scale(matrix, scale.toFloat())

        // Calculate positions of quiggles in transformed packing
        val oldCenters = selectedQuiggles.map { it.centerAnimation.currentValue() }
        var newCenters = packing.centers.map { matrix * it }

        // Try all possible permutations of small packings to find one that requires the least movement of quiggles
        if (n <= 7) {
            val permutations: Iterable<IntArray> = Permutations(n)
            newCenters = permutations.asSequence().map { it.map { i -> newCenters[i] } }.minBy {
                val distances = it.zip(oldCenters).map { (newC, oldC) -> newC.distance(oldC) }
                distances.average() * distances.max()
            }
        }

        // Apply packing transformation to reposition quiggles
        newCenters.zip(selectedQuiggles).map { (center, quiggle) ->
            quiggle.setPosition(
                center,
                scale / quiggle.outerRadius,
                period
            )
            quiggle.setBrightness(1.0, period)
        }

        // Dim non-selected quiggles in background
        for (quiggle in quiggles - selectedQuiggles.toSet()) {
            quiggle.setBrightness(0.3, period)
        }

        tutorial?.state = SelectedMany
    }

    /**
     * Moves all quiggles to usual position and size
     * Non-selected quiggles are dimmed to highlight selected quiggle,
     * but bright enough to see how the selected quiggle looks in context compared
     * to other quiggles while the user makes changes (e.g. to adjust size to match another)
     */
    fun editSelectedQuiggleInContext() {
        selectedQuiggleEdited = true

        val period = 0.7
        resetQuigglePosition(selectedQuiggleChecked, period)
        for (quiggle in quiggles - selectedQuiggleChecked) {
            quiggle.setBrightness(0.5, period)
            resetQuigglePosition(quiggle, 0.0)
        }
    }

    fun touchUp(point: Point) {
        if (quiggles.isEmpty())
            return

        val quiggle = quiggles.last()

        // User tapped rather than drawing a proper quiggle
        if (!quiggle.isLongEnough()) {
            quiggles.remove(quiggle)

            when {
                // Tapped on invisible menu button in corner
                !activity.menuButton.visible
                        && point.x <= activity.dp(50f)
                        && point.y <= activity.dp(50f)
                ->
                    activity.onBackPressedDispatcher.onBackPressed()

                // Tried to select one or more quiggles from all of them
                selectedQuiggles.isEmpty() -> {
                    val d = point.distance(scenter)
                    val fullyVisible = nonTransitioning(includeCompleting = true).second
                    val buffer = activity.dp(25f)
                    selectMany(fullyVisible.filter {
                        val s = it.scaleAnimation.currentValue()
                        -buffer + it.innerRadius * s <= d && d <= s * it.outerRadius + buffer
                    })
                }

                // Tried to pick one quiggle from the current multi-selection
                // or tapped outside to exit selection mode
                selectedQuiggle == null -> {
                    val maybeQuiggle = selectedQuiggles.singleOrNull { it.isSelected(point) }
                    if (maybeQuiggle == null) {
                        selectNone()
                    } else {
                        selectOne(maybeQuiggle)
                    }
                }

                // A quiggle was already selected, exiting the selected mode
                else ->
                    unselectOne()
            }
        } else if (selectedQuiggle == null) {
            // Just finished drawing a quiggle
            quiggle.finishDrawing()

            // Many quiggles were selected - rearrange them in a new packing
            if (selectedQuiggles.isNotEmpty()) {
                selectMany(selectedQuiggles + quiggle)
            }
        } else {
            // A single quiggle was selected, exiting the selected mode
            // Nothing was drawn because the other methods return early
            unselectOne()
        }

        updateButtons()
    }

    fun touchCancel() {
        if (quiggles.isEmpty())
            return

        // This can happen e.g. when taking a screenshot
        // A quiggle may have started being drawn but touchUp is never called
        val quiggle = quiggles.last()
        if (quiggle.state == Quiggle.State.Drawing) {
            quiggles.remove(quiggle)
        }
    }

    private fun updateButtons() {
        with(activity as MainActivity) {
            editQuiggleButtons.visibility = if (selectedQuiggle != null) View.VISIBLE else View.INVISIBLE
            editCanvasButtons.visible = false
            seekBar.visible = false
            resetButtons()
            tutorial.maybeHide()
        }
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

        if (!SelectedOne.visited
            && tutorial != null
            && tutorial?.state != HiddenMenuButton
            && !activity.editCanvasButtons.visible
        ) {
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
            activity.finger?.visible = false
        }
    }

    private fun updateVisibility() {
        val (notTransitioning, fullyVisible, fullyInvisible) = nonTransitioning(includeCompleting = false)

        val diff = fullyVisible.size - maxQuiggles

        // This typically indicates that the user is moving the max quiggles slider
        // It causes a quiggle to be shown/hidden instantly, once for each of these method calls,
        // which is once per frame.
        // This lets the user easily see the effect of the slider.
        // Otherwise, at most one quiggle is becoming visible/invisible (often one of each) at a time,
        // and it's gradual.
        val quick = diff.absoluteValue > 1

        /**
         * Pick a random quiggle that has been visible/invisible for a while
         * and toggle its visibility.
         */
        fun switchOne(part: List<Quiggle>, visibility: Double) {
            part
                .sortedBySafe { it.visibilityAnimation.startTime }
                .take(ceil(part.size / 2.0).toInt())
                .shuffled()[0]
                .setVisibility(
                    visibility,
                    period = if (quick) 0.0 else 2.5
                )
        }

        fun hideOne() = switchOne(fullyVisible, 0.0)
        fun showOne() = switchOne(fullyInvisible, 1.0)

        // If too many quiggles are visible, hide one
        if (diff > 0) {
            hideOne()
        } else if (
        // Only start showing a quiggle if none are currently changing
        // or too many are invisible
            (notTransitioning.size == quiggles.size || quick)
            && fullyInvisible.isNotEmpty()
        ) {
            // Perfectly balanced, as all things should be
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
                oscillate()
            }
        }
    }
}

class NoSelectedQuiggle : RuntimeException()
