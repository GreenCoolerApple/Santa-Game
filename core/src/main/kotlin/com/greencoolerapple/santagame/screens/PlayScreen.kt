package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.ui.PauseMenu

abstract class PlayScreen(game: SantaGame) : BaseScreen(game) {
    protected val pauseMenu = PauseMenu(game)

    protected abstract val canPause: Boolean

    protected abstract fun update(delta: Float)

    protected abstract fun drawWorld(shapes: ShapeRenderer)

    protected abstract fun drawHud()

    protected abstract fun restartFromPause()

    protected open fun cameraShake(): Float = 0f

    protected open fun worldProjection(shakeX: Float, shakeY: Float): Matrix4? = null

    protected open fun drawOverlay(shapes: ShapeRenderer) {}

    override fun render(delta: Float) {
        val dt = minOf(delta, 1f / 20f)
        game.touches.update(game.viewport)
        pauseMenu.layout(worldWidth, worldHeight)

        when (pauseMenu.update(dt, canPause, game.isMobile)) {
            PauseMenu.Action.MENU -> {
                game.setScreen(MainMenuScreen(game))
                return
            }
            PauseMenu.Action.RESTART -> restartFromPause()
            PauseMenu.Action.NONE -> Unit
        }
        if (!pauseMenu.paused) update(dt)
        if (!canPause && pauseMenu.paused) pauseMenu.forceClose()
        game.music.duck(
            when {
                pauseMenu.paused -> PAUSED_MUSIC
                canPause -> 1f
                else -> RESULTS_MUSIC
            }
        )

        val shake = if (pauseMenu.paused) 0f else cameraShake()
        val shakeX = MathUtils.random(-shake, shake)
        val shakeY = MathUtils.random(-shake, shake)
        val world = worldProjection(shakeX, shakeY)
        if (world == null) beginFrame(shakeX, shakeY) else beginFrame()

        val shapes = game.shapes
        if (world != null) shapes.projectionMatrix = world
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawWorld(shapes)
        if (world != null) {
            shapes.end()
            shapes.projectionMatrix = game.camera.combined
            shapes.begin(ShapeRenderer.ShapeType.Filled)
        }
        drawOverlay(shapes)
        if (canPause && game.isMobile && !pauseMenu.paused) pauseMenu.drawButton(shapes)
        shapes.end()

        game.batch.begin()
        drawHud()
        game.batch.end()

        if (pauseMenu.paused) {
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            pauseMenu.drawOverlay(shapes)
            shapes.end()
            game.batch.begin()
            pauseMenu.drawOverlayText()
            game.batch.end()
        }
    }

    override fun pause() {
        if (canPause) pauseMenu.open()
    }

    companion object {
        private const val PAUSED_MUSIC = 0.35f
        private const val RESULTS_MUSIC = 0.6f
    }
}
