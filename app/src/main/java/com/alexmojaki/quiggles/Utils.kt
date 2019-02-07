package com.alexmojaki.quiggles

import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.graphics.Path
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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

inline fun <reified T> Context.fileToJson(filename: String): T {
    FileInputStream(saveFilename(filename)).use {
        return jsonMapper.readValue(it)
    }
}

operator fun File.div(name: String) = File(this, name)

fun Context.saveFilename(filename: String) = saveFileDir() / filename

fun Context.saveFileDir(): File {
    val dir = filesDir / "saved_quiggles"
//    for (f in dir.listFiles()) f.delete()
    dir.mkdir()
    return dir
}

inline fun <reified T> Context.jsonToFile(filename: String, value: T) {
    FileOutputStream(saveFilename(filename)).use {
        jsonMapper.writeValue(it, value)
    }
}

fun Context.load(filename: String, drawing: Drawing) {
    fileToJson<SaveFile>(filename).restore(drawing)
    drawing.filename = filename
}

fun Context.save(drawing: Drawing) {
    fun doSave() {
        val saveFile = SaveFileV1(drawing.quiggles)
        jsonToFile(drawing.filename!!, saveFile)
    }
    if (drawing.filename == null) {
        dialog {
            setTitle("Choose save name")
            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_TEXT
            setView(input)
            setPositiveButton("OK") { dialog, _ ->
                val filename = input.text.toString().trim()
                if (filename.isEmpty()) {
                    dialog.cancel()
                    toast("Empty filename not allowed")
                } else {
                    drawing.filename = filename
                    doSave()
                }
            }
            setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        }
    } else {
        doSave()
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
