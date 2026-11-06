package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.ui.Button
import com.greencoolerapple.santagame.world.Santa

abstract class MenuBase(game: SantaGame) : BaseScreen(game) {
    private val santa = Santa(SantaGame.WORLD_WIDTH * 0.12f, SantaGame.GROUND_HEIGHT - 4f)
    private var toast = ""
    private var toastTimer = 0f

    protected var time = 0f
        private set

    protected abstract val buttons: List<Button>

    protected abstract fun layout()

    protected abstract fun onTap(button: Button)

    protected abstract fun onBack()

    protected open fun drawTexts() {}

    protected fun showToast(message: String) {
        toast = message
        toastTimer = TOAST_TIME
    }

    protected fun backButton(): Button = Button("Back", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 2f)

    override fun render(delta: Float) {
        time += delta
        toastTimer = maxOf(0f, toastTimer - delta)
        game.scenery.update(delta)
        game.touches.update(game.viewport)
        santa.x = worldWidth * 0.12f
        santa.idle(delta)
        layout()

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            onBack()
            return
        }

        val pointer = game.touches.firstJustDown()
        if (pointer >= 0) {
            game.touches.claim(pointer)
            val x = game.touches.x[pointer]
            val y = game.touches.y[pointer]
            val hit = buttons.firstOrNull { it.contains(x, y) }
            if (hit != null) {
                onTap(hit)
                if (game.screen !== this) return
            }
        }

        beginFrame()

        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        game.scenery.drawBackground(shapes, worldWidth, worldHeight)
        santa.draw(shapes)
        game.scenery.drawSnow(shapes, worldWidth)
        for (button in buttons) button.drawShape(shapes)
        shapes.end()

        game.batch.begin()
        drawTexts()
        for (button in buttons) button.drawLabel(game)
        if (toastTimer > 0f) {
            game.drawText(toast, worldWidth / 2f, 46f, 1.8f, Palette.GOLD, alpha = minOf(1f, toastTimer * 2f))
        }
        game.batch.end()
    }

    companion object {
        private const val TOAST_TIME = 2.2f
    }
}
