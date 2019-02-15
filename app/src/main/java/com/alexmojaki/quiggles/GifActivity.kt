package com.alexmojaki.quiggles

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View.INVISIBLE
import com.waynejo.androidndkgif.GifEncoder
import kotlinx.android.synthetic.main.activity_gif.*
import pl.droidsonroids.gif.GifDrawable
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


class GifActivity : CommonActivity() {

    override fun onCreate() {
        setContentView(R.layout.activity_gif)

        val fps = 24
        val delay = 1000 / fps
        clock = ControlledClock(delay)

        val scale = Math.min(0.5, 1000 / (gifDrawing!!.scenter.x * 2))
        val scenter = gifDrawing!!.scenter * scale
        val drawing = Drawing(scenter)

        val quiggles = gifDrawing!!.nonTransitioning(includeCompleting = true).second
        val duration = (quiggles.map { it.oscillationPeriod * 2 } +
                quiggles.map { it.rotationPeriod.absoluteValue / it.numVertices })
            .filter { it.isFinite() }
            .max()!!
            .toNearest(delay / 1000.0)

        drawing.quiggles.addAll(quiggles.map { it.copyForGif(scenter, duration, scale) })

        val (scaledWidth, scaledHeight) = (scenter * 2.0).toInt()
        println(metrics.widthPixels)
        println(scaledWidth)

        val frames = (fps * duration).roundToInt()
        gifProgress.max = frames
        gifProgress.progress = 0

        val gifEncoder = GifEncoder()
        val path = (picsDir() /
                "${gifDrawing!!.filename ?: "untitled"} ${isoFormat(currentTime())}.gif"
                    .replace(Regex("""["*/:<>?\\|]"""), "_")
                ).absolutePath
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 99)
            finish()
            return
        }
        gifEncoder.init(
            scaledWidth, scaledHeight,
            path, GifEncoder.EncodingType.ENCODING_TYPE_SIMPLE_FAST
        )

        val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 99 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            // TODO
        }
    }

}

var gifDrawing: Drawing? = null
