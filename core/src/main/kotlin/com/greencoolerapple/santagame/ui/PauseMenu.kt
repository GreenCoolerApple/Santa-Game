package com.greencoolerapple.santagame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.input.Action as InputAction

class PauseMenu(private val game: SantaGame) {
    enum class Action { NONE, MENU, RESTART }

    private enum class State { CLOSED, OPENING, OPEN, CLOSING, COUNTDOWN }

    private enum class Icon { PLAY, RESTART, MENU }

    private class Entry(val key: String, val icon: Icon, val fill: Color, val edge: Color) {
        val bounds = Rectangle()
        var hover = 0f
    }

    private class Flake(var x: Float, var y: Float, val radius: Float, val speed: Float, val phase: Float)

    private var state = State.CLOSED
    private var stateTime = 0f
    private var time = 0f
    private var width = 0f
    private var height = 0f
    private var lastCount = 0

    private val entries = listOf(
        Entry("pause.resume", Icon.PLAY, Palette.BUTTON, Palette.BUTTON_EDGE),
        Entry("pause.restart", Icon.RESTART, Palette.BUTTON_GOLD, Palette.BUTTON_GOLD_EDGE),
        Entry("pause.menu", Icon.MENU, Palette.BUTTON_DARK, Palette.BUTTON_DARK_EDGE)
    )
    private val flakes = List(FLAKES) {
        Flake(MathUtils.random(), MathUtils.random(), MathUtils.random(1.5f, 4f), MathUtils.random(0.04f, 0.12f), MathUtils.random(0f, 6f))
    }
    private val tint = Color()
    private val mouse = Vector2()
    private val hitArea = Rectangle()

    val pauseButton = Rectangle()

    val paused: Boolean
        get() = state != State.CLOSED

    private val interactive: Boolean
        get() = state == State.OPEN || (state == State.OPENING && stateTime > 0.25f)

    fun layout(width: Float, height: Float) {
        this.width = width
        this.height = height
        pauseButton.set(width - SIZE - MARGIN, height - SIZE - MARGIN, SIZE, SIZE)
        hitArea.set(pauseButton.x - 20f, pauseButton.y - 20f, SIZE + 40f, SIZE + 40f)
    }

    fun open() {
        if (state != State.CLOSED) return
        setState(State.OPENING)
        game.haptics.play(Haptics.Pattern.TAP)
    }

    fun forceClose() {
        state = State.CLOSED
    }

    private fun setState(next: State) {
        state = next
        stateTime = 0f
    }

    private fun resume() {
        setState(State.CLOSING)
        game.haptics.play(Haptics.Pattern.TAP)
    }

    fun update(delta: Float, canPause: Boolean, showButton: Boolean): Action {
        time += delta
        stateTime += delta
        for (flake in flakes) {
            flake.y -= flake.speed * delta
            if (flake.y < -0.02f) {
                flake.y = 1.02f
                flake.x = MathUtils.random()
            }
        }

        when (state) {
            State.OPENING -> if (stateTime >= OPEN_TIME) setState(State.OPEN)
            State.CLOSING -> if (stateTime >= CLOSE_TIME) {
                setState(State.COUNTDOWN)
                lastCount = 0
            }
            State.COUNTDOWN -> {
                val count = COUNTDOWN_STEPS - (stateTime / COUNTDOWN_STEP).toInt()
                if (count != lastCount && count > 0) {
                    lastCount = count
                    game.haptics.play(Haptics.Pattern.TAP)
                }
                if (stateTime >= COUNTDOWN_STEP * COUNTDOWN_STEPS) setState(State.CLOSED)
            }
            else -> Unit
        }

        val backPressed = Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
            game.controls.isJustPressed(InputAction.PAUSE)
        if (backPressed) {
            when (state) {
                State.OPEN, State.OPENING -> resume()
                State.CLOSED -> if (canPause) open() else return Action.MENU
                else -> Unit
            }
        }

        if (state == State.OPEN || state == State.OPENING) layoutEntries()
        updateHover(delta)

        val touches = game.touches
        val pointer = touches.firstJustDown()
        if (pointer < 0) return Action.NONE
        val x = touches.x[pointer]
        val y = touches.y[pointer]

        if (state == State.CLOSED) {
            if (showButton && canPause && hitArea.contains(x, y)) {
                touches.claim(pointer)
                open()
            }
            return Action.NONE
        }

        touches.claim(pointer)
        if (!interactive) return Action.NONE
        val hit = entries.firstOrNull { it.bounds.contains(x, y) } ?: return Action.NONE
        game.haptics.play(Haptics.Pattern.TAP)
        return when (hit.icon) {
            Icon.PLAY -> {
                resume()
                Action.NONE
            }
            Icon.RESTART -> {
                forceClose()
                Action.RESTART
            }
            Icon.MENU -> Action.MENU
        }
    }

