package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.awards.Award
import com.greencoolerapple.santagame.awards.AwardTracker
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.input.Touches
import com.greencoolerapple.santagame.ui.Button
import com.greencoolerapple.santagame.ui.Draw

class AwardsScreen(game: SantaGame) : MenuBase(game) {
    private val back = backButton()
    private val clip = Rectangle()
    private val scissor = Rectangle()
    private val awards = Award.entries

    private var scroll = 0f
    private var velocity = 0f
    private var dragPointer = -1
    private var lastY = 0f

    override val buttons = listOf(back)

    override val showSanta: Boolean = false

    private val rows: Int
        get() = (awards.size + COLUMNS - 1) / COLUMNS

    private val maxScroll: Float
        get() = maxOf(0f, rows * (CARD_HEIGHT + GAP) - GAP - clip.height)

    override fun layout() {
        clip.set(0f, worldHeight * 0.17f, worldWidth, worldHeight * 0.59f)
        back.place(worldWidth / 2f, worldHeight * 0.08f, 260f, 64f)
        updateScroll(Gdx.graphics.deltaTime)
    }

    private fun updateScroll(delta: Float) {
        val touches = game.touches
        if (dragPointer >= 0 && !touches.down[dragPointer]) dragPointer = -1
        if (dragPointer < 0) {
            for (i in 0 until Touches.MAX_POINTERS) {
                if (touches.justDown[i] && clip.contains(touches.x[i], touches.y[i])) {
                    dragPointer = i
                    lastY = touches.y[i]
                    velocity = 0f
                    break
                }
            }
        }
        if (dragPointer >= 0) {
            val y = touches.y[dragPointer]
            val moved = y - lastY
            lastY = y
            scroll += moved
            if (delta > 0f) velocity = MathUtils.lerp(velocity, moved / delta, 0.4f)
        } else {
            scroll += velocity * delta
            velocity *= maxOf(0f, 1f - delta * 5f)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) scroll += KEY_SPEED * delta
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) scroll -= KEY_SPEED * delta
        scroll = MathUtils.clamp(scroll, 0f, maxScroll)
    }

    private fun cardX(column: Int): Float {
        val total = COLUMNS * CARD_WIDTH + (COLUMNS - 1) * GAP
        return worldWidth / 2f - total / 2f + column * (CARD_WIDTH + GAP)
    }

    private fun cardY(row: Int): Float = clip.y + clip.height + scroll - row * (CARD_HEIGHT + GAP) - CARD_HEIGHT

    private inline fun forEachVisibleCard(action: (Award, Float, Float) -> Unit) {
        for ((i, award) in awards.withIndex()) {
            val x = cardX(i % COLUMNS)
            val y = cardY(i / COLUMNS)
            if (y > clip.y + clip.height || y + CARD_HEIGHT < clip.y) continue
            action(award, x, y)
        }
    }

    override fun drawContentShapes(shapes: ShapeRenderer) {
        shapes.color = Palette.OVERLAY
        shapes.rect(0f, 0f, worldWidth, worldHeight)

        shapes.flush()
        game.viewport.calculateScissors(shapes.transformMatrix, clip, scissor)
        if (ScissorStack.pushScissors(scissor)) {
            forEachVisibleCard { award, x, y ->
                val unlocked = game.awards.isUnlocked(award)
                shapes.color = if (unlocked) Palette.GOLD else Palette.CARD_EDGE
                Draw.roundedRect(shapes, x - 3f, y - 3f, CARD_WIDTH + 6f, CARD_HEIGHT + 6f, 22f)
                shapes.color = if (unlocked) Palette.CARD_UNLOCKED else Palette.CARD
                Draw.roundedRect(shapes, x, y, CARD_WIDTH, CARD_HEIGHT, 20f)
                if (unlocked) {
                    AwardTracker.drawTrophy(shapes, x + 52f, y + CARD_HEIGHT / 2f, 0.95f)
                } else {
                    AwardTracker.drawLock(shapes, x + 52f, y + CARD_HEIGHT / 2f)
                }
            }
            shapes.flush()
            ScissorStack.popScissors()
        }

        if (maxScroll > 0f) {
            val trackX = cardX(COLUMNS - 1) + CARD_WIDTH + 18f
            shapes.color = Palette.JOYSTICK_BASE
            shapes.rect(trackX, clip.y, 6f, clip.height)
            val visible = clip.height / (clip.height + maxScroll)
            val thumb = clip.height * visible
            val thumbY = clip.y + (clip.height - thumb) * (1f - scroll / maxScroll)
            shapes.color = Palette.GOLD
            shapes.rect(trackX, thumbY, 6f, thumb)
        }
    }

    override fun drawTexts() {
        val unlocked = game.awards.unlockedCount
        game.drawText(I18n.t("awards.title"), worldWidth / 2f, worldHeight * 0.88f, 4.2f, Palette.GOLD)
        game.drawText("$unlocked / ${awards.size}", worldWidth / 2f, worldHeight * 0.8f, 1.9f)

        val batch = game.batch
        batch.flush()
        game.viewport.calculateScissors(batch.transformMatrix, clip, scissor)
        if (ScissorStack.pushScissors(scissor)) {
            forEachVisibleCard { award, x, y ->
                val isUnlocked = game.awards.isUnlocked(award)
                game.drawText(award.title, x + 98f, y + CARD_HEIGHT - 18f, 1.8f, if (isUnlocked) Palette.GOLD else Palette.TEXT_DIM, centered = false)
                game.drawText(award.description, x + 98f, y + CARD_HEIGHT - 54f, 1.05f, if (isUnlocked) Palette.TEXT else Palette.TEXT_DIM, centered = false)
            }
            batch.flush()
            ScissorStack.popScissors()
        }
    }

    override fun onTap(button: Button) {
        if (button == back) onBack()
    }

    override fun onBack() {
        game.setScreen(MainMenuScreen(game))
    }

    companion object {
        private const val COLUMNS = 2
        private const val CARD_WIDTH = 560f
        private const val CARD_HEIGHT = 92f
        private const val GAP = 18f
        private const val KEY_SPEED = 700f
    }
}
