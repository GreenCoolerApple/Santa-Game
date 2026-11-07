package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.input.Action
import com.greencoolerapple.santagame.input.Touches
import com.greencoolerapple.santagame.shop.CoinRewards
import com.greencoolerapple.santagame.shop.Cosmetics
import com.greencoolerapple.santagame.ui.GameOverAnimation
import com.greencoolerapple.santagame.world.Sparkles
import com.greencoolerapple.santagame.world.sled.Sled
import com.greencoolerapple.santagame.world.sled.SledTrack

class SledRaceScreen(game: SantaGame) : PlayScreen(game) {
    private enum class State { COUNTDOWN, RACING, FINISHED, GAME_OVER }

    private class Rival(val sled: Sled, val nameKey: String, val color: Color, val skill: Float) {
        var targetX = 0f
        var blind = 0f
        var thinkTimer = 0f
    }

    private val worldCamera = OrthographicCamera()
    private val view = Rectangle()
    private val sparkles = Sparkles()
    private val gameOver = GameOverAnimation(game)
    private val tint = Color()
    private val player = Sled(Sled.Rider.SANTA)
    private val rivals = listOf(
        Rival(Sled(Sled.Rider.REINDEER), "sled.rival.comet", Palette.REINDEER, 0.985f),
        Rival(Sled(Sled.Rider.ELF), "sled.rival.jingle", Palette.ELF_GREEN, 0.955f),
        Rival(Sled(Sled.Rider.SNOWMAN), "sled.rival.flurry", Palette.SCARF_BLUE, 0.925f)
    )
    private val sleds = listOf(player) + rivals.map { it.sled }
    private val drawOrder = sleds.toMutableList()

    private lateinit var track: SledTrack

    private var state = State.COUNTDOWN
    private var time = 0f
    private var stateTimer = 0f
    private var raceTime = 0f
    private var finishedCount = 0
    private var cameraX = 0f
    private var shakeTimer = 0f
    private var lastCount = 0
    private var goTimer = 0f
    private var coinsEarned = 0
    private var newBest = false

    override val canPause: Boolean
        get() = state == State.COUNTDOWN || state == State.RACING

    override fun show() {
        restart()
    }

    override fun restartFromPause() {
        restart()
    }

    private fun restart() {
        track = SledTrack(MathUtils.random.nextLong())
        val startY = SledTrack.START_LINE - 70f
        val half = track.halfWidthAt(startY)
        val grid = floatArrayOf(-0.2f, -0.6f, 0.2f, 0.6f)
        for ((i, sled) in sleds.withIndex()) {
            sled.reset(track.centerAt(startY) + grid[i] * half, startY)
        }
        for (rival in rivals) {
            rival.targetX = rival.sled.x
            rival.blind = 0f
            rival.thinkTimer = MathUtils.random(0f, THINK_STEP)
        }
        sparkles.clear()
        state = State.COUNTDOWN
        stateTimer = 0f
        raceTime = 0f
        finishedCount = 0
        lastCount = COUNTDOWN.toInt() + 1
        goTimer = 0f
        shakeTimer = 0f
        cameraX = player.x
    }

    override fun cameraShake(): Float = if (shakeTimer > 0f) shakeTimer / SHAKE_DURATION * 10f else 0f

    override fun update(delta: Float) {
        time += delta
        game.scenery.update(delta)
        sparkles.update(delta)
        shakeTimer = maxOf(0f, shakeTimer - delta)
        goTimer = maxOf(0f, goTimer - delta)

        when (state) {
            State.COUNTDOWN -> updateCountdown(delta)
            State.RACING -> {
                raceTime += delta
                updatePlayer(delta, readSteer())
                updateRivals(delta)
                separate()
            }
            State.FINISHED -> {
                raceTime += delta
                stateTimer += delta
                updatePlayer(delta, 0f)
                updateRivals(delta)
                separate()
                if (stateTimer > FINISH_DELAY) showResults()
            }
            State.GAME_OVER -> {
                raceTime += delta
                updatePlayer(delta, 0f)
                updateRivals(delta)
                gameOver.update(delta)
                when (gameOver.handleInput()) {
                    GameOverAnimation.Action.RETRY -> restart()
                    GameOverAnimation.Action.MENU -> game.setScreen(GameModeScreen(game))
                    GameOverAnimation.Action.NONE -> Unit
                }
            }
        }

        cameraX = MathUtils.lerp(cameraX, player.x, minOf(1f, delta * CAMERA_FOLLOW))
    }

