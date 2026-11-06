package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.ui.Draw
import com.greencoolerapple.santagame.ui.GameOverAnimation
import com.greencoolerapple.santagame.ui.Joystick
import com.greencoolerapple.santagame.world.Sparkles
import com.greencoolerapple.santagame.world.city.Boy
import com.greencoolerapple.santagame.world.city.CityMap
import com.greencoolerapple.santagame.world.city.Sick

class EscapeCityScreen(game: SantaGame) : PlayScreen(game) {
    private enum class State { PLAYING, ESCAPED, CAUGHT, GAME_OVER }

    private val worldCamera = OrthographicCamera()
    private val view = Rectangle()
    private val cameraPosition = Vector2()
    private val boy = Boy()
    private val sick = mutableListOf<Sick>()
    private val candies = mutableListOf<Vector2>()
    private val joystick = Joystick()
    private val sparkles = Sparkles()
    private val gameOver = GameOverAnimation(game)
    private val input = Vector2()
    private val catchPosition = Vector2()
    private val tint = Color()

    private lateinit var map: CityMap

    private var state = State.PLAYING
    private var level = 1
    private var candyCount = 0
    private var cities = 0
    private var stateTimer = 0f
    private var time = 0f
    private var shakeTimer = 0f
    private var bannerTimer = 0f
    private var caughtBest = false

    private val score: Int
        get() = candyCount + cities * CITY_BONUS

    override val canPause: Boolean
        get() = state == State.PLAYING

    override fun show() {
        startCity()
    }

    override fun cameraShake(): Float = if (shakeTimer > 0f) shakeTimer / SHAKE_DURATION * 12f else 0f

    private fun startCity() {
        map = CityMap(level, MathUtils.random.nextLong())
        boy.reset(map.start)
        sick.clear()
        val count = minOf(MAX_SICK, 3 + level * 2)
        repeat(count) {
            sick += Sick(map.randomOpenPoint(map.start, SPAWN_DISTANCE, Vector2()))
        }
        candies.clear()
        repeat(14 + level * 3) {
            candies += map.randomOpenPoint(map.start, 200f, Vector2())
        }
        joystick.release()
        sparkles.clear()
        cameraPosition.set(boy.position)
        state = State.PLAYING
        stateTimer = 0f
        bannerTimer = BANNER_TIME
    }

    private fun restart() {
        level = 1
        candyCount = 0
        cities = 0
        startCity()
    }

