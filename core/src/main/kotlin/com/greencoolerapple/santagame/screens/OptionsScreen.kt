package com.greencoolerapple.santagame.screens

import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.ui.Button

class OptionsScreen(game: SantaGame) : MenuBase(game) {
    private val fpsDown = Button("<", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 3f)
    private val fpsUp = Button(">", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 3f)
    private val saveButton = Button("Save Game", labelScale = 2.4f)
    private val loadButton = Button("Load Game", fill = Palette.BUTTON_ALT, edge = Palette.BUTTON_ALT_EDGE, labelScale = 2.4f)
    private val back = backButton()

    override val buttons = listOf(fpsDown, fpsUp, saveButton, loadButton, back)

    override fun layout() {
        val rowY = worldHeight * 0.6f
        fpsDown.place(worldWidth / 2f - 170f, rowY, 90f, 80f)
        fpsUp.place(worldWidth / 2f + 170f, rowY, 90f, 80f)
        saveButton.place(worldWidth / 2f - 200f, worldHeight * 0.4f, 360f, 96f)
        loadButton.place(worldWidth / 2f + 200f, worldHeight * 0.4f, 360f, 96f)
        back.place(worldWidth / 2f, worldHeight * 0.2f, 260f, 76f)
    }

    override fun onTap(button: Button) {
        when (button) {
            fpsDown -> game.changeFps(-1)
            fpsUp -> game.changeFps(1)
            saveButton -> game.setScreen(SaveSlotsScreen(game, SaveSlotsScreen.Mode.SAVE))
            loadButton -> game.setScreen(SaveSlotsScreen(game, SaveSlotsScreen.Mode.LOAD))
            back -> onBack()
        }
    }

    override fun onBack() {
        game.setScreen(MainMenuScreen(game))
    }

    override fun drawTexts() {
        game.drawText("OPTIONS", worldWidth / 2f, worldHeight * 0.82f, 4.2f, Palette.GOLD)
        game.drawText("FPS", worldWidth / 2f, worldHeight * 0.6f + 62f, 1.6f)
        game.drawText(game.settings.fpsLabel, worldWidth / 2f, worldHeight * 0.6f, 3.2f)
    }
}
