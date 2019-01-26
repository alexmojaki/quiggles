package com.alexmojaki.quiggles

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue.*
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
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    private val buttonsList = ArrayList<ImageButton>()

    fun resetButtons() {
        buttonsList.forEach {
            it.backgroundTintList = null
        }
    }

    fun dp(x: Float) = applyDimension(COMPLEX_UNIT_DIP, x, resources.displayMetrics).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_main)

        paintView.init(this)
        val drawing = paintView.drawing

        fun addButton(imageId: Int, onClick: (View) -> Unit, highlight: Boolean = true) {
            val button = ImageButton(this, null, android.R.style.Widget_DeviceDefault_ImageButton)

            val width = dp(70f)
            val margin = dp(5f)
            val params = LinearLayout.LayoutParams(width, width)
            params.setMargins(margin, 0, margin, 0)
            with(button) {
                setImageResource(imageId)
                setBackgroundResource(R.drawable.round_button_background)
                setOnClickListener {
                    resetButtons()
                    if (highlight) {
                        backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B1cddc39"))
                    }
                    onClick.invoke(this)
                }
                layoutParams = params
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }

            buttonsLayout.addView(button)
            buttonsList.add(button)
        }

        // Scale
        addButton(R.drawable.scale, {
            drawing.edit()
            with(drawing.selectedQuiggle!!) {
                val original = outerRadius / (drawing.sheight / 2) * 100
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
                            drawing.swidth / 2 / outerRadius,
                            0.0
                        )

                        scaleDownToFit(drawing.sheight)

                        if (state == Quiggle.State.Complete) {
                            numPaths = numVertices
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
                    (maxPeriod / oscillationPeriod).roundToInt(),
                    { progress ->
                        oscillationPeriod = maxPeriod / progress
                        oscillate(drawing.sheight)
                    }
                )
            }
        })

        // Thickness
        addButton(R.drawable.thickness, {
            drawing.edit()
            with(drawing.selectedQuiggle!!) {
                val max = 200
                showSeekBar(
                    (thickness / max * 100).roundToInt(),
                    { progress ->
                        thickness = progress / 100f * max
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

}
