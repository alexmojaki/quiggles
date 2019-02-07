package com.alexmojaki.quiggles

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
            startActivity(intent(MainActivity::class.java))
        }

        loadButton.setOnClickListener {
            val filenames = saveFileDir().list()
            filenames.sort()
            dialog {
                setItems(filenames) { _, which ->
                    startActivity(
                        intent(MainActivity::class.java)
                            .putExtra("LOAD_FILENAME", filenames[which])
                    )
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        loadButton.isEnabled = saveFileDir().list().isNotEmpty()
    }

}
