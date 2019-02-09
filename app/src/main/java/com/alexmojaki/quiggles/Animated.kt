package com.alexmojaki.quiggles

import kotlin.math.absoluteValue
import kotlin.math.sign

class Animated<T>(
    val type: String,
    val startValue: T,
    val endValue: T,
    val period: Double,
    val easingFunction: (Double) -> Double = ::s2
) {
    val startTime = clock.now()
    fun elapsedRatio() = if (period == 0.0) 1.0 else (clock.now() - startTime) / (period * 1000)
    fun easedRatio(): Double {
        val elapsed = elapsedRatio()
        return easingFunction(elapsed.absoluteValue) * elapsed.sign
    }

    @Suppress("UNCHECKED_CAST")
    fun currentValue(): T {
        return when (type) {
            "double" -> ((startValue as Double) + (endValue as Double - startValue) * easedRatio()) as T
            "point" -> ((startValue as Point) + (endValue as Point - startValue) * easedRatio()) as T
            else -> throw IllegalArgumentException()
        }
    }

    fun change(
        endValue: T,
        period: Double,
        easingFunction: ((Double) -> Double)? = null
    ): Animated<T> {
        return Animated(
            type,
            currentValue(),
            endValue,
            period,
            easingFunction ?: this.easingFunction
        )
    }

}

fun <T> still(
    type: String,
    value: T,
    easingFunction: (Double) -> Double = ::s2
): Animated<T> {
    return Animated(type, value, value, 1.0, easingFunction)
}