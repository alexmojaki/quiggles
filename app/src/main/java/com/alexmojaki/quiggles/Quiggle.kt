package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import java.util.*

class Quiggle {
    enum class State { Drawing, Completing, Complete }

    var state = State.Drawing
    val points: MutableList<Point> = ArrayList()
    var numPaths = 0
    var fullPath = Path()
    var partialPath = Path()
    var index = 0
    var idealAngle = 0.0
    var numVertices = -1
    val paint: Paint = Paint()

    init {
        with(paint) {
            isAntiAlias = true
            isDither = true
            color = Color.HSVToColor(listOf(360.0f * Random().nextFloat(), 1f, 1f).toFloatArray())
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 4f
            xfermode = null
            alpha = 0xff
        }
    }

    fun start(x: Float, y: Float) {
        points.add(Point(x, y))
        fullPath.moveTo(x, y)
    }

    fun addPoint(x: Float, y: Float) {
        val p = points.last().toFloat()

        val dx = Math.abs(x - p.x)
        val dy = Math.abs(y - p.y)

        if (dx >= Drawing.TOUCH_TOLERANCE || dy >= Drawing.TOUCH_TOLERANCE) {
            fullPath.quadTo(p.x, p.y, (x + p.x) / 2, (y + p.y) / 2)
            points.add(Point(x, y))
        }
    }

    fun finishDrawing() {
        val p = points.last().toFloat()
        fullPath.lineTo(p.x, p.y)
        state = State.Completing
        numPaths--
        update()

        val angle = Math.abs(points[points.size - 2].direction(points.last()) - points[0].direction(points[1]))
        val (idealAngle, numVertices) = star(angle)
        this.idealAngle = idealAngle
        this.numVertices = numVertices
    }

    fun center(): Point {
        val vertices = vertices()

        return Point(
            vertices.asSequence().map { it.x }.average(),
            vertices.asSequence().map { it.y }.average()
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


    fun draw(canvas: Canvas) {
        canvas.save()
        val p1 = points.first()
        val p2 = points.last()
        for (i in 0..numPaths) {
            canvas.drawPath(fullPath, paint)
            (p2 - p1).translate(canvas)
            p1.rotate(canvas, idealAngle)
        }
        canvas.drawPath(partialPath, paint)

        canvas.restore()
    }

    fun update() {
        if (state != State.Completing) return
        val p = points[index].toFloat()
        if (index == 0) {
            numPaths++
            partialPath.reset()
            partialPath.moveTo(p.x, p.y)
            if (numPaths == numVertices) {
                state = State.Complete
            }
        } else {
            partialPath.lineTo(p.x, p.y)
        }
        index = (index + 1) % points.size
    }
}