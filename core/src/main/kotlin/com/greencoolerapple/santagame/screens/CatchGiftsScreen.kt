package com.greencoolerapple.santagame.screens

import com.greencoolerapple.santagame.i18n.I18n
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.GameMode
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.input.Action
import com.greencoolerapple.santagame.shop.ShopItem
import com.greencoolerapple.santagame.shop.CoinRewards
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.input.Touches
import com.greencoolerapple.santagame.ui.GameOverAnimation
import com.greencoolerapple.santagame.world.FallingItem
import com.greencoolerapple.santagame.world.Santa
import com.greencoolerapple.santagame.world.Sparkles

class CatchGiftsScreen(game: SantaGame) : PlayScreen(game) {
    private enum class State { PLAYING, GAME_OVER }

    private val santa = Santa(SantaGame.WORLD_WIDTH / 2f, SantaGame.GROUND_HEIGHT - 4f)
    private val items = mutableListOf<FallingItem>()
    private val sparkles = Sparkles()
    private val gameOver = GameOverAnimation(game)

    private var state = State.PLAYING
    private var score = 0
    private val maxLives = if (game.hasPerk(ShopItem.PERK_EXTRA_LIFE)) BASE_LIVES + 1 else BASE_LIVES
    private var lives = maxLives
    private var elapsed = 0f
    private var spawnTimer = 0.6f
    private var shakeTimer = 0f

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
                gameOver.update(delta)
                when (gameOver.handleInput()) {
                    GameOverAnimation.Action.RETRY -> restart()
                    GameOverAnimation.Action.MENU -> game.setScreen(GameModeScreen(game))
                    GameOverAnimation.Action.NONE -> Unit
                }
            }
        }
    }

    private fun updatePlaying(delta: Float) {
        elapsed += delta
        game.awards.onCatchProgress(score, elapsed)

        val touches = game.touches
        for (i in 0 until Touches.MAX_POINTERS) {
            if (touches.isFree(i)) {
                santa.targetX = touches.x[i]
                break
            }
        }
        if (game.controls.isPressed(Action.MOVE_LEFT)) {
            santa.targetX = santa.x - KEYBOARD_SPEED * delta
        }
        if (game.controls.isPressed(Action.MOVE_RIGHT)) {
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
                game.haptics.play(Haptics.Pattern.TAP)
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
        game.haptics.play(if (lives <= 0) Haptics.Pattern.CAUGHT else Haptics.Pattern.HIT)
        if (lives <= 0) {
            state = State.GAME_OVER
            val coins = CoinRewards.catchGifts(score, elapsed)
            game.earnCoins(coins)
            val newBest = game.finishCatchGame(score)
            gameOver.start(
                title = I18n.t("catch.gameOver"),
                color = Palette.HEART,
                style = GameOverAnimation.Style.DROP,
                stats = listOf(
                    GameOverAnimation.Stat(I18n.t("catch.statGifts"), score),
                    GameOverAnimation.Stat(I18n.t("catch.statTime"), elapsed.toInt()),
                    GameOverAnimation.Stat(I18n.t("coins.earned"), coins)
                ),
                newBest = newBest,
                confetti = true,
                width = worldWidth,
                height = worldHeight
            )
        }
    }

    override fun restartFromPause() {
        restart()
    }

    private fun restart() {
        items.clear()
        sparkles.clear()
        score = 0
        lives = maxLives
        elapsed = 0f
        spawnTimer = 0.6f
        state = State.PLAYING
    }

    override fun drawWorld(shapes: ShapeRenderer) {
        game.scenery.drawBackground(shapes, worldWidth, worldHeight)
        for (item in items) item.draw(shapes)
        santa.draw(shapes)
        if (state == State.GAME_OVER) santa.drawDizzy(shapes, gameOver.time)
        sparkles.draw(shapes)
        game.scenery.drawSnow(shapes, worldWidth)
        if (state == State.PLAYING) drawLives(shapes)
        if (state == State.GAME_OVER) gameOver.drawShapes(shapes, worldWidth, worldHeight)
    }

    override fun drawHud() {
        if (state == State.GAME_OVER) {
            gameOver.drawText(worldWidth, worldHeight)
            return
        }
        game.drawText(I18n.t("score", score), 28f, worldHeight - 28f, 2.4f, centered = false)
        game.drawText(I18n.t("best", game.bestScore(GameMode.CATCH_GIFTS)), 28f, worldHeight - 78f, 1.6f, centered = false)
    }

    private fun drawLives(shapes: ShapeRenderer) {
        val size = 36f
        val gap = 14f
        val top = worldHeight - 56f
        val right = if (game.isMobile) pauseMenu.pauseButton.x - 36f else worldWidth - 44f
        for (i in 0 until maxLives) {
            val cx = right - i * (size + gap)
            shapes.color = if (i < lives) Palette.HEART else Palette.HEART_EMPTY
            shapes.circle(cx - size * 0.25f, top, size * 0.28f, 16)
            shapes.circle(cx + size * 0.25f, top, size * 0.28f, 16)
            shapes.triangle(cx - size * 0.52f, top - size * 0.08f, cx + size * 0.52f, top - size * 0.08f, cx, top - size * 0.62f)
        }
    }

    companion object {
        private const val BASE_LIVES = 3
        private const val START_SPAWN_INTERVAL = 1.1f
        private const val MIN_SPAWN_INTERVAL = 0.35f
        private const val MAX_COAL_CHANCE = 0.4f
        private const val MAX_FALL_SPEED = 720f
        private const val KEYBOARD_SPEED = 900f
        private const val SHAKE_DURATION = 0.3f
        private const val SHAKE_STRENGTH = 10f
    }
}
