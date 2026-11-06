package com.greencoolerapple.santagame.screens

import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.ui.Button

class OptionsScreen(game: SantaGame) : MenuBase(game) {
    private val fpsDown = arrow("<")
    private val fpsUp = arrow(">")
    private val languageDown = arrow("<")
    private val languageUp = arrow(">")
    private val saveButton = Button("", labelScale = 2.3f)
    private val loadButton = Button("", fill = Palette.BUTTON_ALT, edge = Palette.BUTTON_ALT_EDGE, labelScale = 2.3f)
    private val back = backButton()

    override val buttons = listOf(fpsDown, fpsUp, languageDown, languageUp, saveButton, loadButton, back)

    private val fpsY: Float
        get() = worldHeight * 0.64f

    private val languageY: Float
        get() = worldHeight * 0.47f

    private fun arrow(symbol: String) = Button(symbol, fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 3f)

    override fun layout() {
        fpsDown.place(worldWidth / 2f - 250f, fpsY, 84f, 72f)
        fpsUp.place(worldWidth / 2f + 250f, fpsY, 84f, 72f)
        languageDown.place(worldWidth / 2f - 250f, languageY, 84f, 72f)
        languageUp.place(worldWidth / 2f + 250f, languageY, 84f, 72f)
        saveButton.place(worldWidth / 2f - 200f, worldHeight * 0.3f, 360f, 90f)
        loadButton.place(worldWidth / 2f + 200f, worldHeight * 0.3f, 360f, 90f)
        back.place(worldWidth / 2f, worldHeight * 0.13f, 260f, 72f)
        saveButton.label = I18n.t("options.save")
        loadButton.label = I18n.t("options.load")
    }

    override fun onTap(button: Button) {
        when (button) {
            fpsDown -> game.changeFps(-1)
            fpsUp -> game.changeFps(1)
            languageDown -> game.changeLanguage(-1)
            languageUp -> game.changeLanguage(1)
            saveButton -> game.setScreen(SaveSlotsScreen(game, SaveSlotsScreen.Mode.SAVE))
            loadButton -> game.setScreen(SaveSlotsScreen(game, SaveSlotsScreen.Mode.LOAD))
            back -> onBack()
        }
    }

    override fun onBack() {
        game.setScreen(MainMenuScreen(game))
    }

    override fun drawTexts() {
        game.drawText(I18n.t("options.title"), worldWidth / 2f, worldHeight * 0.85f, 4.2f, Palette.GOLD)
        game.drawText(I18n.t("options.fps"), worldWidth / 2f, fpsY + 50f, 1.5f, Palette.GOLD)
        game.drawText(game.settings.fpsLabel, worldWidth / 2f, fpsY - 6f, 2.8f)
        game.drawText(I18n.t("options.language"), worldWidth / 2f, languageY + 50f, 1.5f, Palette.GOLD)
        game.drawText(game.settings.language.label, worldWidth / 2f, languageY - 6f, 2.4f)
    }
}
