package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Matrix
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlin.math.*

@JsonIgnoreProperties(
    "xf",
    "yf",
    "xi",
    "yi"
)
data class Point(val x: Double, val y: Double) {
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    val xf: Float get() = x.toFloat()
    val yf: Float get() = y.toFloat()
    private val xi: Int get() = x.roundToInt()
    private val yi: Int get() = y.roundToInt()

    fun direction(other: Point) =
        atan2(other.y - y, other.x - x)

    fun distance(other: Point) =
        hypot(other.y - y, other.x - x)

    fun pointInDirection(direction: Double, distance: Double) =
        Point(
            x + distance * cos(direction),
            y + distance * sin(direction)
        )

    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun times(f: Double) = Point(x * f, y * f)
    operator fun div(f: Double) = Point(x / f, y / f)

    fun translate(canvas: Canvas) = canvas.translate(xf, yf)
    fun translate(matrix: Matrix) = matrix.postTranslate(xf, yf)

    fun rotate(canvas: Canvas, radians: Double) = canvas.rotate(
        Math.toDegrees(radians).toFloat(),
        xf, yf
    )

    fun scale(matrix: Matrix, scale: Float) = matrix.postScale(scale, scale, xf, yf)

    fun toFloat() = FloatPoint(xf, yf)
    fun toInt() = IntPoint(xi, yi)
}

operator fun Matrix.times(point: Point): Point = transform(point.toFloat()).toDouble()

operator fun List<Point>.times(scale: Double):  List<Point> = map { it * scale }

data class FloatPoint(val x: Float, val y: Float) {
    constructor(arr: FloatArray) : this(arr[0], arr[1])

    fun toDouble() = Point(x, y)

    fun toArray() = floatArrayOf(x, y)
}

fun Matrix.transform(point: FloatPoint): FloatPoint {
    val array = point.toArray()
    mapPoints(array)
    return FloatPoint(array)
}

data class IntPoint(val x: Int, val y: Int)
