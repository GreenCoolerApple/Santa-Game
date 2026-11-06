package com.greencoolerapple.santagame.screens

import com.greencoolerapple.santagame.i18n.I18n
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.ui.Draw
import com.greencoolerapple.santagame.ui.GameOverAnimation
import com.greencoolerapple.santagame.ui.Joystick
import com.greencoolerapple.santagame.world.Sparkles
import com.greencoolerapple.santagame.world.house.Guard
import com.greencoolerapple.santagame.world.house.HouseLayout
import com.greencoolerapple.santagame.world.house.HouseRenderer
import com.greencoolerapple.santagame.world.house.SneakySanta

class StolenGiftsScreen(game: SantaGame) : PlayScreen(game) {
    private enum class State { PLAYING, ESCAPED, CAUGHT, GAME_OVER }

    private val santa = SneakySanta()
    private val guards = mutableListOf<Guard>()
    private val joystick = Joystick()
    private val sparkles = Sparkles()
    private val input = Vector2()
    private val meterColor = Color()
    private val gameOver = GameOverAnimation(game)

    private lateinit var house: HouseLayout

    private var state = State.PLAYING
    private var level = 1
    private var totalGifts = 0
    private var housesRobbed = 0
    private var carried = 0
    private var treeGifts = 0
    private var lastGain = 0
    private var stealTimer = 0f
    private var detection = 0f
    private var stateTimer = 0f
    private var time = 0f
    private var shakeTimer = 0f
    private var caughtBest = false
    private var bannerTimer = 0f
    private var seenThisHouse = false
    private var bannerText = ""

    override val canPause: Boolean
        get() = state == State.PLAYING

    override fun show() {
        val data = game.data
        if (data.hasRun) {
            level = data.runHouse
            totalGifts = data.runGifts
            housesRobbed = data.runHouses
            bannerText = I18n.t("stolen.continue", level)
            bannerTimer = BANNER_TIME
        }
        startHouse()
    }

    override fun cameraShake(): Float = if (shakeTimer > 0f) shakeTimer / SHAKE_DURATION * 12f else 0f

    private fun startHouse() {
        val offsetX = (worldWidth - HouseLayout.ROOM_WIDTH) / 2f
        val offsetY = (worldHeight - HouseLayout.ROOM_HEIGHT) / 2f
        house = HouseLayout.build(level - 1, offsetX, offsetY)
        santa.reset(house.spawn)
        guards.clear()
        val guardCount = minOf(house.guardPaths.size, level)
        val speed = minOf(MAX_GUARD_SPEED, 80f + level * 10f)
        for (i in 0 until guardCount) {
            guards += Guard(house.guardPaths[i], speed, Palette.PAJAMAS[i % Palette.PAJAMAS.size])
        }
        treeGifts = minOf(MAX_TREE_GIFTS, 3 + level)
        carried = 0
        stealTimer = 0f
        seenThisHouse = false
        detection = 0f
        stateTimer = 0f
        joystick.release()
        sparkles.clear()
        state = State.PLAYING
    }

    private fun restart() {
        level = 1
        totalGifts = 0
        housesRobbed = 0
        bannerTimer = 0f
        startHouse()
    }

