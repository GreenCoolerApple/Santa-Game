package com.greencoolerapple.santagame.screens

import com.greencoolerapple.santagame.GameMode
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.ui.Button

class GameModeScreen(game: SantaGame) : MenuBase(game) {
    private val catchButton = Button(GameMode.CATCH_GIFTS.title, labelScale = 2.6f)
    private val stolenButton = Button(GameMode.STOLEN_GIFTS.title, fill = Palette.BUTTON_ALT, edge = Palette.BUTTON_ALT_EDGE, labelScale = 2.6f)
    private val back = backButton()

    override val buttons = listOf(catchButton, stolenButton, back)

    override fun layout() {
        catchButton.place(worldWidth / 2f - 250f, worldHeight * 0.55f, 440f, 110f)
        stolenButton.place(worldWidth / 2f + 250f, worldHeight * 0.55f, 440f, 110f)
        back.place(worldWidth / 2f, worldHeight * 0.24f, 260f, 76f)
        catchButton.sublabel = "Best: ${game.bestScore(GameMode.CATCH_GIFTS)}"
        val data = game.data
        stolenButton.sublabel = if (data.hasRun) "Continue: House ${data.runHouse}" else "Best: ${game.bestScore(GameMode.STOLEN_GIFTS)}"
    }

    override fun onTap(button: Button) {
        when (button) {
            catchButton -> game.setScreen(CatchGiftsScreen(game))
            stolenButton -> game.setScreen(StolenGiftsScreen(game))
            back -> onBack()
        }
    }

    override fun onBack() {
        game.setScreen(MainMenuScreen(game))
    }

    override fun drawTexts() {
        game.drawText("GAME MODE", worldWidth / 2f, worldHeight * 0.8f, 4.2f, Palette.GOLD)
    }
}
