package com.greencoolerapple.santagame.screens

import com.greencoolerapple.santagame.i18n.I18n
import com.badlogic.gdx.Gdx
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.ui.Button

class MainMenuScreen(game: SantaGame) : MenuBase(game) {
    private val gameModeButton = Button("Game Mode", labelScale = 2.8f)
    private val optionsButton = Button("Options", fill = Palette.BUTTON_ALT, edge = Palette.BUTTON_ALT_EDGE, labelScale = 2.8f)

    override val buttons = listOf(gameModeButton, optionsButton)

    override fun layout() {
        gameModeButton.label = I18n.t("menu.gameMode")
        optionsButton.label = I18n.t("menu.options")
        gameModeButton.place(worldWidth / 2f, worldHeight * 0.55f, 460f, 100f)
        optionsButton.place(worldWidth / 2f, worldHeight * 0.36f, 460f, 100f)
    }

    override fun onTap(button: Button) {
        when (button) {
            gameModeButton -> game.setScreen(GameModeScreen(game))
            optionsButton -> game.setScreen(OptionsScreen(game))
        }
    }

    override fun onBack() {
        Gdx.app.exit()
    }

    override fun drawTexts() {
        game.drawText("SANTA GAME", worldWidth / 2f, worldHeight * 0.82f, 5f, Palette.GOLD)
    }
}
