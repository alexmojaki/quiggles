package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Color
import kotlin.math.min

class StarField(private val scenter: Point) {
    private var points = (0..130).map { makeStar() }
    private val paint = makePaint().apply {
        color = Color.WHITE
        strokeWidth = 4f
    }

    private fun makeStar() = Star(
        Point(
            randRange(0f, scenter.xf * 2),
            randRange(0f, scenter.yf * 2)
        ),
        1
    )

    fun update() {
        points = points.map {
            val p = it.point
            if (!(0 < p.x && p.x < scenter.x * 2 &&
                        0 < p.y && p.y < scenter.y * 2)
            ) {
                makeStar()
            } else {
                Star(
                    p.pointInDirection(scenter.direction(p), 10.0),
                    it.age + 1
                )
            }
        }
    }

    fun draw(canvas: Canvas) {
        points.forEach {
            paint.alpha = min(0xff, it.age * 16)
            val p = it.point
            canvas.drawPoint(p.xf, p.yf, paint)
        }
    }

}

data class Star(val point: Point, val age: Int)