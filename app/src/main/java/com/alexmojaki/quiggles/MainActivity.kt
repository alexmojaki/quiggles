package com.alexmojaki.quiggles

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.alexmojaki.quiggles.Tutorial.State.*
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.PI
import kotlin.math.roundToInt


class MainActivity : CommonActivity() {

    val drawing: Drawing
        get() = paintView.drawing

    lateinit var tutorial: Tutorial

    private val buttonsList = ArrayList<ImageButton>()

    fun resetButtons() {
        buttonsList.forEach {
            it.backgroundTintList = null
        }
    }

    override fun onCreate() {
        setContentView(R.layout.activity_main)

        paintView.init(this)

        val filename = intent.getStringExtra("LOAD_FILENAME")
        if (filename != null) {
            load(filename, drawing)
        }

        tutorial = Tutorial(this)
        tutorial.state = DrawOne

        if (intent.getBooleanExtra("LOAD_UNSAVED", false)) {
            fileToJson<SaveFile>(unsavedFile()).restore(drawing)
        }

        fun addButton(imageId: Int, onClick: (View) -> Unit, highlight: Boolean = true) {
            var button: ImageButton? = null
            button = this.addButton(imageId) {
                resetButtons()
                if (highlight) {
                    button!!.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B1cddc39"))
                }
                onClick.invoke(button!!)
            }

            buttonsList.add(button)
        }

        // Scale
        addButton(R.drawable.scale, {
            drawing.edit()
            with(drawing.selectedQuiggle!!) {
                val original = outerRadius / (drawing.scenter.y) * 100
                showSeekBar(
                    (usualScale * original).roundToInt(),
                    { progress ->
                        usualScale = progress / original
                        drawing.resetQuigglePosition(this, 0.0)
                    }
                )
            }
        })

        // Color
        addButton(R.drawable.color_palette, {
            drawing.edited = true
            seekBar.visibility = INVISIBLE

            val quiggle = drawing.selectedQuiggle!!

            ColorPickerDialogBuilder.with(this)
                .initialColor(quiggle.color)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .lightnessSliderOnly()
                .setPositiveButton(
                    "OK"
                ) { _, selectedColor, _ ->
                    quiggle.color = selectedColor
                }
                .setNegativeButton("Cancel") { _, _ ->
                }
                .build()
                .show()
        }, highlight = false)

        // Angle
        addButton(R.drawable.star, {
            drawing.edited = true
            val angles = angleToPoints.navigableKeySet().toList()
            with(drawing.selectedQuiggle!!) {
                drawing.selectOne(this)
                showSeekBar(
                    angles.indexOf(idealAngle),
                    { progress ->
                        setAngle(angles[progress])
                        setPosition(
                            drawing.scenter,
                            drawing.scenter.x / outerRadius,
                            0.0
                        )

                        scaleDownToFit(drawing.scenter)

                        if (state == Quiggle.State.Complete) {
                            numPaths = numVertices - 1
                        }
                    },
                    max = angles.size - 1
                )
            }
        })

        // Oscillation
        addButton(R.drawable.wave, {
            drawing.edit()
            with(drawing.selectedQuiggle!!) {
                val maxPeriod = 50.0
                showSeekBar(
                    unstretchProgress(maxPeriod / oscillationPeriod),
                    { progress ->
                        oscillationPeriod = maxPeriod / stretchProgress(progress)
                        oscillate(drawing.scenter)
                    }
                )
            }
        })

        // Rotation
        addButton(R.drawable.rotate_right, {
            drawing.edit()
            with(drawing.selectedQuiggle!!) {
                val maxPeriod = 50.0
                showSeekBar(
                    unstretchProgress(maxPeriod / rotationPeriod) + 100,
                    { progress ->
                        rotationPeriod = maxPeriod / stretchProgress(progress - 100)
                        rotationAnimation = rotationAnimation.change(
                            rotationAnimation.currentValue() + 2 * PI,
                            period = rotationPeriod,
                            easingFunction = { x -> x }
                        )
                    },
                    max = 200
                )
            }
        })

        // Thickness
        addButton(R.drawable.thickness, {
            drawing.edit()
            with(drawing.selectedQuiggle!!) {
                val min = 0.5
                val max = 200
                showSeekBar(
                    unstretchProgress((thickness - min) / max * 100.0),
                    { progress ->
                        thickness = (stretchProgress(progress) / 100 * max + min).toFloat()
                    }
                )
            }
        })

        // Delete
        addButton(R.drawable.delete, {
            drawing.deleteSelectedQuiggle()
        })

    }

