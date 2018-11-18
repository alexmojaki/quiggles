package com.alexmojaki.quiggles

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
