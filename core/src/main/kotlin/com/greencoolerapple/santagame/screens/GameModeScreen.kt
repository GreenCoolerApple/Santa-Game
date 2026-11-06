package com.greencoolerapple.santagame.screens

import com.greencoolerapple.santagame.GameMode
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.ui.Button

class GameModeScreen(game: SantaGame) : MenuBase(game) {
    private val catchButton = Button("", labelScale = 2.4f)
    private val stolenButton = Button("", fill = Palette.BUTTON_ALT, edge = Palette.BUTTON_ALT_EDGE, labelScale = 2.4f)
    private val escapeButton = Button("", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 2.4f)
    private val back = backButton()

    override val buttons = listOf(catchButton, stolenButton, escapeButton, back)

    override fun layout() {
        catchButton.place(worldWidth / 2f - 240f, worldHeight * 0.6f, 440f, 104f)
        stolenButton.place(worldWidth / 2f + 240f, worldHeight * 0.6f, 440f, 104f)
        escapeButton.place(worldWidth / 2f, worldHeight * 0.41f, 920f, 104f)
        back.place(worldWidth / 2f, worldHeight * 0.2f, 260f, 76f)

        catchButton.label = GameMode.CATCH_GIFTS.title
        stolenButton.label = GameMode.STOLEN_GIFTS.title
        escapeButton.label = GameMode.ESCAPE_CITY.title
        catchButton.sublabel = I18n.t("best", game.bestScore(GameMode.CATCH_GIFTS))
        val data = game.data
        stolenButton.sublabel = if (data.hasRun) {
            I18n.t("continueHouse", data.runHouse)
        } else {
            I18n.t("best", game.bestScore(GameMode.STOLEN_GIFTS))
        }
        escapeButton.sublabel = I18n.t("best", game.bestScore(GameMode.ESCAPE_CITY))
    }

    override fun onTap(button: Button) {
        when (button) {
            catchButton -> game.setScreen(CatchGiftsScreen(game))
            stolenButton -> game.setScreen(StolenGiftsScreen(game))
            escapeButton -> game.setScreen(EscapeCityScreen(game))
            back -> onBack()
        }
    }

    override fun onBack() {
        game.setScreen(MainMenuScreen(game))
    }

    override fun drawTexts() {
        game.drawText(I18n.t("gameMode.title"), worldWidth / 2f, worldHeight * 0.82f, 4.2f, Palette.GOLD)
    }
}
