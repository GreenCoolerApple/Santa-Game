package com.greencoolerapple.santagame.screens

import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.ui.Button

class OptionsScreen(game: SantaGame) : MenuBase(game) {
    private class Row(val labelKey: String, val value: () -> String, val change: (Int) -> Unit) {
        val down = Button("<", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 2.6f)
        val up = Button(">", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 2.6f)
        var y = 0f
    }

    private val rows = listOf(
        Row("options.fps", { game.settings.fpsLabel }, { game.changeFps(it) }),
        Row("options.language", { game.settings.language.label }, { game.changeLanguage(it) }),
        Row("options.music", { game.settings.musicLabel }, { game.changeMusicVolume(it) }),
        Row("options.haptics", { game.settings.hapticsLabel }, { game.toggleHaptics() }),
        Row("options.hapticsIntensity", { game.settings.intensityLabel }, { game.changeHapticIntensity(it) })
    )
    private val saveButton = Button("", labelScale = 2.2f)
    private val loadButton = Button("", fill = Palette.BUTTON_ALT, edge = Palette.BUTTON_ALT_EDGE, labelScale = 2.2f)
    private val controlsButton = Button("", fill = Palette.BUTTON_SHOP, edge = Palette.BUTTON_SHOP_EDGE, labelScale = 2.2f)
    private val back = backButton()

    override val buttons = rows.flatMap { listOf(it.down, it.up) } +
        listOf(saveButton, loadButton) +
        (if (game.isDesktop) listOf(controlsButton) else emptyList()) +
        listOf(back)

    override val showSanta: Boolean = false

    override fun layout() {
        for ((i, row) in rows.withIndex()) {
            row.y = worldHeight * (0.765f - i * 0.1f)
            row.down.place(worldWidth / 2f + 10f, row.y, 70f, 56f)
            row.up.place(worldWidth / 2f + 430f, row.y, 70f, 56f)
        }
        if (game.isDesktop) {
            saveButton.place(worldWidth / 2f - 310f, worldHeight * 0.215f, 290f, 76f)
            loadButton.place(worldWidth / 2f, worldHeight * 0.215f, 290f, 76f)
            controlsButton.place(worldWidth / 2f + 310f, worldHeight * 0.215f, 290f, 76f)
            controlsButton.label = I18n.t("options.controls")
        } else {
            saveButton.place(worldWidth / 2f - 200f, worldHeight * 0.215f, 360f, 76f)
            loadButton.place(worldWidth / 2f + 200f, worldHeight * 0.215f, 360f, 76f)
        }
        back.place(worldWidth / 2f, worldHeight * 0.08f, 260f, 62f)
        saveButton.label = I18n.t("options.save")
        loadButton.label = I18n.t("options.load")
    }

    override fun onTap(button: Button) {
        for (row in rows) {
            if (button == row.down) {
                row.change(-1)
                return
            }
            if (button == row.up) {
                row.change(1)
                return
            }
        }
        when (button) {
            saveButton -> game.setScreen(SaveSlotsScreen(game, SaveSlotsScreen.Mode.SAVE))
            loadButton -> game.setScreen(SaveSlotsScreen(game, SaveSlotsScreen.Mode.LOAD))
            controlsButton -> game.setScreen(ControlsScreen(game))
            back -> onBack()
        }
    }

    override fun onBack() {
        game.setScreen(MainMenuScreen(game))
    }

    override fun drawTexts() {
        game.drawText(I18n.t("options.title"), worldWidth / 2f, worldHeight * 0.9f, 3.8f, Palette.GOLD)
        for (row in rows) {
            game.drawText(I18n.t(row.labelKey), worldWidth / 2f - 440f, row.y + 14f, 1.9f, Palette.GOLD, centered = false)
            game.drawText(row.value(), worldWidth / 2f + 220f, row.y, 2.2f)
        }
    }
}
