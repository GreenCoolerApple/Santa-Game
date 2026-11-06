package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.ui.PauseMenu

abstract class PlayScreen(game: SantaGame) : BaseScreen(game) {
    protected val pauseMenu = PauseMenu(game)

    protected abstract val canPause: Boolean

    protected abstract fun update(delta: Float)

    protected abstract fun drawWorld(shapes: ShapeRenderer)

    protected abstract fun drawHud()

    protected open fun cameraShake(): Float = 0f

    override fun render(delta: Float) {
        val dt = minOf(delta, 1f / 20f)
        game.touches.update(game.viewport)
        pauseMenu.layout(worldWidth, worldHeight)

        if (pauseMenu.update(canPause) == PauseMenu.Action.MENU) {
            game.setScreen(MainMenuScreen(game))
            return
        }
        if (!pauseMenu.paused) update(dt)
        if (!canPause) pauseMenu.paused = false

        val shake = if (pauseMenu.paused) 0f else cameraShake()
        beginFrame(MathUtils.random(-shake, shake), MathUtils.random(-shake, shake))

        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawWorld(shapes)
        if (canPause) pauseMenu.drawButton(shapes)
        shapes.end()

        game.batch.begin()
        drawHud()
        game.batch.end()

        if (pauseMenu.paused) {
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            pauseMenu.drawOverlay(shapes, worldWidth, worldHeight)
            shapes.end()
            game.batch.begin()
            pauseMenu.drawOverlayText(worldWidth, worldHeight)
            game.batch.end()
        }
    }

    override fun pause() {
        if (canPause) pauseMenu.paused = true
    }
}
