package com.alexmojaki.quiggles

import android.annotation.SuppressLint
import android.content.Intent
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.android.synthetic.main.activity_main_menu.*


class MainMenuActivity : CommonActivity() {

    @SuppressLint("InflateParams")
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
            filenames!!.sort()
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

        heartButton.setOnClickListener {
            dialog {
                val view = layoutInflater.inflate(R.layout.heart_dialog, null)
                val textView = view.findViewById<TextView>(R.id.heartTextView)
                setView(view)
                setTextViewHTML(
                    textView,
                    """
<p>
    This app is 100% free and open source. No ads or locked content!
    If this makes you happy and you want to support me doing this kind of work,
    check out my main project <a href="http://futurecoder.io/">futurecoder</a>:
    a 100% free and interactive course to learn programming from scratch.
</p>

<p>You can also help the app spread by:</p>

<p>- <a href="#share">Sharing</a> the app with your friends</p>
<p>- <a href="#rate">Rating and reviewing</a> on the Play Store</p>
<p>- Sending feedback to <a href="mailto:alex.mojaki+quiggles@gmail.com">alex.mojaki@gmail.com</a></p>
<p>- Contributing code on <a href="http://github.com/alexmojaki/quiggles">GitHub</a></p>

<p>
    You can read the
    <a href="https://raw.githubusercontent.com/alexmojaki/quiggles/master/PRIVACY.md">
    privacy policy here</a> if you're into that kind of thing.
</p>
"""
                )
            }
        }
    }

    private fun startMain(intentCallback: (Intent.() -> Unit)? = null) {
        val intent = intent(MainActivity::class)
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

        loadButton.visible = saveFileDir().list()?.isNotEmpty() == true
        loadUnsavedButton.visible = hasUnsaved()
    }

    private fun makeLinkClickable(strBuilder: SpannableStringBuilder, span: URLSpan, callback: () -> Unit) {
        val start = strBuilder.getSpanStart(span)
        val end = strBuilder.getSpanEnd(span)
        val flags = strBuilder.getSpanFlags(span)
        val clickable = object : ClickableSpan() {
            override fun onClick(view: View) {
                callback()
            }
        }
        strBuilder.setSpan(clickable, start, end, flags)
        strBuilder.removeSpan(span)
    }

    @Suppress("SameParameterValue")
    private fun setTextViewHTML(textView: TextView, html: String) {
        // https://stackoverflow.com/questions/12418279/android-textview-with-clickable-links-how-to-capture-clicks/19989677
        val sequence = fromHtml(html)
        val strBuilder = SpannableStringBuilder(sequence)
        val urls = strBuilder.getSpans(
            0,
            sequence.length,
            URLSpan::class.java
        )
        for (span in urls) {
            makeLinkClickable(
                strBuilder,
                span,
                when (span.url) {
                    "#share" -> { -> shareApp() }
                    "#rate" -> { -> rateApp() }
                    else -> { -> openInBrowser(span.url) }
                }
            )
        }
        textView.text = strBuilder
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

}
