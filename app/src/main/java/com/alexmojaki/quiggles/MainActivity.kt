package com.alexmojaki.quiggles

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.Window
import android.widget.ImageButton
import android.widget.SeekBar
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder


class MainActivity : AppCompatActivity() {

    lateinit var paintView: BasePaintView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_main)

        paintView = findViewById(R.id.paintView)
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        paintView.init(metrics)

        findViewById<ImageButton>(R.id.deleteButton).setOnClickListener {
            paintView.drawing.deleteSelectedQuiggle()
        }


        findViewById<ImageButton>(R.id.colorButton).setOnClickListener {
            val quiggle = paintView.drawing.selectedQuiggle!!
            ColorPickerDialogBuilder.with(this)
                .initialColor(quiggle.color)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .lightnessSliderOnly()
                .setPositiveButton(
                    "OK"
                ) { _, selectedColor, _ -> quiggle.color = selectedColor }
                .setNegativeButton("Cancel") { _, _ -> Unit }
                .build()
                .show()
        }

        val scaleBar = findViewById<SeekBar>(R.id.scaleBar)
        findViewById<ImageButton>(R.id.scaleButton).setOnClickListener {
            scaleBar.visibility = if (scaleBar.visibility == VISIBLE) INVISIBLE else VISIBLE
        }

        paintView.drawing.buttons = findViewById(R.id.buttons)
    }

    override fun onResume() {
        super.onResume()
        paintView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

    }

}
