package com.alexmojaki.quiggles

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
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
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

abstract class CommonActivity : AppCompatActivity() {

    val metrics = DisplayMetrics()
    var permissionCallback: (() -> Unit)? = null

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
        val buttonLayout = layoutInflater.inflate(R.layout.button_layout, buttonsLayout, false)
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
        if (requestCode == 99 &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            permissionCallback!!()
        }
    }

    fun saveFilename(filename: String) = saveFileDir() / filename

    private fun quigglesDir(dir: File) = (dir / "Quiggles").apply { mkdir() }

    fun saveFileDir() = quigglesDir(
        Environment.getExternalStorageDirectory()
    )

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

    fun saveWithName(drawing: Drawing, callback: () -> Unit) {
        val saveFile = SaveFileV1(drawing)
        jsonToFile(saveFilename(drawing.filename!!), saveFile)
        callback()
    }

    fun saveAs(drawing: Drawing, callback: () -> Unit = {}) {
        dialog {
            setTitle("Choose save name")
            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_TEXT
            setView(input)
            setPositiveButton("OK") { _, _ ->
                val filename = input.text.toString().trim()
                fun doSave() {
                    drawing.filename = filename
                    saveWithName(drawing, callback)
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

    fun intent(cls: Class<*>) = Intent(this, cls)

    fun hasWritePermission() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

}