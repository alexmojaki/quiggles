package com.alexmojaki.quiggles

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.support.v4.content.FileProvider
import com.waynejo.androidndkgif.GifEncoder
import kotlinx.android.synthetic.main.activity_gif.*
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


class GifActivity : CommonActivity() {

    override fun onCreate() {
        setContentView(R.layout.activity_gif)

        val fps = 24
        val delay = 1000 / fps
        clock = ControlledClock(delay)

        // The drawing is scaled down to reduce file size and encoding time
        // It should be at least twice as small as before,
        // and the width should be at most 1000 pixels
        val scale = Math.min(0.5, 1000 / (gifDrawing!!.scenter.x * 2))

        val scenter = gifDrawing!!.scenter * scale
        val drawing = Drawing(scenter)

        // Only include quiggles which are currently fully visible
        // There will be no visibility changes in the GIF
        val quiggles = gifDrawing!!.nonTransitioning(includeCompleting = true).second

        // Find the longest effective period for a perfect loop
        val duration = (
                quiggles.map { it.oscillationPeriod * 2 } +
                        quiggles.map { it.huePeriod } +
                        quiggles.map { it.rotationPeriod / it.numVertices })
            .map { it.absoluteValue }
            .filter { it.isFinite() }
            .max()!!
            .toNearest(delay / 1000.0)

        // Check if this GIF has been made before
        val hash = sha256(quiggles.map { jsonMapper.writeValueAsString(it) }.sorted().joinToString())
        val cachedPath = sharedPreferences.getString(hash, null)
        if (cachedPath != null && File(cachedPath).exists()) {
            complete(cachedPath)
            return
        }

        drawing.quiggles.addAll(quiggles.map { it.copyForGif(drawing, duration, scale) })

        val (scaledWidth, scaledHeight) = (scenter * 2.0).toInt()

        val frames = (duration / delay * 1000).roundToInt()
        gifProgress.max = frames
        gifProgress.progress = 0

        prn("delay", delay)
        prn("duration", duration)
        prn("frames", frames)

        val gifEncoder = GifEncoder()
        val path = (picsDir() /
                "${gifDrawing!!.filename ?: "untitled"} ${isoFormat(currentTime())}.gif"
                    .replace(Regex("""["*/:<>?\\|]"""), "_")
                ).absolutePath

        gifEncoder.init(
            scaledWidth, scaledHeight,
            path, GifEncoder.EncodingType.ENCODING_TYPE_SIMPLE_FAST
        )

        val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
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

                // Shows each frame in ImageView preview
//                    val copy = Bitmap.createBitmap(bitmap)
//                    runOnUiThread { gifPreview.setImageBitmap(copy) }
            }
            gifEncoder.close()
            sharedPreferences.edit {
                putString(hash, path)
            }

            complete(path)

        }.start()
    }

    private fun complete(path: String) {
        val gifDrawable = GifDrawable(path)
        runOnUiThread {
            gifPreview.setImageDrawable(gifDrawable)
            gifPreview.visible = true
            gifProgress.visible = false
            buttons.visible = true

            addButton(
                "Share",
                R.drawable.share_variant,
                buttonsLayout
            ) {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    File(path)
                )
                share("Share GIF") {
                    type = "image/gif"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        clock = SystemClock()
    }

}

var gifDrawing: Drawing? = null
