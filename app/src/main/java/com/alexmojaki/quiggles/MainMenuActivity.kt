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
            startMain()
        }

        loadButton.setOnClickListener {
            val filenames = saveFileDir().list()
            filenames.sort()
            dialog {
                setItems(filenames) { _, which ->
                    startMain {
                        putExtra("LOAD_FILENAME", filenames[which])
                    }
                }
            }

        }

        loadUnsavedButton.setOnClickListener {
            startMain {
                putExtra("LOAD_UNSAVED", true)
            }
        }
    }

    fun startMain(intentCallback: (Intent.() -> Unit)? = null) {
        val intent = intent(MainActivity::class.java)
        intentCallback?.invoke(intent)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadButton.isEnabled = saveFileDir().list().isNotEmpty()
        loadUnsavedButton.isEnabled = unsavedFile().exists()
    }

}
