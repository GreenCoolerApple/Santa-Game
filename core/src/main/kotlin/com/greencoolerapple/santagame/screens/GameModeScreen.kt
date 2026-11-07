package com.greencoolerapple.santagame.screens

import com.greencoolerapple.santagame.GameMode
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.ui.Button

class GameModeScreen(game: SantaGame) : MenuBase(game) {
    private val catchButton = Button("", labelScale = LABEL_SCALE)
    private val stolenButton = Button("", fill = Palette.BUTTON_ALT, edge = Palette.BUTTON_ALT_EDGE, labelScale = LABEL_SCALE)
    private val escapeButton = Button("", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = LABEL_SCALE)
    private val sledButton = Button("", fill = Palette.BUTTON_GOLD, edge = Palette.BUTTON_GOLD_EDGE, labelScale = LABEL_SCALE)
    private val back = backButton()

    override val buttons = listOf(catchButton, stolenButton, escapeButton, sledButton, back)

    override fun layout() {
        val left = worldWidth / 2f - COLUMN_OFFSET
        val right = worldWidth / 2f + COLUMN_OFFSET
        catchButton.place(left, worldHeight * 0.6f, BUTTON_WIDTH, BUTTON_HEIGHT)
        stolenButton.place(right, worldHeight * 0.6f, BUTTON_WIDTH, BUTTON_HEIGHT)
        escapeButton.place(left, worldHeight * 0.41f, BUTTON_WIDTH, BUTTON_HEIGHT)
        sledButton.place(right, worldHeight * 0.41f, BUTTON_WIDTH, BUTTON_HEIGHT)
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
        sledButton.label = GameMode.SLED_RACE.title
        sledButton.sublabel = I18n.t("best", SledRaceScreen.formatTime(game.bestScore(GameMode.SLED_RACE)))
    }

    override fun onTap(button: Button) {
        when (button) {
            catchButton -> game.setScreen(CatchGiftsScreen(game))
            stolenButton -> game.setScreen(StolenGiftsScreen(game))
            escapeButton -> game.setScreen(EscapeCityScreen(game))
            sledButton -> game.setScreen(SledRaceScreen(game))
            back -> onBack()
        }
    }

    override fun onBack() {
        game.setScreen(MainMenuScreen(game))
    }

    override fun drawTexts() {
        game.drawText(I18n.t("gameMode.title"), worldWidth / 2f, worldHeight * 0.82f, 4.2f, Palette.GOLD)
    }

    companion object {
        private const val LABEL_SCALE = 2.1f
        private const val BUTTON_WIDTH = 560f
        private const val BUTTON_HEIGHT = 112f
        private const val COLUMN_OFFSET = 295f
    }
}
