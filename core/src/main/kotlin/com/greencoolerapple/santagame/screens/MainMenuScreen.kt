package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.awards.Award
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.ui.Button

class MainMenuScreen(game: SantaGame) : MenuBase(game) {
    private val gameModeButton = Button("", labelScale = 2.8f)
    private val awardsButton = Button("", fill = Palette.BUTTON_GOLD, edge = Palette.BUTTON_GOLD_EDGE, labelScale = 2.6f, sublabelColor = Palette.TEXT)
    private val optionsButton = Button("", fill = Palette.BUTTON_ALT, edge = Palette.BUTTON_ALT_EDGE, labelScale = 2.6f)

    override val buttons = listOf(gameModeButton, awardsButton, optionsButton)

    override fun layout() {
        gameModeButton.label = I18n.t("menu.gameMode")
        awardsButton.label = I18n.t("menu.awards")
        awardsButton.sublabel = "${game.awards.unlockedCount} / ${Award.entries.size}"
        optionsButton.label = I18n.t("menu.options")
        gameModeButton.place(worldWidth / 2f, worldHeight * 0.6f, 460f, 100f)
        awardsButton.place(worldWidth / 2f, worldHeight * 0.44f, 460f, 92f)
        optionsButton.place(worldWidth / 2f, worldHeight * 0.29f, 460f, 84f)
    }

    override fun onTap(button: Button) {
        when (button) {
            gameModeButton -> game.setScreen(GameModeScreen(game))
            awardsButton -> game.setScreen(AwardsScreen(game))
            optionsButton -> game.setScreen(OptionsScreen(game))
        }
    }

    override fun onBack() {
        Gdx.app.exit()
    }

    override fun drawTexts() {
        game.drawText("SANTA GAME", worldWidth / 2f, worldHeight * 0.83f, 5f, Palette.GOLD)
    }
}
