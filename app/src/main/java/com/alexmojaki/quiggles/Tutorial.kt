package com.alexmojaki.quiggles

import android.content.Context
import android.widget.RelativeLayout
import android.widget.RelativeLayout.*
import kotlinx.android.synthetic.main.activity_main.*
import com.alexmojaki.quiggles.Tutorial.State.*

class Tutorial(val activity: MainActivity) {
    var state: State = Hidden
        set(value) {
            if (field == value) return
            if (value.visited && value != Hidden) {
                val next =
                    when (value) {
                        Select -> SelectedOne
                        else -> State.values()[value.ordinal + 1]
                    }
                if (next.visited ||
                    value.oneOf(
                        GoBackFromSelection,
                        MaxQuigglesSlider,
                        GlowAll
                    )
                ) {
                    @Suppress("RecursivePropertyAccessor")
                    state = Hidden
                    return
                }
            }

            field = value
            value.visited = true

            val textView = activity.tutorial_text!!
            textView.text = value.text

            val params = textView.layoutParams as RelativeLayout.LayoutParams
            TextPosition.values().forEach {
                params.removeRule(it.verb)
            }
            params.addRule(value.position.verb)

            with(prefs.edit()) {
                putBoolean(value.prefsKey(), true)
                apply()
            }
        }

    val prefs
        get() = activity.sharedPreferences

    init {
        State.values().forEach {
            it.visited = prefs.getBoolean(it.prefsKey(), false)
        }
    }

    fun maybeHide() {
        if (state.oneOf(GlowAll, MaxQuigglesSlider)) {
            state = Hidden
        }
    }

    enum class TextPosition(val verb: Int) {
        Top(ALIGN_PARENT_TOP),
        Middle(CENTER_VERTICAL),
        Bottom(ALIGN_PARENT_BOTTOM),
    }

    enum class State(val position: TextPosition, val text: String) {
        DrawOne(
            TextPosition.Middle,
            "Drag your finger to draw any simple shape."
        ),
        DrawMore(
            TextPosition.Bottom,
            "Great! This is a quiggle. Draw some more!"
        ),
        Select(
            TextPosition.Bottom,
            "Tap on the space occupied by a quiggle to select it. Don't worry if quiggles overlap."
        ),
        SelectedMany(
            TextPosition.Bottom,
            "You selected multiple quiggles. Tap on one to finish selecting, tap outside to go back, or draw some more."
        ),
        SelectedOne(
            TextPosition.Top,
            "Nice! These buttons give you control over each quiggle. Try them all."
        ),
        MoveSlider(
            TextPosition.Top,
            "Move the slider to see what it does. Go all the way if it's not obvious."
        ),
        GoBackFromSelection(
            TextPosition.Top,
            "Once you're done experimenting, tap outside the controls to go back."
        ),
        MaxQuigglesSlider(
            TextPosition.Top,
            "If you draw many quiggles, only some will be shown at a time. This slider controls the limit."
        ),
        GlowAll(
            TextPosition.Top,
            "This makes all quiggles (including new ones) 'glow', always changing color. Tap again to undo."
        ),
        HiddenMenuButton(
            TextPosition.Top,
            "The menu button is now invisible, but it's still there. Tapping the top left of the screen will " +
                    "show the menu."
        ),
        Hidden(TextPosition.Top, "");

        var visited: Boolean = false
        fun prefsKey() = "tutorial_visited_$name"
    }
}