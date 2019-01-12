package com.alexmojaki.quiggles

class Animated<T>(
    val type: String,
    val startValue: T,
    val endValue: T,
    val period: Double,
    val easingFunction: (Double) -> Double = ::s2
) {
    val startTime = System.currentTimeMillis()
    fun elapsedRatio() = if (period == 0.0) 1.0 else (System.currentTimeMillis() - startTime) / (period * 1000)
    fun easedRatio() = easingFunction(elapsedRatio())

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