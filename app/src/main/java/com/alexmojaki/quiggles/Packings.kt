package com.alexmojaki.quiggles

import android.graphics.Matrix
import kotlin.math.sqrt

data class Packing(val centers: List<Point>) {
    val n = centers.size
    val minx = centers.asSequence().map { it.x }.min()!! - 1
    val maxx = centers.asSequence().map { it.x }.max()!! + 1
    val miny = centers.asSequence().map { it.y }.min()!! - 1
    val maxy = centers.asSequence().map { it.y }.max()!! + 1
    val boxCenter = Point((minx + maxx) / 2, (miny + maxy) / 2)
    val width = maxx - minx
    val height = maxy - miny

    fun rotate(angle: Int): Packing {
        val matrix = Matrix()
        matrix.setRotate(angle.toFloat())
        return Packing(centers.map(matrix::times))
    }

    companion object {
        val packings = HashMap<Int, MutableList<Packing>>()
        val indices = HashMap<Int, Int>()

        init {
            val grids = """
o

o
o

 o
o o

oo
oo

oo
oo * 45

 o
ooo * 45
 o

 o o
o   o  * 90
 o o

  o
 o o
o o o

oo
oo
oo

 o o
o o o  * 90
 o o

o o o
 o o   * 90
o o o

oo
oo
oo
oo

   o
  o o
 o   o
o o o o

  o
 o o
o o o
 o o
  o

ooo
ooo
ooo

ooo
ooo * 45
ooo

   o
  o o
 o o o
o o o o

 o o o
o o o o  * 90
 o o o

o o o o
 o o o   * 90
o o o o

   o
o o o o
 o   o   * 30
o o o o
   o

  o o
 o o o
o o o o
 o o o

ooo
ooo
ooo
ooo

   o
o o o o
 o o o   * 30
o o o o
   o

 o o o o
o o o o o * 90
 o o o o

  o o
 o o o
o o o o
 o o o
  o o
"""
            val regex = Regex("""\*\s*(\d+)""")
            var n = 1
            for (s in grids.trim().split(Regex("\n\\s*\n"))) {
                var packing = grid(regex.replace(s, ""))
                val matchResult = regex.find(s)
                if (matchResult != null) {
                    packing = packing.rotate(matchResult.groupValues[1].toInt())
                }
                require(packing.n.oneOf(n, n + 1))
                n = packing.n
                packings.getOrPut(n) { ArrayList() }.add(packing)
                indices.put(n, 0)
            }

        }
    }
}


fun grid(s: String): Packing {
    val lines = s.trimIndent().split('\n')
    val result = ArrayList<Point>()
    for ((row, line) in lines.withIndex()) {
        for ((col, c) in line.withIndex()) {
            if (c == 'o') {
                result.add(
                    if ("o o" in s)
                        Point(col.toDouble(), row * sqrt(3.0))
                    else Point(col * 2, row * 2)
                )
            }
        }
    }
    return Packing(result)
}

fun packing(n: Int): Packing {
    Packing.packings[n]?.let {
        val i = Packing.indices[n]!!
        Packing.indices[n] = i + 1
        return it[i % it.size]
    }

    val result = ArrayList<Point>()
    for (row in 0..n) {
        for (col in 0 until sqrt(n.toDouble()).toInt()) {
            result.add(Point(col * 2, row * 2))
            if (result.size == n) {
                return Packing(result)
            }
        }
    }

    throw Exception("that's not supposed to happen...")
}