    fun showSeekBar(progress: Int, onChange: (Int) -> Unit, max: Int = 100) {
        seekBar.visibility = VISIBLE
        seekBar.max = max
        seekBar.progress = progress
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                onChange(progress)
                tutorial.state = GoBackFromSelection
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
        tutorial.state = MoveSlider
    }

    override fun onBackPressed() {
        class Item(val text: String, val icon: Int, val action: () -> Unit) {
            override fun toString(): String {
                return text
            }
        }

        val optionsMap = LinkedHashMap<String, Item>()

        fun item(text: String, icon: Int, action: () -> Unit) {
            optionsMap[text] = Item(text, icon, action)
        }

        item("Main menu", R.drawable.backburger) {
            if (isChanged()) {
                dialog {
                    setTitle("Save changes?")
                    setPositiveButton("Yes") { _, _ ->
                        save(::finish)
                    }
                    setNegativeButton("No") { _, _ ->
                        finish()
                    }
                }
            } else {
                finish()
            }
        }

        if (drawing.starField == null) {
            item("Add stars", R.drawable.star_four_points) {
                drawing.starField = StarField(drawing.scenter)
            }
        } else {
            item("Remove stars", R.drawable.star_four_points) {
                drawing.starField = null
            }
        }

        if (drawing.quiggles.isNotEmpty()) {
            item("Save", R.drawable.content_save) { save() }

            if (drawing.filename != null) {
                item("Save As", R.drawable.content_save_all) { withWritePermission { saveAs(drawing) } }
            }

            item("Make GIF", R.drawable.animation_play) {
                withWritePermission { makeGif() }
            }
        }

        val optionsArr = optionsMap.keys.toTypedArray()

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.select_dialog_item,
            android.R.id.text1,
            optionsArr
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup) =
                super.getView(position, convertView, parent).apply {
                    (findViewById<View>(android.R.id.text1) as TextView).apply {
                        //Put the image on the TextView
                        setCompoundDrawablesWithIntrinsicBounds(
                            optionsMap[optionsArr[position]]!!.icon,
                            0, 0, 0)
                        compoundDrawablePadding = dp(5f)
                    }

                }
        }

        dialog {
            setAdapter(adapter) { _, which ->
                optionsMap[optionsArr[which]]!!.action()
            }
        }

        if (tutorial.state == PressBackButton) {
            tutorial.state = Hidden
        }
    }

    private fun makeGif() {
        gifDrawing = drawing
        startActivity(intent(GifActivity::class.java))
    }

    private fun save(callback: () -> Unit = {}) {
        withWritePermission {
            if (drawing.filename == null) {
                saveAs(drawing, callback)
            } else {
                saveWithName(drawing, callback)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isChanged()) {
            val saveFile = SaveFileV1(drawing)
            jsonToFile(unsavedFile(), saveFile)
        }
    }

    fun isChanged(): Boolean {
        if (drawing.quiggles.isEmpty()) {
            return false
        }
        val filename = drawing.filename ?: return true
        val current = jsonMapper.readTree(jsonMapper.writeValueAsString(SaveFileV1(drawing)))
        val onDisk = jsonMapper.readTree(saveFilename(filename))
        return onDisk != current
    }

}
