package com.alexmojaki.quiggles

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.waynejo.androidndkgif.GifEncoder
import kotlinx.android.synthetic.main.activity_gif.*
import pl.droidsonroids.gif.GifDrawable
import java.io.OutputStream
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt


private const val TAG = "GifActivity"

class GifActivity : CommonActivity() {

    private var cancelled = false

    override fun onCreate() {
        Log.i(TAG, "Starting")
        setContentView(R.layout.activity_gif)

        val fps = 24
        val delay = 1000 / fps
        clock = ControlledClock(delay)

        if (gifDrawing == null) {
            // This apparently happens based on a crash report
            finish()
        }

        // The drawing is scaled down to reduce file size and encoding time
        // It should be at least twice as small as before,
        // and the width should be at most 1000 pixels
        val scale = min(0.5, 1000 / (gifDrawing!!.scenter.x * 2))

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
            .max()
            .toNearest(delay / 1000.0)

        // Check if this GIF has been made before
        val hash =
            sha256(quiggles.map { jsonMapper.writeValueAsString(it) }.sorted().joinToString())
        val cachedUriString = sharedPreferences.getString(hash, null)
        if (cachedUriString != null) {
            val cachedUri = Uri.parse(cachedUriString)
            if (canReadUri(cachedUri)) {
                complete(cachedUri)
                return
            }
        }

        drawing.quiggles.addAll(quiggles.map { it.copyForGif(drawing, duration, scale) })

        val (scaledWidth, scaledHeight) = (scenter * 2.0).toInt()

        val frames = (duration / delay * 1000).roundToInt()
        gifProgress.max = frames
        gifProgress.progress = 0

        prn("delay", delay)
        prn("duration", duration)
        prn("frames", frames)

        Log.i(TAG, "Preparing encoding")
        val gifEncoder = GifEncoder()
        val tempFile = filesDir / "gif"
        gifEncoder.init(
            scaledWidth, scaledHeight,
            tempFile.absolutePath, GifEncoder.EncodingType.ENCODING_TYPE_SIMPLE_FAST
        )

        val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        Thread {
            try {
                for (i in 1..frames) {
                    if (cancelled) break

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

                if (cancelled) return@Thread

                gifEncoder.close()
                val displayName =
                    "${gifDrawing!!.filename ?: "untitled"} ${isoFormat(currentTime())}"
                        .replace(Regex("""["*/:<>?\\|]"""), "_")

                fun copyFileToStream(outputStream: OutputStream) {
                    outputStream.use {
                        tempFile.inputStream().use {
                            it.copyTo(outputStream)
                        }
                    }
                }

                val uri = if (newStorageMethod) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            throw Error("newStorageMethod is wrong")
                        }
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Quiggles")
                    }

                    val resolver = applicationContext.contentResolver
                    val uri = resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )!!

                    copyFileToStream(resolver.openOutputStream(uri)!!)
                    uri
                } else {
                    val outFile = picsDir() / "$displayName.gif"
                    copyFileToStream(outFile.outputStream())
                    FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.provider",
                        outFile
                    )
                }

                sharedPreferences.edit {
                    putString(hash, uri.toString())
                }
                complete(uri)
            } finally {
                try {
                    tempFile.delete()
                } catch (_: Throwable) {
                }
            }

        }.start()
    }

    private fun canReadUri(uri: Uri): Boolean {
        try {
            applicationContext.contentResolver.openInputStream(uri)!!.close()
        } catch (e: Throwable) {
            return false
        }
        return true
    }

    private fun complete(uri: Uri) {
        val gifDrawable = GifDrawable(applicationContext.contentResolver, uri)
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
        cancelled = true
        clock = SystemClock()
    }

}

var gifDrawing: Drawing? = null