    private fun updateCountdown(delta: Float) {
        stateTimer += delta
        val count = MathUtils.ceil(COUNTDOWN - stateTimer)
        if (count < lastCount && count > 0) {
            lastCount = count
            game.haptics.play(Haptics.Pattern.TAP)
        }
        if (stateTimer >= COUNTDOWN) {
            state = State.RACING
            stateTimer = 0f
            goTimer = GO_TIME
            game.haptics.play(Haptics.Pattern.SUCCESS)
            for (sled in sleds) sled.speed = START_SPEED
        }
    }

    private fun readSteer(): Float {
        var steer = 0f
        val controls = game.controls
        if (controls.isPressed(Action.MOVE_LEFT)) steer -= 1f
        if (controls.isPressed(Action.MOVE_RIGHT)) steer += 1f
        val touches = game.touches
        for (i in 0 until Touches.MAX_POINTERS) {
            if (!touches.isFree(i)) continue
            steer += if (touches.x[i] < worldWidth / 2f) -1f else 1f
        }
        return MathUtils.clamp(steer, -1f, 1f)
    }

    private fun updatePlayer(delta: Float, input: Float) {
        val wasBoosting = player.boost > 0f
        val result = move(player, delta, input, 1f, state != State.RACING)
        if (state != State.RACING) return
        when (result) {
            Hit.CRASH -> {
                shakeTimer = SHAKE_DURATION
                game.haptics.play(Haptics.Pattern.HIT)
                sparkles.burst(player.x, player.y + 20f, Palette.SNOW, 22)
            }
            Hit.GIFT -> {
                game.haptics.play(Haptics.Pattern.COLLECT)
                sparkles.burst(player.x, player.y + 20f, Palette.GOLD, 10)
            }
            Hit.NONE -> Unit
        }
        if (!wasBoosting && player.boost > 0f) {
            game.haptics.play(Haptics.Pattern.PREVIEW)
            sparkles.burst(player.x, player.y, Palette.BOOST, 14)
        }
        if (player.y >= SledTrack.LENGTH) finishPlayer()
    }

    private enum class Hit { NONE, CRASH, GIFT }

    private fun move(sled: Sled, delta: Float, input: Float, skill: Float, coasting: Boolean): Hit {
        var hit = Hit.NONE
        sled.boost = maxOf(0f, sled.boost - delta)
        if (sled.stun > 0f) {
            sled.stun = maxOf(0f, sled.stun - delta)
            sled.spin = (1f - sled.stun / STUN_TIME) * 360f
            sled.steer = MathUtils.lerp(sled.steer, 0f, minOf(1f, delta * 6f))
        } else {
            sled.spin = 0f
            sled.steer = MathUtils.lerp(sled.steer, input, minOf(1f, delta * STEER_RESPONSE))
        }

        val onTrack = track.onTrack(sled.x, sled.y)
        var target = MAX_SPEED * skill
        if (sled.boost > 0f) target *= BOOST_FACTOR
        if (!onTrack) target *= DEEP_SNOW_FACTOR
        if (coasting) target = COAST_SPEED
        if (sled.stun > 0f) target = minOf(target, MAX_SPEED * 0.3f)
        sled.speed = if (sled.speed < target) {
            minOf(target, sled.speed + ACCELERATION * delta)
        } else {
            maxOf(target, sled.speed - (if (onTrack) BRAKING else DEEP_SNOW_BRAKING) * delta)
        }

        val grip = 0.45f + 0.55f * MathUtils.clamp(sled.speed / MAX_SPEED, 0f, 1f)
        sled.x += sled.steer * LATERAL_SPEED * grip * delta
        sled.y += sled.speed * delta
        val center = track.centerAt(sled.y)
        val limit = track.limitAt(sled.y)
        sled.x = MathUtils.clamp(sled.x, center - limit, center + limit)

        if (coasting) return hit
        val items = track.items
        var i = firstItemFrom(sled.y - 50f)
        while (i < items.size) {
            val item = items[i]
            if (item.y > sled.y + 50f) break
            i++
            if (item.taken) continue
            val dx = item.x - sled.x
            val dy = item.y - (sled.y + 10f)
            val reach = item.radius + Sled.RADIUS
            if (dx * dx + dy * dy > reach * reach) continue
            when (item.kind) {
                SledTrack.Kind.BOOST -> if (sled.stun <= 0f) sled.boost = BOOST_TIME
                SledTrack.Kind.GIFT -> if (sled === player) {
                    item.taken = true
                    sled.gifts++
                    hit = Hit.GIFT
                }
                else -> if (sled.stun <= 0f) {
                    sled.stun = STUN_TIME
                    sled.speed *= CRASH_FACTOR
                    sled.boost = 0f
                    sled.crashes++
                    hit = Hit.CRASH
                }
            }
        }
        return hit
    }