    override fun update(delta: Float) {
        time += delta
        game.scenery.update(delta)
        sparkles.update(delta)
        shakeTimer = maxOf(0f, shakeTimer - delta)
        bannerTimer = maxOf(0f, bannerTimer - delta)

        when (state) {
            State.PLAYING -> updatePlaying(delta)
            State.ESCAPED -> {
                stateTimer += delta
                if (stateTimer > ESCAPE_DELAY) {
                    level++
                    startCity()
                }
            }
            State.CAUGHT -> updateCaught(delta)
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

        val focus = if (state == State.CAUGHT || state == State.GAME_OVER) catchPosition else boy.position
        cameraPosition.lerp(focus, minOf(1f, delta * CAMERA_FOLLOW))
    }

    private fun updatePlaying(delta: Float) {
        joystick.update(game.touches)
        input.set(joystick.direction)
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) input.x = -1f
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) input.x = 1f
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) input.y = -1f
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) input.y = 1f
        boy.move(input, delta, BOY_SPEED, map)

        val chaseSpeed = minOf(MAX_CHASE_SPEED, 196f + level * 8f)
        for (s in sick) s.update(delta, boy.position, map, WANDER_SPEED, chaseSpeed)
        for (i in sick.indices) {
            for (j in i + 1 until sick.size) sick[i].pushAway(sick[j], map)
        }

        val iterator = candies.iterator()
        while (iterator.hasNext()) {
            val candy = iterator.next()
            if (candy.dst(boy.position) < CANDY_RADIUS) {
                iterator.remove()
                candyCount++
                sparkles.burst(candy.x, candy.y, Palette.GOLD, 10)
            }
        }

        if (sick.any { it.position.dst(boy.position) < Sick.RADIUS + Boy.RADIUS - 2f }) {
            state = State.CAUGHT
            stateTimer = 0f
            catchPosition.set(boy.position)
            joystick.release()
            Gdx.input.vibrate(150)
            caughtBest = game.finishEscapeGame(score, candyCount, cities)
            return
        }

        if (boy.position.dst(map.exit) < EXIT_RADIUS) {
            cities++
            state = State.ESCAPED
            stateTimer = 0f
            joystick.release()
            sparkles.burst(map.exit.x, map.exit.y, Palette.EXIT_RING, 30)
        }
    }

    private fun updateCaught(delta: Float) {
        val before = stateTimer
        stateTimer += delta
        if (before < DROP_TIME && stateTimer >= DROP_TIME) {
            shakeTimer = SHAKE_DURATION
            sparkles.burst(catchPosition.x, catchPosition.y, Palette.SNOW, 24)
            Gdx.input.vibrate(80)
        }
        if (before < BOW_TIME && stateTimer >= BOW_TIME) {
            sparkles.burst(catchPosition.x, catchPosition.y + 30f, Palette.GOLD, 18)
        }
        if (stateTimer > CAUGHT_TIME) {
            state = State.GAME_OVER
            gameOver.start(
                title = I18n.t("escape.wrapped"),
                color = Palette.SICK_SKIN,
                style = GameOverAnimation.Style.STAMP,
                stats = listOf(
                    GameOverAnimation.Stat(I18n.t("escape.statCandy"), candyCount),
                    GameOverAnimation.Stat(I18n.t("escape.statCities"), cities),
                    GameOverAnimation.Stat(I18n.t("escape.statScore"), score)
                ),
                newBest = caughtBest,
                confetti = false,
                width = worldWidth,
                height = worldHeight
            )
        }
    }

    override fun worldProjection(shakeX: Float, shakeY: Float): Matrix4 {
        val w = worldWidth
        val h = worldHeight
        val cx = if (map.width <= w) map.width / 2f else MathUtils.clamp(cameraPosition.x, w / 2f, map.width - w / 2f)
        val cy = if (map.height <= h) map.height / 2f else MathUtils.clamp(cameraPosition.y, h / 2f, map.height - h / 2f)
        worldCamera.viewportWidth = w
        worldCamera.viewportHeight = h
        worldCamera.position.set(cx + shakeX, cy + shakeY, 0f)
        worldCamera.update()
        view.set(cx - w / 2f, cy - h / 2f, w, h)
        return worldCamera.combined
    }

    override fun drawWorld(shapes: ShapeRenderer) {
        map.drawGround(shapes, view, time)
        drawExit(shapes)
        drawCandies(shapes)
        map.drawTrees(shapes, view)
        for (s in sick) {
            if (inView(s.position, 60f)) s.draw(shapes)
        }
        val wrapped = (state == State.CAUGHT && stateTimer >= DROP_TIME) || state == State.GAME_OVER
        if (state != State.ESCAPED && !wrapped) boy.draw(shapes)
        if (state == State.CAUGHT || state == State.GAME_OVER) drawGiftBox(shapes)
        sparkles.draw(shapes)
    }

    private fun inView(point: Vector2, margin: Float): Boolean =
        point.x > view.x - margin && point.x < view.x + view.width + margin &&
            point.y > view.y - margin && point.y < view.y + view.height + margin

    private fun drawExit(shapes: ShapeRenderer) {
        val e = map.exit
        if (!inView(e, 140f)) return
        val pulse = (MathUtils.sin(time * 3f) + 1f) / 2f
        shapes.color = Palette.EXIT_GLOW
        shapes.circle(e.x, e.y, 80f + pulse * 18f, 36)
        shapes.circle(e.x, e.y, 56f, 32)
        shapes.color = Palette.EXIT_RING
        Draw.ring(shapes, e.x, e.y, 44f, 54f, 40)
        for (i in 0 until 6) {
            val angle = time * 90f + i * 60f
            shapes.circle(e.x + MathUtils.cosDeg(angle) * 66f, e.y + MathUtils.sinDeg(angle) * 66f, 5f, 8)
        }
    }

    private fun drawCandies(shapes: ShapeRenderer) {
        for ((i, candy) in candies.withIndex()) {
            if (!inView(candy, 30f)) continue
            val bob = MathUtils.sin(time * 4f + i) * 3f
            val x = candy.x
            val y = candy.y + bob
            val color = Palette.GIFT_BOXES[i % Palette.GIFT_BOXES.size]
            shapes.color = Palette.SHADOW
            shapes.circle(x + 2f, candy.y - 6f, 9f, 12)
            shapes.color = color
            shapes.triangle(x - 9f, y, x - 18f, y + 7f, x - 18f, y - 7f)
            shapes.triangle(x + 9f, y, x + 18f, y + 7f, x + 18f, y - 7f)
            shapes.circle(x, y, 10f, 16)
            shapes.color = Palette.CANDY_STRIPE
            shapes.rect(x - 2.5f, y - 9f, 5f, 18f)
        }
    }

    private fun drawGiftBox(shapes: ShapeRenderer) {
        val t = if (state == State.GAME_OVER) CAUGHT_TIME else stateTimer
        val x = catchPosition.x
        val y = catchPosition.y
        val falling = t < DROP_TIME
        val p = minOf(1f, t / DROP_TIME)
        val scale = if (falling) MathUtils.lerp(3.4f, 1f, p * p) else 1f + 0.18f * maxOf(0f, 1f - (t - DROP_TIME) / 0.2f) * MathUtils.sin((t - DROP_TIME) * 40f)
        val size = BOX_SIZE * scale
        val half = size / 2f

        tint.set(Palette.SHADOW)
        tint.a = 0.15f + 0.25f * p
        shapes.color = tint
        shapes.circle(x + 6f, y - 8f, BOX_SIZE * 0.7f * (0.4f + 0.6f * p), 28)

        tint.set(Palette.SANTA_RED)
        if (falling) tint.a = 0.4f + 0.6f * p
        shapes.color = tint
        shapes.rect(x - half, y - half, size, size)
        tint.set(Palette.SANTA_RED_DARK)
        if (falling) tint.a = 0.4f + 0.6f * p
        shapes.color = tint
        shapes.rect(x - half, y - half, size, size * 0.12f)
        shapes.rect(x - half, y - half, size * 0.1f, size)

        if (!falling) {
            val ribbon = minOf(1f, (t - DROP_TIME) / RIBBON_TIME)
            shapes.color = Palette.GOLD
            shapes.rect(x - 7f, y - half, 14f, size * ribbon)
            shapes.rect(x - half, y - 7f, size * ribbon, 14f)
        }
        if (t >= BOW_TIME) {
            val pop = easeOutBack(minOf(1f, (t - BOW_TIME) / 0.3f))
            shapes.color = Palette.GOLD
            shapes.circle(x - 16f * pop, y + 6f * pop, 14f * pop, 16)
            shapes.circle(x + 16f * pop, y + 6f * pop, 14f * pop, 16)
            shapes.triangle(x, y, x - 14f * pop, y - 24f * pop, x - 4f * pop, y - 26f * pop)
            shapes.triangle(x, y, x + 14f * pop, y - 24f * pop, x + 4f * pop, y - 26f * pop)
            shapes.color = Palette.SANTA_RED_DARK
            shapes.circle(x - 16f * pop, y + 6f * pop, 6f * pop, 12)
            shapes.circle(x + 16f * pop, y + 6f * pop, 6f * pop, 12)
            shapes.color = Palette.FIRE_CORE
            shapes.circle(x, y, 9f * pop, 14)
        }
    }

    override fun drawOverlay(shapes: ShapeRenderer) {
        game.scenery.drawSnow(shapes, worldWidth)
        if (state == State.PLAYING) {
            joystick.draw(shapes)
            drawExitArrow(shapes)
        }
        if (state != State.GAME_OVER) drawMinimap(shapes)
        if (state == State.GAME_OVER) gameOver.drawShapes(shapes, worldWidth, worldHeight)
    }

    private fun drawExitArrow(shapes: ShapeRenderer) {
        if (inView(map.exit, -40f)) return
        val cx = view.x + view.width / 2f
        val cy = view.y + view.height / 2f
        val dx = map.exit.x - cx
        val dy = map.exit.y - cy
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        if (length < 1f) return
        val nx = dx / length
        val ny = dy / length
        val halfW = worldWidth / 2f - 70f
        val halfH = worldHeight / 2f - 90f
        val reach = minOf(if (nx != 0f) halfW / kotlin.math.abs(nx) else Float.MAX_VALUE, if (ny != 0f) halfH / kotlin.math.abs(ny) else Float.MAX_VALUE)
        val ax = worldWidth / 2f + nx * reach
        val ay = worldHeight / 2f + ny * reach
        val pulse = 1f + MathUtils.sin(time * 6f) * 0.12f
        val size = 26f * pulse
        shapes.color = Palette.EXIT_GLOW
        shapes.circle(ax, ay, 34f * pulse, 24)
        shapes.color = Palette.EXIT_RING
        shapes.triangle(
            ax + nx * size, ay + ny * size,
            ax - nx * size * 0.6f - ny * size * 0.7f, ay - ny * size * 0.6f + nx * size * 0.7f,
            ax - nx * size * 0.6f + ny * size * 0.7f, ay - ny * size * 0.6f - nx * size * 0.7f
        )
    }

    private fun drawMinimap(shapes: ShapeRenderer) {
        val scale = MINIMAP_WIDTH / map.width
        val x = worldWidth - 24f - map.width * scale
        val y = worldHeight - 120f - map.height * scale
        map.drawMinimap(shapes, x, y, scale)
        shapes.color = Palette.EXIT_RING
        shapes.circle(x + map.exit.x * scale, y + map.exit.y * scale, 5f, 10)
        shapes.color = Palette.METER_DANGER
        for (s in sick) {
            if (s.chasing) shapes.circle(x + s.position.x * scale, y + s.position.y * scale, 3.5f, 8)
        }
        shapes.color = Palette.SNOW
        shapes.circle(x + boy.position.x * scale, y + boy.position.y * scale, 5f, 10)
        shapes.color = Palette.BOY_JACKET
        shapes.circle(x + boy.position.x * scale, y + boy.position.y * scale, 3.5f, 10)
    }

    override fun drawHud() {
        if (state == State.GAME_OVER) {
            gameOver.drawText(worldWidth, worldHeight)
            return
        }
        game.drawText(I18n.t("escape.city", level), 28f, worldHeight - 24f, 2f, centered = false)
        game.drawText(I18n.t("escape.candy", candyCount), 28f, worldHeight - 64f, 1.5f, centered = false)
        game.drawText(I18n.t("score", score), 28f, worldHeight - 96f, 1.5f, centered = false)

        if (inView(map.exit, 0f)) {
            game.drawText(I18n.t("escape.exit"), toScreenX(map.exit.x), toScreenY(map.exit.y) + 100f, 1.8f, Palette.EXIT_RING)
        }
        for (s in sick) {
            if (s.chasing && inView(s.position, 0f)) {
                game.drawText("!", toScreenX(s.position.x), toScreenY(s.position.y) + 38f, 2.6f, Palette.METER_DANGER)
            }
        }

        if (state == State.PLAYING && bannerTimer > 0f) {
            game.drawText(I18n.t("escape.hint"), worldWidth / 2f, worldHeight * 0.62f, 2.2f, Palette.GOLD, alpha = minOf(1f, bannerTimer))
        }
        if (state == State.ESCAPED) {
            game.drawText(I18n.t("escape.escaped"), worldWidth / 2f, worldHeight * 0.58f, 3.6f, Palette.EXIT_RING)
            game.drawText(I18n.t("escape.bonus", CITY_BONUS), worldWidth / 2f, worldHeight * 0.46f, 2.2f)
        }
    }

    private fun toScreenX(x: Float): Float = x - view.x

    private fun toScreenY(y: Float): Float = y - view.y

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val u = t - 1f
        return 1f + c3 * u * u * u + c1 * u * u
    }

    companion object {
        private const val BOY_SPEED = 270f
        private const val WANDER_SPEED = 85f
        private const val MAX_CHASE_SPEED = 250f
        private const val MAX_SICK = 16
        private const val SPAWN_DISTANCE = 650f
        private const val CANDY_RADIUS = 34f
        private const val EXIT_RADIUS = 50f
        private const val CITY_BONUS = 10
        private const val CAMERA_FOLLOW = 8f
        private const val BANNER_TIME = 3f
        private const val ESCAPE_DELAY = 2.2f
        private const val DROP_TIME = 0.45f
        private const val RIBBON_TIME = 0.3f
        private const val BOW_TIME = 0.85f
        private const val CAUGHT_TIME = 1.7f
        private const val BOX_SIZE = 76f
        private const val SHAKE_DURATION = 0.35f
        private const val MINIMAP_WIDTH = 190f
    }
}
