package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.greencoolerapple.santagame.GameMode
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.ui.Button
import com.greencoolerapple.santagame.world.Santa

class MenuScreen(game: SantaGame) : BaseScreen(game) {
    private val santa = Santa(SantaGame.WORLD_WIDTH / 2f, SantaGame.GROUND_HEIGHT - 4f)
    private val catchButton = Button(GameMode.CATCH_GIFTS.title)
    private val stolenButton = Button(GameMode.STOLEN_GIFTS.title)

    override fun render(delta: Float) {
        game.scenery.update(delta)
        game.touches.update(game.viewport)
        santa.x = worldWidth / 2f
        santa.idle(delta)

        catchButton.place(worldWidth / 2f - 250f, worldHeight * 0.55f, 440f, 110f)
        stolenButton.place(worldWidth / 2f + 250f, worldHeight * 0.55f, 440f, 110f)
        catchButton.sublabel = "Best: ${game.bestScore(GameMode.CATCH_GIFTS)}"
        stolenButton.sublabel = "Best: ${game.bestScore(GameMode.STOLEN_GIFTS)}"

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            game.setScreen(CatchGiftsScreen(game))
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            game.setScreen(StolenGiftsScreen(game))
            return
        }

        val pointer = game.touches.firstJustDown()
        if (pointer >= 0) {
            val x = game.touches.x[pointer]
            val y = game.touches.y[pointer]
            game.touches.claim(pointer)
            if (catchButton.contains(x, y)) {
                game.setScreen(CatchGiftsScreen(game))
                return
            }
            if (stolenButton.contains(x, y)) {
                game.setScreen(StolenGiftsScreen(game))
                return
            }
        }

        beginFrame()

        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        game.scenery.drawBackground(shapes, worldWidth, worldHeight)
        santa.draw(shapes)
        game.scenery.drawSnow(shapes, worldWidth)
        catchButton.drawShape(shapes)
        stolenButton.drawShape(shapes, Palette.BUTTON_ALT, Palette.BUTTON_ALT_EDGE)
        shapes.end()

        val batch = game.batch
        batch.begin()
        game.drawText("SANTA GAME", worldWidth / 2f, worldHeight * 0.83f, 5f, Palette.GOLD)
        game.drawText("Choose a game mode", worldWidth / 2f, worldHeight * 0.71f, 1.8f)
        catchButton.drawLabel(game, 2.6f)
        stolenButton.drawLabel(game, 2.6f)
        batch.end()
    }
}
