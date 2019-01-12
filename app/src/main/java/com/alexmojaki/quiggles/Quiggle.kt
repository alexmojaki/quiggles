package com.alexmojaki.quiggles

import android.graphics.*
import java.util.*
import kotlin.math.PI

class Quiggle {
    enum class State { Drawing, Completing, Complete }

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
    var brightnessAnimation: Animated<Double> = still("double", 1.0, ::linear)
    var visibilityAnimation: Animated<Double> = still("double", 1.0, ::linear)

    lateinit var center: Point

    var usualScale = 1.0
    var thickness = 4f

    var outerRadius: Double = 0.0
    var innerRadius: Double = 0.0
    var hue = nextHue().toFloat()
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

    fun start(point: Point) {
        points.add(point)
        fullPath.start(point)
    }

    fun addPoint(point: Point) {
        require(state == State.Drawing)
        if (point.distance(points.last()) >= Drawing.TOUCH_TOLERANCE) {
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
        outerRadius = distances.max()!!
        innerRadius = distances.min()!!
    }

    fun finishDrawing(swidth: Int, sheight: Int) {
        fullPath.complete()
        state = State.Completing
        numPaths--
        update()

        setAngle(Math.abs(points[points.size - 2].direction(points.last()) - points[0].direction(points[1])))

        rotationAnimation = Animated(
            "double",
            0.0,
            2 * PI,
            period = randRange(5f, 20f).toDouble(),
            easingFunction = ::s2Line
        )

        if (sheight / 2 < outerRadius) {
            usualScale = randRange(0.85f, 1f) * sheight / 2 / outerRadius
        }

        scaleAnimation = Animated(
            "double",
            1.0,
            usualScale,
            3.0
        )

        centerAnimation = Animated(
            "point",
            center,
            Point(swidth / 2, sheight / 2),
            3.0
        )
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
        scaleAnimation = scaleAnimation.change(scale, period)
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

    fun draw(canvas: Canvas) {
        val brightness = brightness().toFloat()
        if (brightness == 0f) {
            return
        }

        canvas.save()
        val paint = Paint()

        with(paint) {
            isAntiAlias = true
            isDither = true
            color = Color.HSVToColor(floatArrayOf(hue, saturation, colorValue * brightness))
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = thickness
            xfermode = null
            alpha = 0xff
        }

        val matrix = Matrix()

        if (state != Quiggle.State.Drawing) {
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
    }

    fun brightness() = brightnessAnimation.currentValue() *
            visibilityAnimation.currentValue()

    fun update() {
        if (state != State.Completing) return
        val p = points[index]
        if (index == 0) {
            numPaths++
            partialPath = QuadraticPath()
            partialPath.start(p)
            if (numPaths == numVertices) {
                state = State.Complete
            }
        } else {
            partialPath.add(p)
        }
        index = (index + 1) % points.size
    }
}