    private fun updateHover(delta: Float) {
        val desktop = game.isDesktop && interactive
        if (desktop) {
            mouse.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
            game.viewport.unproject(mouse)
        }
        for (entry in entries) {
            val over = desktop && entry.bounds.contains(mouse.x, mouse.y)
            entry.hover = MathUtils.clamp(entry.hover + (if (over) delta else -delta) * 8f, 0f, 1f)
        }
    }

    private fun panelOffset(): Float = when (state) {
        State.OPENING -> (1f - easeOutBack(minOf(1f, stateTime / OPEN_TIME))) * height
        State.CLOSING -> easeIn(minOf(1f, stateTime / CLOSE_TIME)) * height
        State.COUNTDOWN, State.CLOSED -> height * 2f
        State.OPEN -> 0f
    }

    private fun panelCenterY(): Float = height / 2f + panelOffset()

    private fun entryPop(index: Int): Float {
        if (state == State.CLOSING) return 1f
        val t = (if (state == State.OPEN) OPEN_TIME + stateTime else stateTime) - 0.18f - index * 0.09f
        if (t <= 0f) return 0f
        return easeOutBack(minOf(1f, t / 0.3f))
    }

    private fun layoutEntries() {
        val cy = panelCenterY()
        for ((i, entry) in entries.withIndex()) {
            val scale = entryPop(i) * (1f + entry.hover * 0.06f)
            val w = BUTTON_WIDTH * scale
            val h = BUTTON_HEIGHT * scale
            entry.bounds.set(width / 2f - w / 2f, cy + 44f - i * 90f - h / 2f, w, h)
        }
    }

    private fun overlayAlpha(): Float = when (state) {
        State.OPENING -> minOf(1f, stateTime / OPEN_TIME)
        State.OPEN -> 1f
        State.CLOSING -> 1f - 0.4f * minOf(1f, stateTime / CLOSE_TIME)
        State.COUNTDOWN -> 0.6f * (1f - stateTime / (COUNTDOWN_STEP * COUNTDOWN_STEPS))
        State.CLOSED -> 0f
    }

    fun drawButton(shapes: ShapeRenderer) {
        val cx = pauseButton.x + SIZE / 2f
        val cy = pauseButton.y + SIZE / 2f
        val pulse = 1f + MathUtils.sin(time * 3f) * 0.03f
        shapes.color = Palette.PAUSE_BG
        shapes.circle(cx, cy, SIZE / 2f * pulse, 32)
        shapes.color = Palette.GOLD
        Draw.ring(shapes, cx, cy, SIZE / 2f * pulse - 3f, SIZE / 2f * pulse, 32)
        shapes.color = Palette.TEXT
        shapes.rect(cx - 13f, cy - 15f, 9f, 30f)
        shapes.rect(cx + 4f, cy - 15f, 9f, 30f)
        shapes.color = Palette.SANTA_RED
        shapes.triangle(cx - 22f, cy + 22f, cx + 14f, cy + 26f, cx - 18f, cy + 46f)
        shapes.color = Palette.SNOW
        Draw.roundedRect(shapes, cx - 26f, cy + 18f, 44f, 10f, 5f)
        shapes.circle(cx - 18f, cy + 47f, 5f, 10)
    }

