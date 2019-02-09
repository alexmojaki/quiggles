package com.alexmojaki.quiggles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Window
import com.waynejo.androidndkgif.GifEncoder
import kotlinx.android.synthetic.main.activity_gif.*
import kotlin.math.absoluteValue


class GifActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_gif)

        val fps = 30
        val delay = 1000 / fps
        clock = ControlledClock(delay)

        val scenter = gifDrawing!!.scenter
        val drawing = Drawing(scenter)
        val quiggles = gifDrawing!!.nonTransitioning(includeCompleting = false).second
        val duration = (quiggles.map { it.oscillationPeriod * 2 } +
                quiggles.map { it.rotationPeriod.absoluteValue / it.numVertices })
            .filter { it.isFinite() }
            .max()!!
            .toNearest(delay / 1000.0)

        drawing.quiggles.addAll(quiggles.map { it.copyForGif(scenter, duration) })

        val width = (drawing.scenter.x * 2).toInt()
        val height = (drawing.scenter.y * 2).toInt()

        val frames = (fps * duration).toInt()
        gifProgress.max = frames
        gifProgress.progress = 0

        val gifEncoder = GifEncoder()
        gifEncoder.init(
            width, height,
            (rootDir() / "video.gif").absolutePath, GifEncoder.EncodingType.ENCODING_TYPE_SIMPLE_FAST
        )

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        Thread {
            for (i in 1..frames) {
                drawing.draw(canvas)
                clock.tick()
                gifEncoder.encodeFrame(
                    bitmap,
                    delay
                )
                gifProgress.progress = i
            }
            gifEncoder.close()
            finish()
        }.start()
    }

    override fun finish() {
        super.finish()
        clock = SystemClock()
    }

}

var gifDrawing: Drawing? = null
