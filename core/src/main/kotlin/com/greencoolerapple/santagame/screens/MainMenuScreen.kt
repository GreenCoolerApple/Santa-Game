package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.awards.Award
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.ui.Button
import com.greencoolerapple.santagame.ui.Draw

class MainMenuScreen(game: SantaGame) : MenuBase(game) {
    private val gameModeButton = Button("", labelScale = 2.8f)
    private val awardsButton = Button("", fill = Palette.BUTTON_GOLD, edge = Palette.BUTTON_GOLD_EDGE, labelScale = 2.1f, sublabelColor = Palette.TEXT)
    private val shopButton = Button("", fill = Palette.BUTTON_SHOP, edge = Palette.BUTTON_SHOP_EDGE, labelScale = 2.1f, sublabelColor = Palette.GOLD_LIGHT)
    private val optionsButton = Button("", fill = Palette.BUTTON_ALT, edge = Palette.BUTTON_ALT_EDGE, labelScale = 2.4f)

    override val buttons = listOf(gameModeButton, awardsButton, shopButton, optionsButton)

    override fun layout() {
        gameModeButton.label = I18n.t("menu.gameMode")
        awardsButton.label = I18n.t("menu.awards")
        awardsButton.sublabel = "${game.awards.unlockedCount} / ${Award.entries.size}"
        shopButton.label = I18n.t("menu.shop")
        shopButton.sublabel = I18n.t("shop.balance", game.data.coins)
        optionsButton.label = I18n.t("menu.options")
        gameModeButton.place(worldWidth / 2f, worldHeight * 0.63f, 480f, 100f)
        awardsButton.place(worldWidth / 2f - 125f, worldHeight * 0.465f, 230f, 92f)
        shopButton.place(worldWidth / 2f + 125f, worldHeight * 0.465f, 230f, 92f)
        optionsButton.place(worldWidth / 2f, worldHeight * 0.31f, 480f, 80f)
    }

    override fun onTap(button: Button) {
        when (button) {
            gameModeButton -> game.setScreen(GameModeScreen(game))
            awardsButton -> game.setScreen(AwardsScreen(game))
            shopButton -> game.setScreen(ShopScreen(game))
            optionsButton -> game.setScreen(OptionsScreen(game))
        }
    }

    override fun onBack() {
        Gdx.app.exit()
    }

    override fun drawContentShapes(shapes: ShapeRenderer) {
        val width = game.textWidth(game.data.coins.toString(), 2.2f)
        Draw.goldenGift(shapes, worldWidth - 44f - width - 30f, worldHeight - 46f, 32f)
    }

    override fun drawTexts() {
        game.drawText("SANTA GAME", worldWidth / 2f, worldHeight * 0.84f, 5f, Palette.GOLD)
        game.drawText(game.data.coins.toString(), worldWidth - 32f - game.textWidth(game.data.coins.toString(), 2.2f) / 2f, worldHeight - 42f, 2.2f, Palette.GOLD_LIGHT)
    }
}
