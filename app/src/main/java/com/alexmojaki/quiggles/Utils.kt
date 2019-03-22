package com.alexmojaki.quiggles

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

interface TwoComponents<C1, C2> {
    fun component1(): C1
    fun component2(): C2

    fun <R> spread(f: (C1, C2) -> R) = f(component1(), component2())
}

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

@Suppress("unused")
fun <T> prn(x: T): T {
    println(x)
    return x
}

@Suppress("unused")
fun <T> prn(label: String, x: T): T {
    println("$label: $x")
    return x
}

val jsonMapper = jacksonObjectMapper()
    .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)!!

inline fun <reified T> fileToJson(file: File): T {
    FileInputStream(file).use {
        return jsonMapper.readValue(it)
    }
}

operator fun File.div(name: String) = File(this, name)

fun currentTime(): Date = Calendar.getInstance().time

@SuppressLint("SimpleDateFormat")
fun isoFormat(dt: Date) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dt)!!

fun time(block: () -> Unit) {
    val start = System.currentTimeMillis()
    block()
    val end = System.currentTimeMillis()
    println("Time taken = ${end - start}")
}

fun makePaint() = Paint().apply {
    isAntiAlias = true
    isDither = true
    style = Paint.Style.STROKE
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
    xfermode = null
}

fun sha256(s: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(s.toByteArray())
        .joinToString("") { "%02x".format(it) }

fun <T> Iterable<T>.forEachApply(action: T.() -> Unit) {
    for (element in this) action(element)
}

var View.visible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.INVISIBLE
    }

fun SharedPreferences.edit(block: SharedPreferences.Editor.() -> Unit) {
    with(edit()) {
        block()
        apply()
    }
}
