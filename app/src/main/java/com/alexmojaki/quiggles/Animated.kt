package com.alexmojaki.quiggles

class Animated<T>(
    val type: String,
    val startValue: T,
    val endValue: T,
    val period: Double,
    val easingFunction: (Double) -> Double = ::s2
) {
    val startTime = System.currentTimeMillis()
    fun elapsedRatio() = (System.currentTimeMillis() - startTime) / (period * 1000)
    fun easedRatio() = easingFunction(elapsedRatio())

    @Suppress("UNCHECKED_CAST")
    fun currentValue(): T {
        return when (type) {
            "double" -> ((startValue as Double) + (endValue as Double - startValue) * easedRatio()) as T
            "point" -> ((startValue as Point) + (endValue as Point - startValue) * easedRatio()) as T
            else -> throw IllegalArgumentException()
        }
    }

}
