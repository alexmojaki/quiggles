package com.alexmojaki.quiggles

import java.util.*
import kotlin.math.*

val angleToPoints = TreeMap<Double, Int>().apply {
    for (points in 4..9) {
        for (revolutions in 1 until points) {
            if (gcd(points, revolutions) == 1) {
                val angle = 2 * PI * revolutions / points
                this[angle] = points
            }
        }
    }
}

fun star(angle: Double): Pair<Double, Int> {
    val bestAngle = if (angle in angleToPoints) {
        angle
    } else {
        val lo = angleToPoints.lowerKey(angle)
        val hi = angleToPoints.higherKey(angle)
        listOf(lo, hi).minBy { Math.abs(angle - (it ?: 1000.0)) }!!
    }
    return Pair(bestAngle, angleToPoints[bestAngle]!!)
}

tailrec fun gcd(a: Int, b: Int): Int =
    if (b == 0) a else gcd(b, a % b)

val rand = Random()

fun randRange(min: Float, max: Float) = min + rand.nextFloat() * (max - min)

const val tau = 2 * PI

fun linear(x: Double) = Math.min(1.0, x)

fun s2(x: Double) = Math.min(1.0, x - Math.sin(tau * x) / tau)

fun s2Line(x: Double) = if (x < 0.25) s2(x) else x - 0.25 + s2(0.25)

var hue = rand.nextDouble()
val phi = (1 + sqrt(5.0)) / 2
val phiInv = 1 / phi

fun nextHue(): Float {
    hue += phiInv
    hue %= 1
    return (hue * 360).toFloat()
}

fun square(x: Double) = x * x

fun stretchProgress(x: Int) = 100 * (1 - sqrt(1 - square(x / 100.0))) * x.sign

fun unstretchProgress(x: Double) = (x.sign * (100 * sqrt(1 - square(x.absoluteValue / 100 - 1)))).roundToInt()

fun Double.toNearest(x: Double) = (this / x).roundToInt() * x
