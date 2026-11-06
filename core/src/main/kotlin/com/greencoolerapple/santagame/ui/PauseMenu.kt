package com.greencoolerapple.santagame.ui

import com.greencoolerapple.santagame.i18n.I18n
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.haptics.Haptics

class PauseMenu(private val game: SantaGame) {
    enum class Action { NONE, MENU }

    var paused = false

    val pauseButton = Rectangle()

    private val hitArea = Rectangle()
    private val resumeButton = Button("Resume")
    private val menuButton = Button("Menu")

    fun layout(width: Float, height: Float) {
        pauseButton.set(width - SIZE - MARGIN, height - SIZE - MARGIN, SIZE, SIZE)
        resumeButton.label = I18n.t("pause.resume")
        menuButton.label = I18n.t("pause.menu")
        hitArea.set(pauseButton.x - 20f, pauseButton.y - 20f, SIZE + 40f, SIZE + 40f)
        resumeButton.place(width / 2f, height * 0.48f, 380f, 92f)
        menuButton.place(width / 2f, height * 0.32f, 380f, 92f)
    }

    fun update(canPause: Boolean): Action {
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            when {
                paused -> paused = false
                canPause -> paused = true
                else -> return Action.MENU
            }
        }

        val touches = game.touches
        val pointer = touches.firstJustDown()
        if (pointer < 0) return Action.NONE
        val x = touches.x[pointer]
        val y = touches.y[pointer]

        if (paused) {
            touches.claim(pointer)
            if (resumeButton.contains(x, y)) {
                game.haptics.play(Haptics.Pattern.TAP)
                paused = false
            }
            if (menuButton.contains(x, y)) {
                game.haptics.play(Haptics.Pattern.TAP)
                return Action.MENU
            }
        } else if (canPause && hitArea.contains(x, y)) {
            touches.claim(pointer)
            game.haptics.play(Haptics.Pattern.TAP)
            paused = true
        }
        return Action.NONE
    }

    fun drawButton(shapes: ShapeRenderer) {
        val cx = pauseButton.x + SIZE / 2f
        val cy = pauseButton.y + SIZE / 2f
        shapes.color = Palette.PAUSE_BG
        shapes.circle(cx, cy, SIZE / 2f, 32)
        shapes.color = Palette.TEXT
        shapes.rect(cx - 13f, cy - 15f, 9f, 30f)
        shapes.rect(cx + 4f, cy - 15f, 9f, 30f)
    }

    fun drawOverlay(shapes: ShapeRenderer, width: Float, height: Float) {
        shapes.color = Palette.OVERLAY
        shapes.rect(-20f, -20f, width + 40f, height + 40f)
        resumeButton.drawShape(shapes)
        menuButton.drawShape(shapes)
    }

    fun drawOverlayText(width: Float, height: Float) {
        game.drawText(I18n.t("pause.title"), width / 2f, height * 0.7f, 4.5f, Palette.GOLD)
        resumeButton.drawLabel(game, 2.4f)
        menuButton.drawLabel(game, 2.4f)
    }

    companion object {
        const val SIZE = 72f
        const val MARGIN = 20f
    }
}
