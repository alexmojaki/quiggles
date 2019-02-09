package com.alexmojaki.quiggles

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.PI
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    val metrics = DisplayMetrics()

    val drawing: Drawing
        get() = paintView.drawing

    private val buttonsList = ArrayList<ImageButton>()

    fun resetButtons() {
        buttonsList.forEach {
            it.backgroundTintList = null
        }
    }

    fun dp(x: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, x, metrics).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_main)

        windowManager.defaultDisplay.getMetrics(metrics)

        paintView.init(this)

        val filename = intent.getStringExtra("LOAD_FILENAME")
        if (filename != null) {
            load(filename, drawing)
        }

        if (intent.getBooleanExtra("LOAD_UNSAVED", false)) {
            fileToJson<SaveFile>(unsavedFile()).restore(drawing)
        }

        fun addButton(imageId: Int, onClick: (View) -> Unit, highlight: Boolean = true) {
            val button = ImageButton(this, null, android.R.style.Widget_DeviceDefault_ImageButton)

            val width = dp(70f)
            val margin = dp(5f)
            val padding = dp(16f)
            val params = LinearLayout.LayoutParams(width, width)
            params.setMargins(margin, 0, margin, 0)
            with(button) {
                setImageResource(imageId)
                setPadding(padding, padding, padding, padding)
                setBackgroundResource(R.drawable.round_button_background)
                setOnClickListener {
                    resetButtons()
                    if (highlight) {
                        backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B1cddc39"))
                    }
                    onClick.invoke(this)
                }
                layoutParams = params
                scaleType = ImageView.ScaleType.FIT_XY
            }

            buttonsLayout.addView(button)
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
                    hideSystemUi()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    hideSystemUi()
                }
                .build()
                .show()
            hideSystemUi()
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
        addButton(android.R.drawable.ic_menu_rotate, {
            drawing.edit()
            with(drawing.selectedQuiggle!!) {
                val maxPeriod = 50.0
                showSeekBar(
                    unstretchProgress(maxPeriod / rotationPeriod + 100),
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
        addButton(android.R.drawable.ic_menu_delete, {
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
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    private fun hideSystemUi() {
        paintView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    override fun onBackPressed() {
        val optionsMap = mapOf(
            "Main menu" to {
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
            },
            "Save" to { save() }
        ) + (
                if (drawing.filename != null)
                    mapOf("Save As" to {
                        saveAs(drawing)
                    })
                else emptyMap()
                ) +
                mapOf(
                    "Make GIF" to {
                        gifDrawing = drawing
                        startActivity(intent(GifActivity::class.java))
                    }
                )
        val optionsArr = optionsMap.keys.toTypedArray()

        dialog {
            setItems(optionsArr) { _, which ->
                optionsMap[optionsArr[which]]?.invoke()
                hideSystemUi()
            }
        }
        hideSystemUi()
    }

    private fun save(callback: () -> Unit = {}) {
        if (drawing.filename == null) {
            saveAs(drawing, callback)
        } else {
            saveWithName(drawing, callback)
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
