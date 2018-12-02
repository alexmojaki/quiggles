package com.alexmojaki.quiggles

import android.graphics.Matrix
import android.graphics.Path

interface TwoComponents<C1, C2> {
    fun component1(): C1
    fun component2(): C2

    fun <R> spread(f: (C1, C2) -> R) = f(component1(), component2())
}

fun <R> TwoComponents<Float, Float>.spreadD(f: (Double, Double) -> R): R =
    f(
        component1().toDouble(),
        component2().toDouble()
    )


fun <R> TwoComponents<Double, Double>.spreadF(f: (Float, Float) -> R): R =
    f(
        component1().toFloat(),
        component2().toFloat()
    )

fun Any.oneOf(vararg vals: Any): Boolean {
    return vals.any { this == it }
}

operator fun Matrix.times(path: Path): Path {
    val result = Path(path)
    result.transform(this)
    return result
}

fun <T> prn(x: T): T {
    println(x)
    return x
}

fun <T> prn(label: String, x: T): T {
    println("$label: $x")
    return x
}