package com.alexmojaki.quiggles

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.alexmojaki.quiggles.Tutorial.State.*
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileNotFoundException
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt


class MainActivity : CommonActivity() {

    private inline val drawing: Drawing
        get() = paintView.drawing
    private inline val scenter: Point
        get() = drawing.scenter
    private inline val selectedQuiggle: Quiggle
        get() = drawing.selectedQuiggleChecked

    lateinit var tutorial: Tutorial

    private val buttonsList = ArrayList<ImageButton>()

    fun resetButtons() {
        buttonsList.forEach {
            it.backgroundTintList = null
        }
    }

    override fun onCreate() {
        setContentView(R.layout.activity_main)

        paintView.init(this)
        drawing.tutorialQuiggle = TutorialQuiggle(drawing)

        val filename = intent.getStringExtra("LOAD_FILENAME")
        if (filename != null) {
            load(filename, drawing)
        }

        if (intent.getBooleanExtra("LOAD_UNSAVED", false)) {
            loadUnsaved().restore(drawing)
        }

        tutorial = Tutorial(this)
        if (!DrawOne.visited) tutorial.state = DrawOne

        var buttonsGroup = editQuiggleButtonsLayout

        fun addButton(
            label: String,
            imageId: Int,
            onClick: (View) -> Unit,
            highlight: Boolean = true
        ) {
            var button: ImageButton? = null
            button = this.addButton(label, imageId, buttonsGroup) {
                resetButtons()
                if (highlight) {
                    button!!.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B1cddc39"))
                }

                try {
                    onClick.invoke(button!!)
                } catch (_: NoSelectedQuiggle) {
                    // Crash reports show that sometimes drawing.selectedQuiggle is null,
                    // presumably some race condition of clicking the button interleaved with
                    // exiting the selection. Ignore this.
                }
            }

            buttonsList.add(button)
        }

        addButton("Size", R.drawable.scale, {
            drawing.editSelectedQuiggleInContext()
            with(selectedQuiggle) {
                val original = outerRadius / (scenter.y) * 100
                showSeekBar(
                    max((usualScale * original).roundToInt(), 1) - 1,
                    { progress ->
                        usualScale = (progress + 1) / original
                        drawing.resetQuigglePosition(this, 0.0)
                    }
                )
            }
        })

        addButton("Color", R.drawable.color_palette, {
            drawing.selectedQuiggleEdited = true
            seekBar.visibility = INVISIBLE

            val quiggle = selectedQuiggle

            ColorPickerDialogBuilder.with(this)
                .initialColor(quiggle.color)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .lightnessSliderOnly()
                .setPositiveButton(
                    "OK"
                ) { _, selectedColor, _ ->
                    quiggle.color = selectedColor
                }
                .setNegativeButton("Cancel") { _, _ ->
                }
                .build()
                .show()
        }, highlight = false)

        addButton("Glow", R.drawable.rainbow, {
            drawing.editSelectedQuiggleInContext()
            with(selectedQuiggle) {
                val maxPeriod = 200.0
                showSeekBar(
                    unstretchProgress(maxPeriod / huePeriod) + 100,
                    { progress ->
                        huePeriod = maxPeriod / stretchProgress(progress - 100)
                        glow()
                        glowNormally = huePeriod.isFinite()
                    },
                    max = 200
                )
            }
        })

        addButton("Shape", R.drawable.star, {
            drawing.selectedQuiggleEdited = true
            val angles = angleToPoints.navigableKeySet().toList()
            with(selectedQuiggle) {
                drawing.selectOne(this)
                showSeekBar(
                    angles.indexOf(idealAngle),
                    { progress ->
                        setAngle(angles[progress])
                        setPosition(
                            scenter,
                            scenter.x / outerRadius,
                            0.0
                        )

                        scaleDownToFit()

                        if (state == Quiggle.State.Complete) {
                            numPaths = numVertices - 1
                        }
                    },
                    max = angles.size - 1
                )
            }
        })

        addButton("Copy", R.drawable.content_copy, {
            val copy = selectedQuiggle.duplicate()
            with(drawing) {
                quiggles.add(copy)
                selectOne(copy)
                editSelectedQuiggleInContext()
            }
        })

        addButton("Grow", R.drawable.wave, {
            drawing.editSelectedQuiggleInContext()
            with(selectedQuiggle) {
                val maxPeriod = 50.0
                showSeekBar(
                    unstretchProgress(maxPeriod / oscillationPeriod),
                    { progress ->
                        oscillationPeriod = maxPeriod / stretchProgress(progress)
                        oscillate()
                    }
                )
            }
        })

        addButton("Spin", R.drawable.rotate_right, {
            drawing.editSelectedQuiggleInContext()
            with(selectedQuiggle) {
                val maxPeriod = 50.0
                showSeekBar(
                    unstretchProgress(maxPeriod / rotationPeriod) + 100,
                    { progress ->
                        rotationPeriod = maxPeriod / stretchProgress(progress - 100)
                        rotationAnimation = rotationAnimation.change(
                            rotationAnimation.currentValue() + 2 * PI,
                            period = rotationPeriod,
                            easingFunction = { x -> x }
                        )
                    },
                    max = 200
                )
            }
        })

        addButton("Thicken", R.drawable.thickness, {
            drawing.editSelectedQuiggleInContext()
            with(selectedQuiggle) {
                val min = 0.5
                val max = 200
                showSeekBar(
                    unstretchProgress((thickness - min) / max * 100.0),
                    { progress ->
                        thickness = (stretchProgress(progress) / 100 * max + min).toFloat()
                    }
                )
            }
        })

        addButton("To front", R.drawable.arrange_bring_to_front, {
            val quiggle = selectedQuiggle
            with(drawing) {
                selectedQuiggleEdited = true
                quiggles.remove(quiggle)
                quiggles.add(quiggle)
            }
            toast("Quiggle brought to front/top")
        })

        addButton("To back", R.drawable.arrange_send_to_back, {
            val quiggle = selectedQuiggle
            with(drawing) {
                selectedQuiggleEdited = true
                quiggles.remove(quiggle)
                quiggles.add(0, quiggle)
            }
            toast("Quiggle sent to back/bottom")
        })

        addButton("Delete", R.drawable.delete, {
            drawing.deleteSelectedQuiggle()
        })

        buttonsGroup = editCanvasButtonsLayout

        addButton("Stars", R.drawable.star_four_points, {
            seekBar.visibility = INVISIBLE
            tutorial.maybeHide()
            drawing.starField =
                if (drawing.starField == null)
                    StarField(scenter)
                else
                    null
        }, highlight = false)

        addButton("Number", R.drawable.counter, {
            tutorial.state = MaxQuigglesSlider
            showSeekBar(
                drawing.maxQuiggles - 1,
                { x -> drawing.maxQuiggles = x + 1 },
                40,
                doTutorial = false
            )
        })

        addButton("Glow All", R.drawable.rainbow, {
            tutorial.state = GlowAll
            with(drawing) {
                allGlow = !allGlow
                quiggles.forEachApply {
                    if (!glowNormally) {
                        if (allGlow) {
                            glowRandomly()
                        } else {
                            stopGlowing()
                        }
                    }
                }
            }
        }, highlight = false)

        menuButton.setOnClickListener {
            if (isInstant) {
                editCanvas()
            } else {
                showMenu()
            }
        }
        if (showMenuButton == null) {
            showMenuButton = sharedPreferences.getInt(HIDE_MENU_COUNT, 0) < 3
        }
        menuButton.visible = showMenuButton!!

        startedMain = true

        if (!isInstant) {
            onBackPressedDispatcher.addCallback(this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        showMenu()
                    }
                }
            )
        }
    }

    private fun showSeekBar(progress: Int, onChange: (Int) -> Unit, max: Int = 100, doTutorial: Boolean = true) {
        seekBar.visibility = VISIBLE
        seekBar.max = max
        seekBar.progress = progress
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                onChange(progress)
                if (doTutorial) tutorial.state = GoBackFromSelection
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
        if (doTutorial) tutorial.state = MoveSlider
    }

    fun showMenu() {
        class Item(val text: String, val icon: Int, val action: () -> Unit) {
            override fun toString(): String {
                return text
            }
        }

        val optionsMap = LinkedHashMap<String, Item>()

        fun item(text: String, icon: Int, action: () -> Unit) {
            optionsMap[text] = Item(text, icon, action)
        }

        item("Main menu", R.drawable.backburger) {
            if (isChanged()) {
                dialog {
                    setTitle("Save changes?")
                    setPositiveButton("Yes") { _, _ ->
                        save(::finish)
                    }
                    setNegativeButton("No") { _, _ ->
                        finish()
                    }
                }
            } else {
                finish()
            }
        }

        if (drawing.quiggles.isNotEmpty()) {
            item("Save", R.drawable.content_save) { save() }

            if (drawing.filename != null) {
                item("Save As", R.drawable.content_save_all) { saveAsDialog(drawing) }
            }

            item("Make GIF", R.drawable.animation_play) {
                withWritePermission { makeGif() }
            }
        }

        item("Edit canvas", R.drawable.drawing_box, ::editCanvas)

        item(
            if (menuButton.visible) "Hide menu button"
            else "Show menu button",
            R.drawable.menu
        ) {
            val show = !menuButton.visible
            showMenuButton = show
            menuButton.visible = show
            tutorial.state = if (show) Hidden else HiddenMenuButton
            sharedPreferences.edit {
                putInt(
                    HIDE_MENU_COUNT,
                    if (show) 0
                    else sharedPreferences.getInt(HIDE_MENU_COUNT, 0) + 1
                )
            }
        }

        val optionsArr = optionsMap.keys.toTypedArray()

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.select_dialog_item,
            android.R.id.text1,
            optionsArr
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup) =
                super.getView(position, convertView, parent).apply {
                    (findViewById<View>(android.R.id.text1) as TextView).apply {
                        //Put the image on the TextView
                        setCompoundDrawablesWithIntrinsicBounds(
                            optionsMap[optionsArr[position]]!!.icon,
                            0, 0, 0
                        )
                        compoundDrawablePadding = dp(5f)
                    }

                }
        }

        dialog {
            setAdapter(adapter) { _, which ->
                optionsMap[optionsArr[which]]!!.action()
            }
        }

    }

    private fun editCanvas() {
        drawing.selectNone()
        editCanvasButtons.visibility = VISIBLE
        tutorial.state = Hidden
    }

    private fun makeGif() {
        gifDrawing = drawing
        startActivity(intent(GifActivity::class))
    }

    private fun save(callback: () -> Unit = {}) {
        if (drawing.filename == null) {
            saveAsDialog(drawing, callback)
        } else {
            saveWithName(drawing)
            callback()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isChanged()) {
            val saveFile = SaveFileV1(drawing)
            jsonToFile(unsavedFile(), saveFile)
        }
    }

    private fun isChanged(): Boolean {
        if (drawing.quiggles.isEmpty()) {
            return false
        }
        val filename = drawing.filename ?: return true
        val current = jsonMapper.readTree(jsonMapper.writeValueAsString(SaveFileV1(drawing)))
        val onDisk = try {
            jsonMapper.readTree(saveFilename(filename))
        } catch (_: FileNotFoundException) {
            null
        }
        return onDisk != current
    }

}

var showMenuButton: Boolean? = null
const val HIDE_MENU_COUNT = "hideMenuCount"
var startedMain = false
