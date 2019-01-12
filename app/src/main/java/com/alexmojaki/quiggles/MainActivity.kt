package com.alexmojaki.quiggles

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.VISIBLE
import android.view.Window
import android.widget.SeekBar
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_main)

        paintView.init(this)
        val drawing = paintView.drawing

        deleteButton.setOnClickListener {
            drawing.deleteSelectedQuiggle()
        }

        colorButton.setOnClickListener {
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
        }

        scaleButton.setOnClickListener {
            drawing.edit()
            with(drawing.selectedQuiggle!!) {
                val original = outerRadius / (drawing.sheight / 2) * 100
                showSeekBar(
                    (usualScale * original).roundToInt(),
                    { progress ->
                        usualScale = progress / original
                        setPosition(drawing.scenter, usualScale, 0.0)
                    }
                )
            }
        }

        thicknessButton.setOnClickListener {
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
        }

        angleButton.setOnClickListener {
            drawing.edit()
            val angles = angleToPoints.navigableKeySet().toList()
            with(drawing.selectedQuiggle!!) {
                showSeekBar(
                    angles.indexOf(idealAngle),
                    { progress ->
                        setAngle(angles[progress])
                        setPosition(drawing.scenter, usualScale, 0.0)
                        if (state == Quiggle.State.Complete) {
                            numPaths = numVertices
                        }
                    },
                    max = angles.size - 1
                )
            }
        }


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
