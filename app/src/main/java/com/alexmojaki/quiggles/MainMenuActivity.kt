package com.alexmojaki.quiggles

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Window
import kotlinx.android.synthetic.main.activity_main_menu.*


class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main_menu)

        newButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        loadButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("load", true)
            })
        }
    }


}
