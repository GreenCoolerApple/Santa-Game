package com.greencoolerapple.santagame.ui

import com.greencoolerapple.santagame.i18n.I18n
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame

class GameOverAnimation(private val game: SantaGame) {
    enum class Style { DROP, STAMP }
    enum class Action { NONE, RETRY, MENU }

    class Stat(val label: String, val value: Int)

    private class Piece(
        var x: Float,
        var y: Float,
        val speed: Float,
        val sway: Float,
        var phase: Float,
        var rotation: Float,
        val spin: Float,
        val size: Float,
        val box: Color,
        val ribbon: Color
    )

    var time = 0f
        private set

    private var title = ""
    private val titleColor = Color()
    private var style = Style.DROP
    private var stats: List<Stat> = emptyList()
    private var newBest = false
    private val pieces = mutableListOf<Piece>()
    private val tint = Color()

    private val retryButton = Button("Retry", labelScale = 2.4f)
    private val menuButton = Button("Menu", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 2.4f)

    private val titleDone: Float
        get() = if (style == Style.DROP) LETTER_DELAY + title.length * LETTER_STEP + 0.45f else STAMP_TIME + 0.35f

    private val bestAt: Float
        get() = titleDone + stats.size * STAT_STEP + 0.2f

    private val buttonsAt: Float
        get() = bestAt + if (newBest) 0.5f else 0f

    val stampImpact: Float
        get() = STAMP_TIME

    fun start(title: String, color: Color, style: Style, stats: List<Stat>, newBest: Boolean, confetti: Boolean, width: Float, height: Float) {
        this.title = title
        this.style = style
        this.stats = stats
        this.newBest = newBest
        titleColor.set(color)
        time = 0f
        pieces.clear()
        if (confetti) {
            repeat(CONFETTI_COUNT) { i ->
                pieces += Piece(
                    x = MathUtils.random(0f, width),
                    y = height + MathUtils.random(20f, height * 1.4f),
                    speed = MathUtils.random(110f, 220f),
                    sway = MathUtils.random(10f, 40f),
                    phase = MathUtils.random(0f, MathUtils.PI2),
                    rotation = MathUtils.random(0f, 360f),
                    spin = MathUtils.random(-160f, 160f),
                    size = MathUtils.random(14f, 26f),
                    box = Palette.GIFT_BOXES[i % Palette.GIFT_BOXES.size],
                    ribbon = Palette.GIFT_RIBBONS[i % Palette.GIFT_RIBBONS.size]
                )
            }
        }
    }

    fun update(delta: Float) {
        time += delta
        for (p in pieces) {
            p.phase += delta * 2f
            p.y -= p.speed * delta
            p.rotation += p.spin * delta
        }
        pieces.removeAll { it.y < -40f }
    }