    private fun firstItemFrom(y: Float): Int {
        val items = track.items
        var low = 0
        var high = items.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (items[mid].y < y) low = mid + 1 else high = mid
        }
        return low
    }

    private fun updateRivals(delta: Float) {
        for (rival in rivals) {
            val sled = rival.sled
            if (sled.finished) {
                move(sled, delta, 0f, 1f, true)
                continue
            }
            rival.blind = maxOf(0f, rival.blind - delta)
            rival.thinkTimer -= delta
            if (rival.thinkTimer <= 0f) {
                rival.thinkTimer = THINK_STEP
                if (MathUtils.random() < MISTAKE_CHANCE * (1.05f - rival.skill) * 10f) rival.blind = BLIND_TIME
                if (rival.blind <= 0f) rival.targetX = chooseLine(sled)
            }
            val steer = MathUtils.clamp((rival.targetX - sled.x) / 50f, -1f, 1f)
            val gap = player.y - sled.y
            val band = 1f + MathUtils.clamp(gap / RUBBER_BAND_RANGE, -0.12f, 0.06f)
            move(sled, delta, steer, rival.skill * band, false)
            if (sled.y >= SledTrack.LENGTH) {
                sled.finished = true
                sled.finishTime = raceTime
                finishedCount++
                sled.place = finishedCount
            }
        }
    }

    private fun chooseLine(sled: Sled): Float {
        val ahead = sled.y + LOOK_AHEAD
        val start = firstItemFrom(sled.y + 30f)
        var bestX = sled.x
        var bestScore = Float.MAX_VALUE
        for (lane in SledTrack.LANES.indices) {
            val x = track.laneX(lane, ahead)
            var score = kotlin.math.abs(x - sled.x) * 0.6f
            var i = start
            val items = track.items
            while (i < items.size && items[i].y < sled.y + LOOK_AHEAD + 120f) {
                val item = items[i]
                i++
                val lx = track.laneX(lane, item.y)
                if (kotlin.math.abs(item.x - lx) > 56f) continue
                when (item.kind) {
                    SledTrack.Kind.BOOST -> score -= 140f
                    SledTrack.Kind.GIFT -> Unit
                    else -> score += 10000f / (1f + (item.y - sled.y) / 200f)
                }
            }
            if (score < bestScore) {
                bestScore = score
                bestX = x
            }
        }
        return bestX
    }

    private fun separate() {
        for (i in sleds.indices) {
            for (j in i + 1 until sleds.size) {
                val a = sleds[i]
                val b = sleds[j]
                val dy = b.y - a.y
                if (kotlin.math.abs(dy) > 50f) continue
                val dx = b.x - a.x
                val overlap = Sled.RADIUS * 2f + 6f - kotlin.math.abs(dx)
                if (overlap <= 0f) continue
                val push = if (dx >= 0f) overlap / 2f else -overlap / 2f
                a.x -= push
                b.x += push
                if ((a === player || b === player) && state == State.RACING && overlap > 6f) {
                    game.haptics.play(Haptics.Pattern.TAP)
                }
            }
        }
    }

    private fun finishPlayer() {
        player.finished = true
        player.finishTime = raceTime
        finishedCount++
        player.place = finishedCount
        state = State.FINISHED
        stateTimer = 0f
        game.haptics.play(Haptics.Pattern.SUCCESS)
        sparkles.burst(player.x, player.y + 40f, Palette.GOLD, 30)
        sparkles.burst(player.x, player.y + 40f, Palette.SANTA_RED, 20)

        val millis = (raceTime * 1000f).toInt()
        coinsEarned = CoinRewards.sledRace(player.place, raceTime, player.gifts)
        game.earnCoins(coinsEarned)
        newBest = game.finishSledRace(player.place, millis, player.gifts)
        game.awards.onSledFinished(player.place, raceTime, player.crashes)
    }

    private fun showResults() {
        state = State.GAME_OVER
        val place = player.place
        gameOver.start(
            title = I18n.t("sled.place$place"),
            color = if (place == 1) Palette.GOLD else Palette.SNOW,
            style = GameOverAnimation.Style.DROP,
            stats = listOf(
                GameOverAnimation.Stat(I18n.t("sled.statPlace"), place),
                GameOverAnimation.Stat(I18n.t("sled.statTime"), player.finishTime.toInt()),
                GameOverAnimation.Stat(I18n.t("sled.statGifts"), player.gifts),
                GameOverAnimation.Stat(I18n.t("coins.earned"), coinsEarned)
            ),
            newBest = newBest,
            confetti = place == 1,
            width = worldWidth,
            height = worldHeight
        )
    }

    private val livePlace: Int
        get() {
            if (player.finished) return player.place
            var place = 1
            for (rival in rivals) {
                if (rival.sled.finished || rival.sled.y > player.y) place++
            }
            return place
        }

    override fun worldProjection(shakeX: Float, shakeY: Float): Matrix4 {
        val w = worldWidth
        val h = worldHeight
        val cx = cameraX
        val cy = player.y + h * CAMERA_LEAD
        worldCamera.viewportWidth = w
        worldCamera.viewportHeight = h
        worldCamera.position.set(cx + shakeX, cy + shakeY, 0f)
        worldCamera.update()
        view.set(cx - w / 2f, cy - h / 2f, w, h)
        return worldCamera.combined
    }

    override fun drawWorld(shapes: ShapeRenderer) {
        track.drawGround(shapes, view, time)
        track.drawItems(shapes, view, time)
        drawOrder.sortByDescending { it.y }
        for (sled in drawOrder) {
            if (sled.y < view.y - 80f || sled.y > view.y + view.height + 80f) continue
            sled.draw(shapes, time)
        }
        track.drawForest(shapes, view)
        sparkles.draw(shapes)
    }

    override fun drawOverlay(shapes: ShapeRenderer) {
        if (player.boost > 0f && state == State.RACING) drawSpeedLines(shapes)
        game.scenery.drawSnow(shapes, worldWidth)
        if (state == State.GAME_OVER) {
            gameOver.drawShapes(shapes, worldWidth, worldHeight)
            return
        }
        drawProgress(shapes)
        if (game.isMobile && state != State.FINISHED) drawTouchHints(shapes)
    }

    private fun drawSpeedLines(shapes: ShapeRenderer) {
        val fade = minOf(1f, player.boost / 0.3f)
        for (k in 0 until 14) {
            val seed = k * 97.31f
            val x = (seed * 13.7f) % worldWidth
            val y = worldHeight - ((time * 1600f + seed * 41f) % (worldHeight + 200f))
            tint.set(Palette.SNOW)
            tint.a = 0.25f * fade
            shapes.color = tint
            shapes.rect(x, y, 3f, 90f)
        }
    }

    private fun drawProgress(shapes: ShapeRenderer) {
        val x = worldWidth - PROGRESS_MARGIN
        val bottom = 150f
        val top = worldHeight - 190f
        shapes.color = Palette.PROGRESS_BG
        shapes.rect(x - 6f, bottom - 6f, 12f, top - bottom + 12f)
        shapes.color = Palette.SNOW
        for (k in 0 until 4) {
            shapes.rect(x - 12f + (k % 2) * 12f, top + 6f, 12f, 8f)
        }
        shapes.color = Palette.BLACK
        for (k in 0 until 4) {
            shapes.rect(x - 12f + ((k + 1) % 2) * 12f, top + 6f, 12f, 8f)
        }
        for (rival in rivals) {
            val p = progressY(rival.sled.y, bottom, top)
            shapes.color = Palette.BLACK
            shapes.circle(x, p, 10f, 14)
            shapes.color = rival.color
            shapes.circle(x, p, 8f, 14)
        }
        val p = progressY(player.y, bottom, top)
        shapes.color = Palette.SNOW
        shapes.circle(x, p, 13f, 16)
        shapes.color = Cosmetics.suit
        shapes.circle(x, p, 10f, 16)
    }

    private fun progressY(y: Float, bottom: Float, top: Float): Float =
        bottom + (top - bottom) * MathUtils.clamp(y / SledTrack.LENGTH, 0f, 1f)

    private fun drawTouchHints(shapes: ShapeRenderer) {
        tint.set(1f, 1f, 1f, 0.16f)
        shapes.color = tint
        val y = 90f
        shapes.triangle(60f, y, 110f, y + 36f, 110f, y - 36f)
        shapes.triangle(worldWidth - 60f, y, worldWidth - 110f, y + 36f, worldWidth - 110f, y - 36f)
    }

    override fun drawHud() {
        if (state == State.GAME_OVER) {
            gameOver.drawText(worldWidth, worldHeight)
            return
        }
        for (rival in rivals) {
            val sled = rival.sled
            if (sled.y < view.y || sled.y > view.y + view.height) continue
            game.drawText(I18n.t(rival.nameKey), toScreenX(sled.x), toScreenY(sled.y) + 62f, 1.2f, rival.color)
        }

        game.drawText(I18n.t("sled.position", livePlace), 28f, worldHeight - 24f, 2.6f, Palette.GOLD, centered = false)
        game.drawText(I18n.t("sled.time", formatTime((raceTime * 1000f).toInt(), true)), 28f, worldHeight - 78f, 1.6f, centered = false)
        game.drawText(I18n.t("sled.gifts", player.gifts), 28f, worldHeight - 112f, 1.6f, centered = false)
        val kmh = (player.speed * KMH).toInt()
        val speedColor = if (player.boost > 0f) Palette.BOOST else Palette.TEXT
        game.drawText(I18n.t("sled.speed", kmh), worldWidth / 2f, 44f, 2f, speedColor)

        when (state) {
            State.COUNTDOWN -> {
                val count = MathUtils.ceil(COUNTDOWN - stateTimer).coerceAtLeast(1)
                val frac = (COUNTDOWN - stateTimer) % 1f
                val scale = 6f + frac * 3f
                game.drawText(count.toString(), worldWidth / 2f, worldHeight * 0.62f, scale, Palette.GOLD, alpha = 0.4f + 0.6f * frac)
                val hint = I18n.t(if (game.isMobile) "sled.hintMobile" else "sled.hintDesktop")
                game.drawText(hint, worldWidth / 2f, worldHeight * 0.38f, 1.6f)
            }
            State.RACING -> if (goTimer > 0f) {
                val t = 1f - goTimer / GO_TIME
                game.drawText(I18n.t("sled.go"), worldWidth / 2f, worldHeight * 0.62f, 6f + t * 4f, Palette.METER_SAFE, alpha = 1f - t)
            }
            State.FINISHED -> {
                val pop = maxOf(0.1f, minOf(1f, stateTimer * 4f))
                val wobble = 1f + MathUtils.sin(stateTimer * 8f) * 0.04f
                game.drawText(I18n.t("sled.finish"), worldWidth / 2f, worldHeight * 0.64f, 5f * pop * wobble, Palette.GOLD)
            }
            State.GAME_OVER -> Unit
        }
    }

    private fun toScreenX(x: Float): Float = x - view.x

    private fun toScreenY(y: Float): Float = y - view.y

    companion object {
        private const val COUNTDOWN = 3f
        private const val GO_TIME = 0.8f
        private const val FINISH_DELAY = 2.4f
        private const val MAX_SPEED = 640f
        private const val START_SPEED = 160f
        private const val COAST_SPEED = 180f
        private const val ACCELERATION = 260f
        private const val BRAKING = 340f
        private const val DEEP_SNOW_BRAKING = 900f
        private const val DEEP_SNOW_FACTOR = 0.5f
        private const val BOOST_FACTOR = 1.45f
        private const val BOOST_TIME = 1.2f
        private const val CRASH_FACTOR = 0.25f
        private const val STUN_TIME = 0.7f
        private const val LATERAL_SPEED = 430f
        private const val STEER_RESPONSE = 8f
        private const val LOOK_AHEAD = 260f
        private const val THINK_STEP = 0.18f
        private const val MISTAKE_CHANCE = 0.03f
        private const val BLIND_TIME = 0.7f
        private const val RUBBER_BAND_RANGE = 2500f
        private const val CAMERA_FOLLOW = 5f
        private const val CAMERA_LEAD = 0.27f
        private const val SHAKE_DURATION = 0.3f
        private const val PROGRESS_MARGIN = 40f
        private const val KMH = 0.15f

        fun formatTime(millis: Int, running: Boolean = false): String {
            if (millis <= 0 && !running) return "--"
            val total = maxOf(0, millis)
            val minutes = total / 60000
            val seconds = (total / 1000) % 60
            val hundredths = (total / 10) % 100
            return minutes.toString() + ":" + seconds.toString().padStart(2, '0') + "." + hundredths.toString().padStart(2, '0')
        }
    }
}
