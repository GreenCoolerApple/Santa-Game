package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.world.Santa

class MenuScreen(game: SantaGame) : BaseScreen(game) {
    private val santa = Santa(SantaGame.WORLD_WIDTH / 2f, SantaGame.GROUND_HEIGHT - 4f)
    private val pulseColor = Color(Palette.TEXT)
    private var time = 0f

    override fun render(delta: Float) {
        time += delta
        game.scenery.update(delta)
        santa.x = worldWidth / 2f
        santa.idle(delta)

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
            return
        }
        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(GameScreen(game))
            return
        }

        beginFrame()

        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        game.scenery.drawBackground(shapes, worldWidth, worldHeight)
        santa.draw(shapes)
        game.scenery.drawSnow(shapes, worldWidth)
        shapes.end()

        pulseColor.a = 0.55f + 0.45f * (MathUtils.sin(time * 4f) + 1f) / 2f

        val batch = game.batch
        batch.begin()
        game.drawText("SANTA GAME", worldWidth / 2f, worldHeight * 0.78f, 5f, Palette.GOLD)
        game.drawText("Catch the gifts, dodge the coal!", worldWidth / 2f, worldHeight * 0.66f, 1.8f)
        game.drawText("Tap to play", worldWidth / 2f, worldHeight * 0.54f, 2.6f, pulseColor)
        game.drawText("Best: ${game.bestScore}", worldWidth / 2f, worldHeight * 0.45f, 1.7f)
        batch.end()
    }
}
