package com.alexmojaki.quiggles

import android.util.DisplayMetrics
import kotlin.math.min

data class Packing(val centers: List<Point>) {
    val minx = centers.asSequence().map { it.x }.min()!! - 1
    val maxx = centers.asSequence().map { it.x }.max()!! + 1
    val miny = centers.asSequence().map { it.y }.min()!! - 1
    val maxy = centers.asSequence().map { it.y }.max()!! + 1
    val boxCenter = Point((minx + maxx) / 2, (miny + maxy) / 2)
    val width = maxx - minx
    val height = maxy - miny

    fun scale(metrics: DisplayMetrics) = min(
        metrics.widthPixels / width.toFloat(),
        metrics.heightPixels / height.toFloat()
    )
}