    fun drawOverlay(shapes: ShapeRenderer) {
        if (state == State.CLOSED) return
        val alpha = overlayAlpha()
        tint.set(0.02f, 0.04f, 0.1f, 0.68f * alpha)
        shapes.color = tint
        shapes.rect(-20f, -20f, width + 40f, height + 40f)

        for (flake in flakes) {
            tint.set(1f, 1f, 1f, 0.8f * alpha)
            shapes.color = tint
            shapes.circle(flake.x * width + MathUtils.sin(time * 1.5f + flake.phase) * 14f, flake.y * height, flake.radius, 8)
        }

        if (state == State.COUNTDOWN) {
            drawCountdownShapes(shapes)
            return
        }

        layoutEntries()
        val cx = width / 2f
        val cy = panelCenterY()
        val left = cx - PANEL_WIDTH / 2f
        val bottom = cy - PANEL_HEIGHT / 2f
        val top = cy + PANEL_HEIGHT / 2f

        shapes.color = Palette.SHADOW
        Draw.roundedRect(shapes, left + 10f, bottom - 14f, PANEL_WIDTH, PANEL_HEIGHT, 34f)
        shapes.color = Palette.GOLD
        Draw.roundedRect(shapes, left - 6f, bottom - 6f, PANEL_WIDTH + 12f, PANEL_HEIGHT + 12f, 38f)
        shapes.color = Palette.PANEL_GREEN
        Draw.roundedRect(shapes, left, bottom, PANEL_WIDTH, PANEL_HEIGHT, 32f)
        shapes.color = Palette.PANEL_GREEN_DARK
        Draw.roundedRect(shapes, left + 14f, bottom + 14f, PANEL_WIDTH - 28f, PANEL_HEIGHT - 28f, 24f)

        val bannerY = top - 70f
        shapes.color = Palette.SANTA_RED_DARK
        shapes.triangle(left - 40f, bannerY - 34f, left + 10f, bannerY - 34f, left + 10f, bannerY + 12f)
        shapes.triangle(left - 40f, bannerY + 12f, left - 18f, bannerY - 11f, left + 10f, bannerY + 12f)
        shapes.triangle(left + PANEL_WIDTH + 40f, bannerY - 34f, left + PANEL_WIDTH - 10f, bannerY - 34f, left + PANEL_WIDTH - 10f, bannerY + 12f)
        shapes.triangle(left + PANEL_WIDTH + 40f, bannerY + 12f, left + PANEL_WIDTH + 18f, bannerY - 11f, left + PANEL_WIDTH - 10f, bannerY + 12f)
        shapes.color = Palette.SANTA_RED
        shapes.rect(left - 14f, bannerY - 26f, PANEL_WIDTH + 28f, 64f)
        shapes.color = Palette.GOLD
        shapes.rect(left - 14f, bannerY + 32f, PANEL_WIDTH + 28f, 4f)
        shapes.rect(left - 14f, bannerY - 24f, PANEL_WIDTH + 28f, 4f)

        drawGarland(shapes, left + 20f, left + PANEL_WIDTH - 20f, top + 4f)
        drawHolly(shapes, left + 16f, top - 6f, false)
        drawHolly(shapes, left + PANEL_WIDTH - 16f, top - 6f, true)

        for ((i, entry) in entries.withIndex()) {
            if (entryPop(i) <= 0.02f) continue
            val b = entry.bounds
            val r = b.height * 0.3f
            shapes.color = entry.edge
            Draw.roundedRect(shapes, b.x - 4f, b.y - 6f, b.width + 8f, b.height + 10f, r + 4f)
            shapes.color = entry.fill
            Draw.roundedRect(shapes, b.x, b.y, b.width, b.height, r)
            if (entry.hover > 0f) {
                tint.set(1f, 1f, 1f, 0.12f * entry.hover)
                shapes.color = tint
                Draw.roundedRect(shapes, b.x, b.y, b.width, b.height, r)
            }
            drawIcon(shapes, entry.icon, b.x + 46f * (b.width / BUTTON_WIDTH), b.y + b.height / 2f, b.height / BUTTON_HEIGHT)
        }
    }

