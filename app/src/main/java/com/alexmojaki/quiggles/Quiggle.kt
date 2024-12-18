package com.alexmojaki.quiggles

import android.graphics.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.*
import kotlin.math.*

@Suppress("MemberVisibilityCanBePrivate")
@JsonIgnoreProperties(
    "drawing",
    "scenter",
    "state",
    "numPaths",
    "fullPath",
    "partialPath",
    "index",
    "numVertices",
    "centerAnimation",
    "scaleAnimation",
    "rotationAnimation",
    "brightnessAnimation",
    "visibilityAnimation",
    "hueAnimation",
    "hue",
    "outerRadius",
    "innerRadius",
    "center",
    "longEnough",
    "color"
)
open class Quiggle {
    enum class State { Drawing, Completing, Complete }

    lateinit var drawing: Drawing
    val scenter: Point
        get() = drawing.scenter

    var state = State.Drawing
    val points: MutableList<Point> = ArrayList()
    var numPaths = 0
    var fullPath = QuadraticPath()
    var partialPath = QuadraticPath()
    var index = 0
    var idealAngle = 0.0
    var numVertices = -1

    lateinit var centerAnimation: Animated<Point>
    lateinit var scaleAnimation: Animated<Double>
    lateinit var rotationAnimation: Animated<Double>
    var hueAnimation = still(0.0) { it % 1 }
    var brightnessAnimation = still(1.0, ::linear)
    var visibilityAnimation = still(1.0, ::linear)

    var oscillationPeriod: Double = Double.POSITIVE_INFINITY
    var rotationPeriod: Double = randRange(5f, 20f).toDouble()
    var huePeriod: Double = Double.POSITIVE_INFINITY
    var glowNormally = false

    lateinit var center: Point

    var usualScale = 1.0
    var thickness = 4f

    var outerRadius: Double = 0.0
    var innerRadius: Double = 0.0

    var baseHue = nextHue()

    var hue: Float
        get() = (baseHue + hueAnimation.currentValue().toFloat() + 3600) % 360
        set(value) {
            baseHue = value
            stopGlowing()
        }
    var saturation = 1f
    var colorValue = 1f

    var color: Int
        get() = Color.HSVToColor(floatArrayOf(hue, saturation, colorValue))
        set(value) {
            val arr = FloatArray(3)
            Color.colorToHSV(value, arr)
            hue = arr[0]
            saturation = arr[1]
            colorValue = arr[2]
        }

    init {
        hue = baseHue
    }

    fun restore() {
        setAngle(idealAngle)
        state = State.Complete
        numPaths = numVertices - 1
        fullPath = QuadraticPath.fromPoints(points)

        scaleAnimation = still(usualScale)
        oscillate()

        centerAnimation = still(scenter)

        glow()

        setBrightness(0.0, 0.0)
        setBrightness(1.0, 3.0)

        startRotation()
    }

    fun start(point: Point) {
        points.add(point)
        fullPath.start(point)
    }

    fun addPoint(point: Point) {
        if (state != State.Drawing) {
            return
        }

        if (point.distance(points.last()) >= 8) {
            fullPath.add(point)
            points.add(point)
        }
    }

    fun setAngle(angle: Double) {
        val (idealAngle, numVertices) = star(angle)
        this.idealAngle = idealAngle
        this.numVertices = numVertices

        val vertices = vertices()
        center = Point(
            vertices.asSequence().map { it.x }.average(),
            vertices.asSequence().map { it.y }.average()
        )

        val distances = points.asSequence().map { it.distance(center) }
        outerRadius = distances.max()
        innerRadius = distances.min()
    }

    fun finishDrawing() {
        if (state != State.Drawing) return

        fullPath.complete()
        state = State.Completing
        numPaths--
        update()

        setAngle(abs(points[points.size - 2].direction(points.last()) - points[0].direction(points[1])))

        startRotation()

        scaleDownToFit()

        scaleAnimation = still(1.0)

        if (scenter.y * 0.85 < outerRadius) {
            oscillationPeriod = randRange(4f, 12f).toDouble()
            oscillate()
        }

        centerAnimation = Animated(
            center,
            scenter,
            3.0
        )
    }

    private fun startRotation() {
        rotationAnimation = Animated(
            0.0,
            2 * PI,
            period = rotationPeriod,
            easingFunction = ::s2Line
        )
    }

    fun glow() {
        hueAnimation = hueAnimation.change(
            hueAnimation.currentValue() + 360,
            period = huePeriod
        )
    }

    fun glowRandomly() {
        huePeriod = randRange(5f, 20f).toDouble()
        glow()
    }

    fun stopGlowing() {
        hueAnimation = hueAnimation.change(0.0, 0.0)
        huePeriod = Double.POSITIVE_INFINITY
        glowNormally = false
    }

