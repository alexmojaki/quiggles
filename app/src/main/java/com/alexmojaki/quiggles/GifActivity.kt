package com.alexmojaki.quiggles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View.INVISIBLE
import com.waynejo.androidndkgif.GifEncoder
import kotlinx.android.synthetic.main.activity_gif.*
import pl.droidsonroids.gif.GifDrawable
import kotlin.math.absoluteValue


class GifActivity : CommonActivity() {

    override fun onCreate() {
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
        val path = (rootDir() / "video.gif").absolutePath
        gifEncoder.init(
            width, height,
            path, GifEncoder.EncodingType.ENCODING_TYPE_SIMPLE_FAST
        )

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        Thread {
            for (i in 0..frames) {
                drawing.draw(canvas)
                clock.tick()
                gifEncoder.encodeFrame(
                    bitmap,
                    delay
                )
                gifProgress.progress = i

                // Shows each frame in ImageView preview
//                    val copy = Bitmap.createBitmap(bitmap)
//                    runOnUiThread { gifPreview.setImageBitmap(copy) }
            }
            gifEncoder.close()

            val gifDrawable = GifDrawable(path)
            runOnUiThread {
                gifPreview.setImageDrawable(gifDrawable)
                gifProgress.visibility = INVISIBLE
            }

        }.start()
    }

    override fun finish() {
        super.finish()
        clock = SystemClock()
    }

}

var gifDrawing: Drawing? = null
