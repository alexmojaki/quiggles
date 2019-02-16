package com.alexmojaki.quiggles

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_main.*

abstract class CommonActivity : AppCompatActivity() {

    val metrics = DisplayMetrics()

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

    fun addButton(imageId: Int, onClick: (View) -> Unit): ImageButton {
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
            setOnClickListener(onClick)
            layoutParams = params
            scaleType = ImageView.ScaleType.FIT_XY
        }

        findViewById<ViewGroup>(R.id.buttonsLayout).addView(button)

        return button
    }
}