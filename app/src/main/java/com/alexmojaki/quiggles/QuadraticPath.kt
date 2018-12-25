package com.alexmojaki.quiggles

import android.graphics.Path

class QuadraticPath {

    lateinit var previous: Point
    val path = Path()

    fun start(point: Point) {
        previous = point
        path.moveTo(previous.xf, previous.yf)
    }

    fun add(point: Point) {
        val mid = (previous + point) / 2.0
        path.quadTo(previous.xf, previous.yf, mid.xf, mid.yf)
        previous = point
    }

    fun complete() {
        path.lineTo(previous.xf, previous.yf)
    }

}