    override fun update(delta: Float) {
        time += delta
        sparkles.update(delta)
        shakeTimer = maxOf(0f, shakeTimer - delta)
        bannerTimer = maxOf(0f, bannerTimer - delta)

        when (state) {
            State.PLAYING -> updatePlaying(delta)
            State.ESCAPED -> {
                stateTimer += delta
                if (stateTimer > ESCAPE_DELAY) {
                    level++
                    startHouse()
                }
            }
            State.CAUGHT -> {
                stateTimer += delta
                if (stateTimer > CAUGHT_DELAY) {
                    state = State.GAME_OVER
                    stateTimer = 0f
                    gameOver.start(
                        title = I18n.t("stolen.caught"),
                        color = Palette.METER_DANGER,
                        style = GameOverAnimation.Style.STAMP,
                        stats = listOf(
                            GameOverAnimation.Stat(I18n.t("stolen.statGifts"), totalGifts),
                            GameOverAnimation.Stat(I18n.t("stolen.statHouses"), housesRobbed)
                        ),
                        newBest = caughtBest,
                        confetti = false,
                        width = worldWidth,
                        height = worldHeight
                    )
                }
            }
            State.GAME_OVER -> {
                val before = gameOver.time
                gameOver.update(delta)
                if (before < gameOver.stampImpact && gameOver.time >= gameOver.stampImpact) {
                    shakeTimer = SHAKE_DURATION
                    Gdx.input.vibrate(60)
                }
                when (gameOver.handleInput()) {
                    GameOverAnimation.Action.RETRY -> restart()
                    GameOverAnimation.Action.MENU -> game.setScreen(GameModeScreen(game))
                    GameOverAnimation.Action.NONE -> Unit
                }
            }
        }
    }

