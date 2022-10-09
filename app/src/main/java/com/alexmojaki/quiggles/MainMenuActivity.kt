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
<p>You can save children's lives while thanking me for making this app and encouraging more work on it by donating to
    the <a href="https://www.againstmalaria.com/Fundraiser.aspx?FundraiserID=8190">Against Malaria Foundation here</a>.
</p>

<p>You can also help the app spread and lead others to donate by:</p>

<p>- <a href="#share">Sharing</a> the app with your friends.</p>
<p>- <a href="#rate">Rating and reviewing</a> on the Play Store.</p>
<p>- Sending feedback to <a href="mailto:alex.mojaki@gmail.com">alex.mojaki@gmail.com</a>.</p>
<p>- Contributing code on <a href="http://github.com/alexmojaki/quiggles">GitHub</a>.</p>
"""
                )
            }
        }
    }

    fun startMain(intentCallback: (Intent.() -> Unit)? = null) {
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
        loadUnsavedButton.visible = unsavedFile().exists()
    }

    fun makeLinkClickable(strBuilder: SpannableStringBuilder, span: URLSpan, callback: () -> Unit) {
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

    fun setTextViewHTML(textView: TextView, html: String) {
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
