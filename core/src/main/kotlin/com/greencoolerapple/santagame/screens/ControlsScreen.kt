package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.input.Action
import com.greencoolerapple.santagame.input.Controls
import com.greencoolerapple.santagame.ui.Button

class ControlsScreen(game: SantaGame) : MenuBase(game) {
    private class Slot(val action: Action, val index: Int) {
        val button = Button("", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 1.5f)
    }

    private val actions = Action.entries
    private val slots = actions.flatMap { action -> List(Action.SLOTS) { Slot(action, it) } }
    private val resetButton = Button("", labelScale = 2f)
    private val back = backButton()
    private var waiting: Slot? = null

    override val buttons = slots.map { it.button } + listOf(resetButton, back)

    override val showSanta: Boolean = false

    override val toastY: Float
        get() = worldHeight * 0.165f

    private fun rowY(row: Int): Float = worldHeight * (0.715f - row * 0.075f)

    override fun layout() {
        for (slot in slots) {
            val x = worldWidth / 2f + if (slot.index == 0) 90f else 340f
            slot.button.place(x, rowY(slot.action.ordinal), 230f, 44f)
            slot.button.label = if (slot == waiting) {
                if ((MathUtils.sin(time * 8f) + 1f) / 2f > 0.35f) I18n.t("controls.press") else ""
            } else {
                Controls.keyName(game.controls.key(slot.action, slot.index))
            }
            slot.button.fill = if (slot == waiting) Palette.BUTTON_GOLD else Palette.BUTTON_DARK
            slot.button.edge = if (slot == waiting) Palette.BUTTON_GOLD_EDGE else Palette.BUTTON_DARK_EDGE
        }
        resetButton.place(worldWidth / 2f - 150f, worldHeight * 0.08f, 260f, 62f)
        back.place(worldWidth / 2f + 150f, worldHeight * 0.08f, 260f, 62f)
        resetButton.label = I18n.t("controls.reset")
        captureKey()
    }

    private fun captureKey() {
        val slot = waiting ?: return
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) || Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
            game.controls.bind(slot.action, slot.index, Input.Keys.UNKNOWN)
            finishCapture()
            return
        }
        for (code in 1..Input.Keys.MAX_KEYCODE) {
            if (code == Input.Keys.ESCAPE || code == Input.Keys.BACK) continue
            if (Gdx.input.isKeyJustPressed(code)) {
                game.controls.bind(slot.action, slot.index, code)
                finishCapture()
                return
            }
        }
    }

    private fun finishCapture() {
        waiting = null
        game.saveControls()
        game.haptics.play(Haptics.Pattern.TAP)
    }

    override fun interceptBack(): Boolean {
        if (waiting == null) return false
        waiting = null
        return true
    }

    override fun onTap(button: Button) {
        when (button) {
            back -> onBack()
            resetButton -> {
                waiting = null
                game.controls.resetDefaults()
                game.saveControls()
                showToast(I18n.t("controls.resetDone"))
            }
            else -> {
                val slot = slots.firstOrNull { it.button == button } ?: return
                waiting = if (waiting == slot) null else slot
            }
        }
    }

    override fun onBack() {
        game.setScreen(OptionsScreen(game))
    }

    override fun drawTexts() {
        game.drawText(I18n.t("controls.title"), worldWidth / 2f, worldHeight * 0.9f, 3.8f, Palette.GOLD)
        game.drawText(I18n.t("controls.key"), worldWidth / 2f + 90f, worldHeight * 0.79f, 1.3f, Palette.GOLD_LIGHT)
        game.drawText(I18n.t("controls.alternative"), worldWidth / 2f + 340f, worldHeight * 0.79f, 1.3f, Palette.GOLD_LIGHT)
        for (action in actions) {
            game.drawText(action.title, worldWidth / 2f - 460f, rowY(action.ordinal) + 12f, 1.6f, centered = false)
        }
        game.drawText(I18n.t("controls.hint"), worldWidth / 2f, worldHeight * 0.215f, 1.15f, Palette.TEXT_DIM)
    }
}
