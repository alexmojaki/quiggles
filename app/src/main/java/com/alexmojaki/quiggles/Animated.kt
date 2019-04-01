package com.alexmojaki.quiggles

import kotlin.math.absoluteValue
import kotlin.math.sign

class Animated<T>(
    val type: Class<T>,
    val startValue: T,
    val endValue: T,
    val period: Double,
    val easingFunction: (Double) -> Double
) {
    val startTime = clock.now()

    fun elapsedRatio() =
        if (period == 0.0)
            1.0
        else
            (clock.now() - startTime) / (period * 1000)

    fun easedRatio(): Double {
        if (period == 0.0) return 1.0
        val elapsed = elapsedRatio()

        // easingFunction only needs to make sense for positive inputs,
        // this will return symmetric values for negative inputs
        return easingFunction(elapsed.absoluteValue) * elapsed.sign
    }

    @Suppress("UNCHECKED_CAST")
    fun currentValue(): T {
        return when (type) {
            Double::class.java -> ((startValue as Double) + (endValue as Double - startValue) * easedRatio()) as T
            Point::class.java -> ((startValue as Point) + (endValue as Point - startValue) * easedRatio()) as T
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

inline fun <reified T> animated(
    startValue: T,
    endValue: T,
    period: Double,
    noinline easingFunction: (Double) -> Double = ::s2
) = Animated(
    T::class.java,
    startValue,
    endValue,
    period,
    easingFunction
)

inline fun <reified T> still(
    value: T,
    noinline easingFunction: (Double) -> Double = ::s2
): Animated<T> {
    return animated(value, value, 0.0, easingFunction)
}