    private fun updatePlaying(delta: Float) {
        joystick.update(game.touches)
        input.set(joystick.direction)
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) input.x = -1f
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) input.x = 1f
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) input.y = -1f
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) input.y = 1f

        val speed = SANTA_SPEED * maxOf(MIN_SPEED_FACTOR, 1f - carried * 0.04f)
        santa.move(input, delta, speed, house.blockers, house.interior)

        var spotted = false
        for (guard in guards) {
            val sees = guard.canSee(santa.position, house.blockers)
            guard.update(delta, sees, santa.position)
            if (sees) {
                spotted = true
                seenThisHouse = true
                val closeness = 1f - minOf(1f, guard.position.dst(santa.position) / Guard.VIEW_RANGE)
                detection += (1.3f + closeness * 2.4f) * delta
            }
        }
        if (!spotted) detection = maxOf(0f, detection - DETECTION_DECAY * delta)

        if (detection >= 1f) {
            detection = 1f
            state = State.CAUGHT
            stateTimer = 0f
            shakeTimer = SHAKE_DURATION
            Gdx.input.vibrate(200)
            joystick.release()
            stealTimer = 0f
            caughtBest = game.finishStolenRun(totalGifts)
            return
        }

        if (treeGifts > 0 && santa.position.dst(house.tree) < STEAL_RADIUS) {
            stealTimer += delta
            if (stealTimer >= STEAL_TIME) {
                stealTimer = 0f
                treeGifts--
                carried++
                sparkles.burst(house.tree.x, house.tree.y, Palette.GOLD, 12)
                Gdx.input.vibrate(30)
            }
        } else {
            stealTimer = 0f
        }

        if (carried > 0 && house.fireplace.contains(santa.position)) {
            lastGain = carried + if (treeGifts == 0) CLEAN_SWEEP_BONUS else 0
            totalGifts += lastGain
            housesRobbed++
            carried = 0
            game.checkpointStolenRun(level + 1, totalGifts, housesRobbed, lastGain)
            game.awards.onHouseRobbed(housesRobbed, treeGifts == 0, !seenThisHouse)
            joystick.release()
            sparkles.burst(santa.position.x, santa.position.y, Palette.FIRE_CORE, 24)
            state = State.ESCAPED
            stateTimer = 0f
        }
    }

    override fun drawWorld(shapes: ShapeRenderer) {
        HouseRenderer.drawRoom(shapes, house, worldWidth, worldHeight, time, carried > 0)

        if (treeGifts > 0) {
            shapes.color = Palette.STEAL_ZONE
            shapes.circle(house.tree.x, house.tree.y, STEAL_RADIUS, 40)
        }
        HouseRenderer.drawTree(shapes, house, treeGifts, time)

        for (guard in guards) guard.drawCone(shapes)
        for (guard in guards) guard.drawBody(shapes)
        if (state != State.ESCAPED) santa.draw(shapes, carried)
        sparkles.draw(shapes)

        if (stealTimer > 0f) {
            val x = santa.position.x - 30f
            val y = santa.position.y + 34f
            shapes.color = Palette.METER_BG
            shapes.rect(x, y, 60f, 10f)
            shapes.color = Palette.GOLD
            shapes.rect(x, y, 60f * (stealTimer / STEAL_TIME), 10f)
        }

        if (state == State.PLAYING) joystick.draw(shapes)
        if (state == State.CAUGHT || state == State.GAME_OVER) drawIris(shapes)
        if (state == State.GAME_OVER) {
            gameOver.drawShapes(shapes, worldWidth, worldHeight)
        } else {
            drawMeter(shapes)
        }
    }

    private fun drawIris(shapes: ShapeRenderer) {
        val far = Vector2.len(worldWidth, worldHeight) + 200f
        val progress = if (state == State.CAUGHT) minOf(1f, stateTimer / CAUGHT_DELAY) else 1f
        val eased = progress * progress * (3f - 2f * progress)
        val radius = far + (IRIS_RADIUS - far) * eased
        shapes.color = Palette.IRIS
        Draw.ring(shapes, santa.position.x, santa.position.y, radius, far + 400f)
    }

    private fun drawMeter(shapes: ShapeRenderer) {
        val width = 320f
        val x = worldWidth / 2f - width / 2f
        val y = worldHeight - 46f
        shapes.color = Palette.METER_BG
        shapes.rect(x - 4f, y - 4f, width + 8f, 26f)
        meterColor.set(Palette.METER_SAFE).lerp(Palette.METER_DANGER, detection)
        shapes.color = meterColor
        shapes.rect(x, y, width * detection, 18f)
    }

    override fun drawHud() {
        if (state == State.GAME_OVER) {
            gameOver.drawText(worldWidth, worldHeight)
            return
        }
        game.drawText(I18n.t("stolen.gifts", totalGifts), 28f, worldHeight - 24f, 2f, centered = false)
        game.drawText(I18n.t("stolen.house", level), 28f, worldHeight - 64f, 1.5f, centered = false)

        val hint = when {
            state != State.PLAYING -> ""
            carried > 0 && treeGifts == 0 -> I18n.t("stolen.hintEscape")
            carried > 0 -> I18n.t("stolen.hintSack", carried)
            else -> I18n.t("stolen.hintSneak")
        }
        if (hint.isNotEmpty()) game.drawText(hint, worldWidth / 2f, worldHeight - 70f, 1.3f)
        if (bannerTimer > 0f && state == State.PLAYING) {
            game.drawText(bannerText, worldWidth / 2f, worldHeight * 0.5f, 2.6f, Palette.GOLD, alpha = minOf(1f, bannerTimer))
        }

        for (guard in guards) {
            if (guard.spotting) game.drawText("!", guard.position.x, guard.position.y + 42f, 2.6f, Palette.METER_DANGER)
        }

        when (state) {
            State.ESCAPED -> {
                game.drawText(I18n.t("stolen.robbed"), worldWidth / 2f, worldHeight * 0.58f, 4f, Palette.GOLD)
                game.drawText(I18n.t("stolen.gain", lastGain), worldWidth / 2f, worldHeight * 0.46f, 2.2f)
            }
            State.CAUGHT, State.GAME_OVER, State.PLAYING -> Unit
        }
    }

    companion object {
        private const val SANTA_SPEED = 240f
        private const val MIN_SPEED_FACTOR = 0.68f
        private const val MAX_GUARD_SPEED = 150f
        private const val MAX_TREE_GIFTS = 8
        private const val STEAL_RADIUS = 95f
        private const val STEAL_TIME = 0.7f
        private const val DETECTION_DECAY = 0.4f
        private const val CLEAN_SWEEP_BONUS = 2
        private const val ESCAPE_DELAY = 1.8f
        private const val CAUGHT_DELAY = 0.9f
        private const val IRIS_RADIUS = 70f
        private const val BANNER_TIME = 2.5f
        private const val SHAKE_DURATION = 0.35f
    }
}
