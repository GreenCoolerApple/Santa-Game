package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.GameMode
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.input.Touches
import com.greencoolerapple.santagame.world.FallingItem
import com.greencoolerapple.santagame.world.Santa
import com.greencoolerapple.santagame.world.Sparkles

class CatchGiftsScreen(game: SantaGame) : PlayScreen(game) {
    private enum class State { PLAYING, GAME_OVER }

    private val santa = Santa(SantaGame.WORLD_WIDTH / 2f, SantaGame.GROUND_HEIGHT - 4f)
    private val items = mutableListOf<FallingItem>()
    private val sparkles = Sparkles()

    private var state = State.PLAYING
    private var score = 0
    private var lives = MAX_LIVES
    private var elapsed = 0f
    private var spawnTimer = 0.6f
    private var overTimer = 0f
    private var shakeTimer = 0f
    private var newBest = false

    override val canPause: Boolean
        get() = state == State.PLAYING

    override fun show() {
        santa.x = worldWidth / 2f
        santa.targetX = santa.x
    }

    override fun cameraShake(): Float = if (shakeTimer > 0f) shakeTimer / SHAKE_DURATION * SHAKE_STRENGTH else 0f

    override fun update(delta: Float) {
        game.scenery.update(delta)
        sparkles.update(delta)
        shakeTimer = maxOf(0f, shakeTimer - delta)

        when (state) {
            State.PLAYING -> updatePlaying(delta)
            State.GAME_OVER -> {
                santa.idle(delta)
                overTimer += delta
                if (overTimer > RESTART_DELAY && (game.touches.firstJustDown() >= 0 || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
                    restart()
                }
            }
        }
    }

    private fun updatePlaying(delta: Float) {
        elapsed += delta

        val touches = game.touches
        for (i in 0 until Touches.MAX_POINTERS) {
            if (touches.isFree(i)) {
                santa.targetX = touches.x[i]
                break
            }
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            santa.targetX = santa.x - KEYBOARD_SPEED * delta
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            santa.targetX = santa.x + KEYBOARD_SPEED * delta
        }
        santa.update(delta, 0f, worldWidth)

        spawnTimer -= delta
        if (spawnTimer <= 0f) {
            spawnItem()
            spawnTimer = maxOf(MIN_SPAWN_INTERVAL, START_SPAWN_INTERVAL - elapsed * 0.012f)
        }

        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            item.update(delta)
            if (item.bounds.overlaps(santa.catchBounds)) {
                iterator.remove()
                onCaught(item)
                if (state != State.PLAYING) return
            } else if (item.y + item.size / 2f < SantaGame.GROUND_HEIGHT - 6f) {
                iterator.remove()
                onLanded(item)
                if (state != State.PLAYING) return
            }
        }
    }

    private fun spawnItem() {
        val coalChance = minOf(MAX_COAL_CHANCE, 0.15f + elapsed * 0.004f)
        val type = if (MathUtils.random() < coalChance) FallingItem.Type.COAL else FallingItem.Type.GIFT
        val speed = minOf(MAX_FALL_SPEED, MathUtils.random(220f, 300f) + elapsed * 6f)
        val x = MathUtils.random(50f, worldWidth - 50f)
        items += FallingItem(type, x, worldHeight + 50f, speed)
    }

    private fun onCaught(item: FallingItem) {
        when (item.type) {
            FallingItem.Type.GIFT -> {
                score++
                sparkles.burst(item.x, item.y, Palette.GOLD)
            }
            FallingItem.Type.COAL -> {
                sparkles.burst(item.x, item.y, Palette.COAL_SHINE, 22)
                loseLife()
            }
        }
    }

    private fun onLanded(item: FallingItem) {
        when (item.type) {
            FallingItem.Type.GIFT -> {
                sparkles.burst(item.x, SantaGame.GROUND_HEIGHT, Palette.HEART, 12)
                loseLife()
            }
            FallingItem.Type.COAL -> sparkles.burst(item.x, SantaGame.GROUND_HEIGHT, Palette.SNOW, 10)
        }
    }

    private fun loseLife() {
        lives--
        shakeTimer = SHAKE_DURATION
        Gdx.input.vibrate(80)
        if (lives <= 0) {
            state = State.GAME_OVER
            overTimer = 0f
            newBest = game.submitScore(GameMode.CATCH_GIFTS, score)
        }
    }

    private fun restart() {
        items.clear()
        sparkles.clear()
        score = 0
        lives = MAX_LIVES
        elapsed = 0f
        spawnTimer = 0.6f
        newBest = false
        state = State.PLAYING
    }

    override fun drawWorld(shapes: ShapeRenderer) {
        game.scenery.drawBackground(shapes, worldWidth, worldHeight)
        for (item in items) item.draw(shapes)
        santa.draw(shapes)
        sparkles.draw(shapes)
        game.scenery.drawSnow(shapes, worldWidth)
        drawLives(shapes)
        if (state == State.GAME_OVER) {
            shapes.color = Palette.OVERLAY
            shapes.rect(-SHAKE_STRENGTH, -SHAKE_STRENGTH, worldWidth + SHAKE_STRENGTH * 2f, worldHeight + SHAKE_STRENGTH * 2f)
        }
    }

    override fun drawHud() {
        game.drawText("Score: $score", 28f, worldHeight - 28f, 2.4f, centered = false)
        game.drawText("Best: ${game.bestScore(GameMode.CATCH_GIFTS)}", 28f, worldHeight - 78f, 1.6f, centered = false)
        if (state == State.GAME_OVER) {
            game.drawText("GAME OVER", worldWidth / 2f, worldHeight * 0.66f, 4.5f, Palette.HEART)
            game.drawText("Gifts saved: $score", worldWidth / 2f, worldHeight * 0.53f, 2.2f)
            if (newBest) game.drawText("New best!", worldWidth / 2f, worldHeight * 0.45f, 2f, Palette.GOLD)
            if (overTimer > RESTART_DELAY) game.drawText("Tap to play again", worldWidth / 2f, worldHeight * 0.34f, 2f)
        }
    }

    private fun drawLives(shapes: ShapeRenderer) {
        val size = 36f
        val gap = 14f
        val top = worldHeight - 56f
        val right = pauseMenu.pauseButton.x - 36f
        for (i in 0 until MAX_LIVES) {
            val cx = right - i * (size + gap)
            shapes.color = if (i < lives) Palette.HEART else Palette.HEART_EMPTY
            shapes.circle(cx - size * 0.25f, top, size * 0.28f, 16)
            shapes.circle(cx + size * 0.25f, top, size * 0.28f, 16)
            shapes.triangle(cx - size * 0.52f, top - size * 0.08f, cx + size * 0.52f, top - size * 0.08f, cx, top - size * 0.62f)
        }
    }

    companion object {
        private const val MAX_LIVES = 3
        private const val START_SPAWN_INTERVAL = 1.1f
        private const val MIN_SPAWN_INTERVAL = 0.35f
        private const val MAX_COAL_CHANCE = 0.4f
        private const val MAX_FALL_SPEED = 720f
        private const val KEYBOARD_SPEED = 900f
        private const val RESTART_DELAY = 0.8f
        private const val SHAKE_DURATION = 0.3f
        private const val SHAKE_STRENGTH = 10f
    }
}