    fun oscillate() {
        if (usualScale <= 0.0) return
        val initial = scaleAnimation.currentValue()
        var lower = { 0.05 * scenter.y / outerRadius }
        if (usualScale < lower() * 2) {
            lower = { 0.0 }
        }
        scaleAnimation = scaleAnimation.change(
            lower(),
            oscillationPeriod,
            easingFunction = {
                /*
                cos for oscillation
                1 - ... to start at max value
                pow(..., 4.0) to spend more time at smaller scale than bigger
                PI / 2 * ... to normalise period
                First shrinkage starts from the current/initial size rather than the
                maximum size, we use initialRatio to compensate for that
                so that changing the oscillation speed feels natural and intuitive
                 */
                val initialRatio = initial / (usualScale - lower())
                if (it < initialRatio)
                    1 - cos(
                        PI / 2 *
                                // Speed up initial shrinkage
                                it / initialRatio
                    ).pow(4.0)
                else
                    1 - cos(
                        PI / 2 *
                                // Shift to match speed up of initial shrinkage
                                (it - initialRatio + 1)
                    ).pow(4.0) *
                            // Scale back up to max size (usualScale) instead of initial
                            usualScale / (initial - lower())
            }
        )
    }

    fun scaleDownToFit() {
        if (scenter.y < usualScale * outerRadius) {
            usualScale = randRange(0.85f, 1f) * scenter.y / outerRadius
        }
    }

    private fun vertices(): ArrayList<Point> {
        val vertices = ArrayList(listOf(points[0]))
        val dist = points[0].distance(points.last())
        val angle = points[0].direction(points.last())
        for (i in 1 until numVertices) {
            vertices.add(vertices.last().pointInDirection((i - 1) * idealAngle + angle, dist))
        }
        return vertices
    }

    fun setPosition(position: Point, scale: Double, period: Double) {
        finishDrawing()
        scaleAnimation = scaleAnimation.change(scale, period, easingFunction = ::s2)
        centerAnimation = centerAnimation.change(position, period)
    }

    fun setBrightness(brightness: Double, period: Double) {
        brightnessAnimation = brightnessAnimation.change(brightness, period)
    }

    fun setVisibility(visibility: Double, period: Double) {
        visibilityAnimation = visibilityAnimation.change(visibility, period)
    }

    fun isSelected(point: Point) =
        point.distance(centerAnimation.currentValue()) <= scaleAnimation.currentValue() * outerRadius

    open fun draw(canvas: Canvas, drawRings: Boolean = false) {
        val brightness = brightness().toFloat()
        if (brightness == 0f) {
            return
        }

        canvas.save()
        val paint = makePaint().apply {
            color = Color.HSVToColor(floatArrayOf(hue, saturation, colorValue * brightness))
            strokeWidth = thickness
            alpha = 0xff
        }

        val matrix = Matrix()

        if (state != State.Drawing && this !is TutorialQuiggle) {
            (centerAnimation.currentValue() - center).translate(canvas)
            center.scale(matrix, scaleAnimation.currentValue().toFloat())
            center.rotate(canvas, rotationAnimation.currentValue())
        }

        val p1 = points.first()
        val p2 = points.last()
        for (i in 0..numPaths) {
            canvas.drawPath(matrix * fullPath.path, paint)
            (p2 - p1).translate(canvas)
            p1.rotate(canvas, idealAngle)
        }
        canvas.drawPath(matrix * partialPath.path, paint)
        canvas.restore()

        if (drawRings && state != State.Drawing) {
            val scale = scaleAnimation.currentValue().toFloat()
            val c = centerAnimation.currentValue().toFloat()
            with(paint) {
                strokeWidth = (outerRadius - innerRadius).toFloat() * scale
                alpha = 0x20
            }
            canvas.drawCircle(
                c.x, c.y,
                ((outerRadius + innerRadius) / 2).toFloat() * scale,
                paint
            )
        }
    }

    fun brightness() = brightnessAnimation.currentValue() *
            visibilityAnimation.currentValue()

    open fun update() {
        if (state != State.Completing) return
        val p = points[index]
        if (index == 0) {
            numPaths++
            partialPath = QuadraticPath()
            partialPath.start(p)
            if (numPaths == numVertices - 1) {
                state = State.Complete
            }
        } else {
            partialPath.add(p)
        }
        index = (index + 1) % points.size
    }

    private fun copy() = jsonMapper.readValue<Quiggle>(jsonMapper.writeValueAsString(this))

    fun copyForGif(drawing: Drawing, duration: Double, scale: Double) =
        copy().apply {
            this.drawing = drawing
            val newPoints = points * scale
            points.clear()
            points.addAll(newPoints)
            setAngle(idealAngle)

            fun alignPeriod(period: Double, factor: Double) = if (period.isFinite()) {
                duration / (duration / (period * factor)).roundToInt() / factor
            } else {
                period
            }

            rotationPeriod = alignPeriod(rotationPeriod, 1.0 / numVertices)
            oscillationPeriod = alignPeriod(oscillationPeriod, 2.0)
            huePeriod = alignPeriod(huePeriod, 1.0)

            restore()
            setBrightness(1.0, 0.0)
            rotationAnimation = Animated(
                0.0,
                2 * PI,
                period = rotationPeriod,
                easingFunction = { it }
            )

            // Decrease thickness by scale, but not too much
            thickness = max(min(2f, thickness), thickness * scale.toFloat())
        }

    fun duplicate() =
        copy().apply {
            drawing = this@Quiggle.drawing
            usualScale *= 0.9
            restore()
            rotationAnimation = this@Quiggle.rotationAnimation
        }

    fun isLongEnough() = points.size >= 5
}