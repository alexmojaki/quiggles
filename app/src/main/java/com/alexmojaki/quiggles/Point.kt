package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Matrix

data class Point(val x: Double, val y: Double) : TwoComponents<Double, Double> {
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    val xf: Float get() = x.toFloat()
    val yf: Float get() = y.toFloat()

    fun direction(other: Point) =
        Math.atan2(other.y - y, other.x - x)

    fun distance(other: Point) =
        Math.hypot(other.y - y, other.x - x)

    fun pointInDirection(direction: Double, distance: Double) =
        Point(
            x + distance * Math.cos(direction),
            y + distance * Math.sin(direction)
        )

    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun times(f: Double) = Point(x * f, y * f)
    operator fun div(f: Double) = Point(x / f, y / f)

    fun translate(canvas: Canvas) = spreadF(canvas::translate)
    fun translate(matrix: Matrix) = spreadF(matrix::postTranslate)

    fun rotate(canvas: Canvas, radians: Double) = canvas.rotate(
        Math.toDegrees(radians).toFloat(),
        xf, yf
    )

    fun scale(canvas: Canvas, scale: Float) = canvas.scale(scale, scale, xf, yf)
    fun scale(matrix: Matrix, scale: Float) = matrix.postScale(scale, scale, xf, yf)

    fun toFloat() = FloatPoint(xf, yf)
}

operator fun Matrix.times(point: Point): Point = transform(point.toFloat()).toDouble()