    fun handleInput(): Action {
        if (time < buttonsAt + 0.25f) return Action.NONE
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) return Action.RETRY
        val touches = game.touches
        val pointer = touches.firstJustDown()
        if (pointer < 0) return Action.NONE
        touches.claim(pointer)
        val x = touches.x[pointer]
        val y = touches.y[pointer]
        if (retryButton.contains(x, y)) return Action.RETRY
        if (menuButton.contains(x, y)) return Action.MENU
        return Action.NONE
    }

    fun drawShapes(shapes: ShapeRenderer, width: Float, height: Float) {
        tint.set(0f, 0f, 0f, minOf(OVERLAY_ALPHA, time * 1.3f))
        shapes.color = tint
        shapes.rect(-30f, -30f, width + 60f, height + 60f)

        for (p in pieces) {
            val x = p.x + MathUtils.sin(p.phase) * p.sway
            val half = p.size / 2f
            shapes.color = p.box
            shapes.rect(x - half, p.y - half, half, half, p.size, p.size, 1f, 1f, p.rotation)
            shapes.color = p.ribbon
            shapes.rect(x - half * 0.25f, p.y - half, half * 0.25f, half, p.size * 0.25f, p.size, 1f, 1f, p.rotation)
        }

        if (newBest && time > bestAt) {
            val t = time - bestAt
            val cy = bestY(height)
            val spread = 190f + MathUtils.sin(t * 3f) * 10f
            for (i in 0 until 6) {
                val angle = t * 60f + i * 60f
                val sx = width / 2f + MathUtils.cosDeg(angle) * spread
                val sy = cy + MathUtils.sinDeg(angle) * spread * 0.28f
                val pop = easeOutBack(minOf(1f, t * 3f))
                shapes.color = Palette.GOLD
                Draw.star(shapes, sx, sy, 14f * pop, t * 120f + i * 30f)
            }
        }

        retryButton.label = I18n.t("over.retry")
        menuButton.label = I18n.t("over.menu")
        val pop = buttonPop()
        if (pop > 0f) {
            val y = height * 0.15f
            retryButton.place(width / 2f - 170f, y, 280f * pop, 84f * pop)
            menuButton.place(width / 2f + 170f, y, 280f * pop, 84f * pop)
            retryButton.drawShape(shapes)
            menuButton.drawShape(shapes)
        } else {
            retryButton.place(0f, 0f, 0f, 0f)
            menuButton.place(0f, 0f, 0f, 0f)
        }
    }

    fun drawText(width: Float, height: Float) {
        val titleY = height * 0.74f
        when (style) {
            Style.DROP -> drawDroppingTitle(width, titleY)
            Style.STAMP -> drawStampTitle(width, titleY)
        }

        for ((j, stat) in stats.withIndex()) {
            val start = titleDone + j * STAT_STEP
            val t = time - start
            if (t <= 0f) continue
            val progress = minOf(1f, t / COUNT_TIME)
            val shown = (stat.value * easeOut(progress)).toInt()
            val slide = (1f - easeOut(minOf(1f, t * 4f))) * 40f
            game.drawText("${stat.label}: $shown", width / 2f, height * 0.56f - j * 46f - slide, 2.1f, alpha = minOf(1f, t * 4f))
        }

        if (newBest && time > bestAt) {
            val t = time - bestAt
            val scale = 2.6f * easeOutBack(minOf(1f, t * 2.5f)) + MathUtils.sin(t * 6f) * 0.12f
            game.drawText(I18n.t("over.newBest"), width / 2f, bestY(height), scale, Palette.GOLD)
        }

        val pop = buttonPop()
        if (pop > 0.7f) {
            val alpha = (pop - 0.7f) / 0.3f
            retryButton.drawLabel(game, alpha = alpha)
            menuButton.drawLabel(game, alpha = alpha)
        }
    }

    private fun drawDroppingTitle(width: Float, y: Float) {
        val scale = 5f
        val total = game.textWidth(title, scale)
        val left = width / 2f - total / 2f
        for (i in title.indices) {
            val t = time - LETTER_DELAY - i * LETTER_STEP
            if (t <= 0f) continue
            val progress = minOf(1f, t / DROP_TIME)
            val offset = (1f - easeOutBounce(progress)) * 320f
            val wobble = if (progress >= 1f) MathUtils.sin(time * 4f + i * 0.6f) * 4f else 0f
            val x = left + game.textWidth(title.substring(0, i), scale)
            game.drawText(title[i].toString(), x, y + offset + wobble + 36f, scale, titleColor, centered = false, alpha = minOf(1f, t * 6f))
        }
    }

    private fun drawStampTitle(width: Float, y: Float) {
        if (time <= 0f) return
        val scale = if (time < STAMP_TIME) {
            MathUtils.lerp(14f, 5.5f, easeIn(time / STAMP_TIME))
        } else {
            val t = time - STAMP_TIME
            5.5f + MathUtils.sin(t * 22f) * 0.6f * maxOf(0f, 1f - t * 2.5f)
        }
        game.drawText(title, width / 2f, y, scale, titleColor, alpha = minOf(1f, time * 5f))
    }

    private fun bestY(height: Float): Float = height * 0.56f - stats.size * 46f - 16f

    private fun buttonPop(): Float {
        val t = time - buttonsAt
        if (t <= 0f) return 0f
        return easeOutBack(minOf(1f, t / 0.35f))
    }

    private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)

    private fun easeIn(t: Float): Float = t * t * t

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val u = t - 1f
        return 1f + c3 * u * u * u + c1 * u * u
    }

    private fun easeOutBounce(t: Float): Float {
        val n = 7.5625f
        val d = 2.75f
        return when {
            t < 1f / d -> n * t * t
            t < 2f / d -> { val u = t - 1.5f / d; n * u * u + 0.75f }
            t < 2.5f / d -> { val u = t - 2.25f / d; n * u * u + 0.9375f }
            else -> { val u = t - 2.625f / d; n * u * u + 0.984375f }
        }
    }

    companion object {
        private const val OVERLAY_ALPHA = 0.62f
        private const val LETTER_DELAY = 0.25f
        private const val LETTER_STEP = 0.08f
        private const val DROP_TIME = 0.6f
        private const val STAMP_TIME = 0.32f
        private const val STAT_STEP = 0.45f
        private const val COUNT_TIME = 0.7f
        private const val CONFETTI_COUNT = 70
    }
}
