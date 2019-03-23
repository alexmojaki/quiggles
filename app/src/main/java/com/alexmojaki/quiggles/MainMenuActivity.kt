package com.alexmojaki.quiggles

import android.content.Intent
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.android.synthetic.main.activity_main_menu.*


class MainMenuActivity : CommonActivity() {

    override fun onCreate() {
        if (isInstant) {
            startMain()
            return
        }

        setContentView(R.layout.activity_main_menu)

        paintView.init(this)
        assets.open("menu_background.json").use {
            jsonMapper.readValue<SaveFile>(it).restore(paintView.drawing)
        }
        paintView.drawing.quiggles[0].baseHue = nextHue()

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
        if (isInstant) {
            if (startedMain) {
                // The main screen has already been opened, so the user
                // probably pressed the back button. End the app.
                finish()
            }
            return
        }

        val can = hasWritePermission()
        loadButton.isEnabled = can && saveFileDir().list().isNotEmpty()
        loadUnsavedButton.isEnabled = can && unsavedFile().exists()
    }

}
