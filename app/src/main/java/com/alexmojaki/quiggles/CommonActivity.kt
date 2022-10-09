package com.alexmojaki.quiggles

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.text.InputType
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.android.instantapps.InstantApps
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.reflect.KClass

val newStorageMethod = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

abstract class CommonActivity : AppCompatActivity() {

    val metrics = DisplayMetrics()

    var permissionCallback: (() -> Unit)? = null

    val sharedPreferences
        get() = getPreferences(Context.MODE_PRIVATE)!!

    val isInstant by lazy { InstantApps.isInstantApp(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        windowManager.defaultDisplay.getMetrics(metrics)

        onCreate()
    }

    fun dp(x: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, x, metrics).toInt()

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    abstract fun onCreate()

    fun addButton(
        label: String,
        imageId: Int,
        viewGroup: ViewGroup,
        onClick: (View) -> Unit
    ): ImageButton {
        val buttonLayout = layoutInflater.inflate(
            R.layout.button_layout,
            viewGroup,
            false
        )
        buttonLayout.findViewById<TextView>(R.id.label).text = label
        val button = buttonLayout.findViewById<ImageButton>(R.id.button).apply {
            setImageResource(imageId)
            setOnClickListener(onClick)
        }
        viewGroup.addView(buttonLayout)
        return button
    }

    fun withWritePermission(action: () -> Unit) {
        if (hasWritePermission()) {
            action()
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            99
        )
        permissionCallback = action

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 99 &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            permissionCallback!!()
        }
    }

    fun saveFilename(filename: String) = saveFileDir() / filename

    private fun quigglesDir(dir: File) = (dir / "Quiggles").apply { mkdir() }

    fun saveFileDir() = quigglesDir(filesDir)

    fun picsDir() = quigglesDir(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    )

    fun unsavedFile(): File = filesDir / "unsaved"

    inline fun <reified T> jsonToFile(file: File, value: T) {
        try {
            FileOutputStream(file).use {
                jsonMapper.writeValue(it, value)
            }
        } catch (e: IOException) {
            toast("Error saving file: ${e.message}", Toast.LENGTH_LONG)
        }
    }

    fun load(filename: String, drawing: Drawing) {
        fileToJson<SaveFile>(saveFilename(filename)).restore(drawing)
        drawing.filename = filename
    }

    /** Save a drawing that has a filename */
    fun saveWithName(drawing: Drawing) {
        val saveFile = SaveFileV1(drawing)
        jsonToFile(saveFilename(drawing.filename!!), saveFile)
    }

    /**
     * Show dialog to choose a name to save a drawing.
     * Execute callback after saving.
     * */
    fun saveAsDialog(drawing: Drawing, callback: () -> Unit = {}) {
        dialog {
            setTitle("Choose save name")
            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_TEXT
            setView(input)
            setPositiveButton("OK") { _, _ ->
                val filename = input.text.toString().trim()
                fun doSave() {
                    drawing.filename = filename
                    saveWithName(drawing)
                    callback()
                }

                when {
                    filename.isEmpty() -> toast("Empty filename not allowed")
                    saveFilename(filename).exists() -> dialog {
                        setTitle("Filename already exists. Replace?")
                        setPositiveButton("Yes") { _, _ -> doSave() }
                        setNegativeButton("No") { _, _ -> }
                    }
                    else -> doSave()
                }
            }
            setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        }
    }

    fun toast(text: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, text, duration).show()
    }

    fun dialog(block: AlertDialog.Builder.() -> Unit) {
        AlertDialog.Builder(this).apply {
            block()
            show()
        }
    }

    fun intent(cls: KClass<*>) = Intent(this, cls.java)

    fun hasWritePermission() = newStorageMethod || ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

    fun share(title: String, intentBlock: Intent.() -> Unit) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply(intentBlock),
                title
            )
        )
    }

    fun shareApp() = share("Share the app") {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_SUBJECT,
            "Quiggles app"
        )
        putExtra(
            Intent.EXTRA_TEXT,
            "Try out the Quiggles app from the Play Store! " +
                    playStoreHttpLink
        )

    }

    fun rateApp() {
        try {
            viewUrl(playStoreDirectLink)
        } catch (e: ActivityNotFoundException) {
            openInBrowser(playStoreHttpLink)
        }
    }

    fun openInBrowser(url: String) {
        try {
            viewUrl(url)
        } catch (e: ActivityNotFoundException) {
            toast("No browser found")
        }
    }

    private fun viewUrl(url: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }
}

const val playStoreHttpLink =
    "https://play.google.com/store/apps/details?id=" +
            BuildConfig.APPLICATION_ID

const val playStoreDirectLink =
    "market://details?id=" +
            BuildConfig.APPLICATION_ID
