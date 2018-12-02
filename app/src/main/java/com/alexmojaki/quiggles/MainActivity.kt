package com.alexmojaki.quiggles

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.widget.ImageButton


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

        val deleteButton: ImageButton = findViewById(R.id.deleteButton)
        deleteButton.setOnClickListener { paintView.drawing.deleteSelectedQuiggle() }

        paintView.drawing.buttons = listOf(deleteButton)
    }

    override fun onResume() {
        super.onResume()
        paintView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

    }

}
