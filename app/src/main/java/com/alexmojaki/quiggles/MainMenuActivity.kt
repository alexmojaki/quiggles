package com.alexmojaki.quiggles

import android.content.Intent
import kotlinx.android.synthetic.main.activity_main_menu.*


class MainMenuActivity : CommonActivity() {

    override fun onCreate() {
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
