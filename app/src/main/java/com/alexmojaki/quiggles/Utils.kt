package com.alexmojaki.quiggles

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.graphics.Path
import android.os.Environment
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
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

inline fun <reified T> Context.fileToJson(file: File): T {
    FileInputStream(file).use {
        return jsonMapper.readValue(it)
    }
}

operator fun File.div(name: String) = File(this, name)

fun Context.saveFilename(filename: String) = saveFileDir() / filename

fun Context.saveFileDir(): File {
    val dir = internalDir() / "saved_quiggles"
//    for (f in dir.listFiles()) f.delete()
    dir.mkdir()
    return dir
}

fun Context.internalDir(): File = (getExternalFilesDirs(null).filterNotNull() + filesDir)[0]

fun picsDir(): File {
    val dir = getExternalStoragePublicDirectory(DIRECTORY_PICTURES) / "Quiggles"
//    for (f in dir.listFiles()) f.delete()
    dir.mkdir()
    return dir
}

fun currentTime(): Date = Calendar.getInstance().time

@SuppressLint("SimpleDateFormat")
fun isoFormat(dt: Date) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dt)

fun Context.unsavedFile(): File = internalDir() / "unsaved"

inline fun <reified T> Context.jsonToFile(file: File, value: T) {
    try {
        FileOutputStream(file).use {
            jsonMapper.writeValue(it, value)
        }
    } catch (e: IOException) {
        toast("Error saving file: ${e.message}", Toast.LENGTH_LONG)
    }
}

fun Context.load(filename: String, drawing: Drawing) {
    fileToJson<SaveFile>(saveFilename(filename)).restore(drawing)
    drawing.filename = filename
}

fun Context.saveWithName(drawing: Drawing, callback: () -> Unit) {
    val saveFile = SaveFileV1(drawing)
    jsonToFile(saveFilename(drawing.filename!!), saveFile)
    callback.invoke()
}

fun Context.saveAs(drawing: Drawing, callback: () -> Unit = {}) {
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

fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Context.dialog(block: AlertDialog.Builder.() -> Unit) {
    AlertDialog.Builder(this).apply {
        block()
        show()
    }
}

fun Context.intent(cls: Class<*>) = Intent(this, cls)

fun time(block: () -> Unit) {
    val start = System.currentTimeMillis()
    block()
    val end = System.currentTimeMillis()
    println("Time taken = ${end - start}")
}