    private fun drawGarland(shapes: ShapeRenderer, x0: Float, x1: Float, y: Float) {
        val segments = 40
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val x = MathUtils.lerp(x0, x1, t)
            val sag = MathUtils.sin(t * MathUtils.PI2 * 2f).let { kotlin.math.abs(it) } * 26f
            shapes.color = if (i % 2 == 0) Palette.PINE else Palette.TREE_LIGHT
            shapes.circle(x, y - sag, 8f, 10)
        }
        for (i in 0 until 12) {
            val t = (i + 0.5f) / 12f
            val x = MathUtils.lerp(x0, x1, t)
            val sag = kotlin.math.abs(MathUtils.sin(t * MathUtils.PI2 * 2f)) * 26f
            val on = ((time * 3f).toInt() + i) % 3 != 0
            tint.set(Palette.GIFT_BOXES[i % Palette.GIFT_BOXES.size])
            if (!on) tint.lerp(Color.BLACK, 0.55f)
            shapes.color = tint
            shapes.circle(x, y - sag - 11f, 6.5f, 12)
            if (on) {
                tint.a = 0.25f
                shapes.color = tint
                shapes.circle(x, y - sag - 11f, 13f, 14)
            }
        }
    }

    private fun drawHolly(shapes: ShapeRenderer, x: Float, y: Float, flip: Boolean) {
        val d = if (flip) -1f else 1f
        shapes.color = Palette.PINE
        shapes.triangle(x, y, x + 34f * d, y + 16f, x + 22f * d, y - 8f)
        shapes.triangle(x, y, x + 34f * d, y + 16f, x + 14f * d, y + 20f)
        shapes.triangle(x, y, x + 8f * d, y - 34f, x - 10f * d, y - 20f)
        shapes.triangle(x, y, x + 8f * d, y - 34f, x + 20f * d, y - 14f)
        shapes.color = Palette.SANTA_RED
        shapes.circle(x - 2f * d, y + 4f, 7f, 12)
        shapes.circle(x + 8f * d, y - 2f, 6.5f, 12)
        shapes.circle(x - 4f * d, y - 8f, 6f, 12)
        shapes.color = Palette.BLUSH
        shapes.circle(x - 4f * d, y + 6f, 2.2f, 6)
    }

    private fun drawIcon(shapes: ShapeRenderer, icon: Icon, cx: Float, cy: Float, scale: Float) {
        val s = 14f * scale
        shapes.color = Palette.TEXT
        when (icon) {
            Icon.PLAY -> shapes.triangle(cx - s * 0.7f, cy - s, cx - s * 0.7f, cy + s, cx + s, cy)
            Icon.RESTART -> {
                Draw.ring(shapes, cx, cy, s * 0.62f, s * 1.0f, 24)
                tint.set(Palette.BUTTON_GOLD)
                shapes.color = tint
                shapes.rect(cx, cy, s * 1.1f, s * 1.1f)
                shapes.color = Palette.TEXT
                shapes.triangle(cx + s * 0.2f, cy + s * 0.25f, cx + s * 1.25f, cy + s * 0.25f, cx + s * 0.72f, cy - s * 0.45f)
            }
            Icon.MENU -> {
                shapes.rect(cx - s, cy + s * 0.5f, s * 2f, s * 0.36f)
                shapes.rect(cx - s, cy - s * 0.18f, s * 2f, s * 0.36f)
                shapes.rect(cx - s, cy - s * 0.86f, s * 2f, s * 0.36f)
            }
        }
    }

    private fun drawCountdownShapes(shapes: ShapeRenderer) {
        val step = (stateTime / COUNTDOWN_STEP).toInt()
        val local = (stateTime - step * COUNTDOWN_STEP) / COUNTDOWN_STEP
        val cx = width / 2f
        val cy = height / 2f
        shapes.color = Palette.PANEL_GREEN_DARK
        shapes.circle(cx, cy, 86f, 48)
        tint.set(Palette.GOLD)
        shapes.color = tint
        val segments = 48
        val filled = (segments * (1f - local)).toInt()
        val stepAngle = MathUtils.PI2 / segments
        for (i in 0 until filled) {
            val a0 = MathUtils.PI / 2f - i * stepAngle
            val a1 = a0 - stepAngle
            shapes.triangle(cx + MathUtils.cos(a0) * 78f, cy + MathUtils.sin(a0) * 78f, cx + MathUtils.cos(a0) * 90f, cy + MathUtils.sin(a0) * 90f, cx + MathUtils.cos(a1) * 90f, cy + MathUtils.sin(a1) * 90f)
            shapes.triangle(cx + MathUtils.cos(a0) * 78f, cy + MathUtils.sin(a0) * 78f, cx + MathUtils.cos(a1) * 90f, cy + MathUtils.sin(a1) * 90f, cx + MathUtils.cos(a1) * 78f, cy + MathUtils.sin(a1) * 78f)
        }
    }

    fun drawOverlayText() {
        if (state == State.CLOSED) return
        if (state == State.COUNTDOWN) {
            val step = (stateTime / COUNTDOWN_STEP).toInt()
            val local = (stateTime - step * COUNTDOWN_STEP) / COUNTDOWN_STEP
            val number = COUNTDOWN_STEPS - step
            if (number > 0) {
                val scale = 7f * (1.35f - 0.35f * easeOutBack(minOf(1f, local * 3f)))
                game.drawText(number.toString(), width / 2f, height / 2f, scale, Palette.GOLD_LIGHT, alpha = 1f - local * 0.4f)
            }
            return
        }
        val cy = panelCenterY()
        val title = I18n.t("pause.title")
        val scale = 3.4f
        val total = game.textWidth(title, scale)
        var x = width / 2f - total / 2f
        val bannerY = cy + PANEL_HEIGHT / 2f - 70f
        for ((i, ch) in title.withIndex()) {
            val text = ch.toString()
            val wave = MathUtils.sin(time * 5f + i * 0.6f) * 5f
            game.drawText(text, x, bannerY + 26f + wave, scale, Palette.SNOW, centered = false)
            x += game.textWidth(text, scale)
        }

        for ((i, entry) in entries.withIndex()) {
            val pop = entryPop(i)
            if (pop <= 0.6f) continue
            val b = entry.bounds
            val labelScale = 2.1f * (b.height / BUTTON_HEIGHT)
            game.drawText(I18n.t(entry.key), b.x + b.width / 2f + 22f, b.y + b.height / 2f, labelScale, alpha = (pop - 0.6f) / 0.4f)
        }
        game.drawText(I18n.t("pause.hint"), width / 2f, cy - PANEL_HEIGHT / 2f + 18f, 1.05f, Palette.GOLD_LIGHT, alpha = 0.8f)
    }

    private fun easeIn(t: Float): Float = t * t * t

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val u = t - 1f
        return 1f + c3 * u * u * u + c1 * u * u
    }

    companion object {
        const val SIZE = 72f
        const val MARGIN = 20f
        private const val FLAKES = 60
        private const val OPEN_TIME = 0.45f
        private const val CLOSE_TIME = 0.3f
        private const val COUNTDOWN_STEP = 0.45f
        private const val COUNTDOWN_STEPS = 3
        private const val PANEL_WIDTH = 520f
        private const val PANEL_HEIGHT = 470f
        private const val BUTTON_WIDTH = 360f
        private const val BUTTON_HEIGHT = 72f
    }
}
