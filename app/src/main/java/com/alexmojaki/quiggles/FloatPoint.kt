package com.alexmojaki.quiggles

import android.graphics.Matrix

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
