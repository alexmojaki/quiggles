package com.alexmojaki.quiggles

import android.graphics.Canvas

data class Point(val x: Double, val y: Double) : TwoComponents<Double, Double> {
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
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

    fun translate(canvas: Canvas) = spreadF(canvas::translate)
    fun rotate(canvas: Canvas, radians: Double) = canvas.rotate(
        Math.toDegrees(radians).toFloat(),
        x.toFloat(),
        y.toFloat())

    fun toFloat() = FloatPoint(x.toFloat(), y.toFloat())

}