package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.awards.Award
import com.greencoolerapple.santagame.easter.GreenApple
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.shop.Cosmetics
import com.greencoolerapple.santagame.ui.Button
import com.greencoolerapple.santagame.ui.Draw

class MainMenuScreen(game: SantaGame) : MenuBase(game) {
    private class Slot(val button: Button, val baseScale: Float, val order: Int) {
        var hover = 0f
    }

    private val gameModeButton = Button("", labelScale = 2.8f)
    private val awardsButton = Button("", fill = Palette.BUTTON_GOLD, edge = Palette.BUTTON_GOLD_EDGE, labelScale = 2.1f, sublabelColor = Palette.TEXT)
    private val shopButton = Button("", fill = Palette.BUTTON_SHOP, edge = Palette.BUTTON_SHOP_EDGE, labelScale = 2.1f, sublabelColor = Palette.GOLD_LIGHT)
    private val optionsButton = Button("", fill = Palette.BUTTON_ALT, edge = Palette.BUTTON_ALT_EDGE, labelScale = 2.4f)

    override val buttons = listOf(gameModeButton, awardsButton, shopButton, optionsButton)

    private val slots = listOf(
        Slot(gameModeButton, 2.8f, 0),
        Slot(awardsButton, 2.1f, 1),
        Slot(shopButton, 2.1f, 2),
        Slot(optionsButton, 2.4f, 3)
    )

    private val apple = GreenApple(game)
    private val mouse = Vector2()
    private val tint = Color()
    private val stars = List(STAR_COUNT) {
        floatArrayOf(MathUtils.random(0.03f, 0.97f), MathUtils.random(0.5f, 0.95f), MathUtils.random(0f, MathUtils.PI2), MathUtils.random(2f, 4.5f))
    }

    override fun show() {
        apple.roll()
    }

    override fun interceptTap(x: Float, y: Float): Boolean = apple.handleTap(x, y)

    override fun interceptBack(): Boolean = apple.handleBack()

    override fun layout() {
        apple.update(Gdx.graphics.deltaTime, worldWidth, worldHeight)
        gameModeButton.label = I18n.t("menu.gameMode")
        awardsButton.label = I18n.t("menu.awards")
        awardsButton.sublabel = "${game.awards.unlockedCount} / ${Award.entries.size}"
        shopButton.label = I18n.t("menu.shop")
        shopButton.sublabel = I18n.t("shop.balance", game.data.coins)
        optionsButton.label = I18n.t("menu.options")

        val hovered = hoveredButton()
        val delta = Gdx.graphics.deltaTime
        for (slot in slots) {
            val target = if (slot.button === hovered) 1f else 0f
            slot.hover = MathUtils.lerp(slot.hover, target, minOf(1f, delta * HOVER_SPEED))
        }

        val breathe = 1f + MathUtils.sin(time * 3f) * 0.022f
        place(slots[0], worldWidth / 2f, worldHeight * 0.615f, 500f, 104f, breathe)
        place(slots[1], worldWidth / 2f - 130f, worldHeight * 0.44f, 240f, 92f, 1f)
        place(slots[2], worldWidth / 2f + 130f, worldHeight * 0.44f, 240f, 92f, 1f)
        place(slots[3], worldWidth / 2f, worldHeight * 0.275f, 500f, 80f, 1f)
    }

    private fun appear(order: Int): Float {
        val t = (time - BUTTONS_START - order * BUTTON_STEP) / BUTTON_TIME
        return easeOutBack(MathUtils.clamp(t, 0f, 1f))
    }

    private fun place(slot: Slot, cx: Float, cy: Float, width: Float, height: Float, pulse: Float) {
        val pop = appear(slot.order)
        val scale = maxOf(0.01f, pop) * pulse * (1f + slot.hover * HOVER_GROW)
        slot.button.place(cx, cy + slot.hover * 4f, width * scale, height * scale)
        slot.button.labelScale = slot.baseScale * scale
    }

    private fun hoveredButton(): Button? {
        if (!game.isDesktop || apple.active) return null
        mouse.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        game.viewport.unproject(mouse)
        return buttons.firstOrNull { it.contains(mouse.x, mouse.y) }
    }

    override fun onTap(button: Button) {
        when (button) {
            gameModeButton -> game.setScreen(GameModeScreen(game))
            awardsButton -> game.setScreen(AwardsScreen(game))
            shopButton -> game.setScreen(ShopScreen(game))
            optionsButton -> game.setScreen(OptionsScreen(game))
        }
    }

    override fun onBack() {
        Gdx.app.exit()
    }

    override fun drawContentShapes(shapes: ShapeRenderer) {
        drawStars(shapes)
        drawSleigh(shapes)
        drawGarland(shapes)
        drawTitleShapes(shapes)
        drawCoinPill(shapes)
        drawButtonGlow(shapes)
    }

    override fun drawOverlayShapes(shapes: ShapeRenderer) {
        for (slot in slots) {
            if (appear(slot.order) > 0.6f) drawSnowCap(shapes, slot.button)
        }
        drawHolly(shapes, gameModeButton)
        apple.drawShapes(shapes)
    }

    override fun drawOverlayTexts() {
        apple.drawTexts()
    }

    private fun drawStars(shapes: ShapeRenderer) {
        for (star in stars) {
            val twinkle = (MathUtils.sin(time * 2.2f + star[2]) + 1f) / 2f
            val x = star[0] * worldWidth
            val y = star[1] * worldHeight
            val r = star[3] * (0.5f + twinkle * 0.6f)
            tint.set(Palette.GOLD_LIGHT)
            tint.a = 0.3f + 0.7f * twinkle
            shapes.color = tint
            shapes.triangle(x - r * 2.2f, y, x + r * 2.2f, y, x, y + r * 0.5f)
            shapes.triangle(x - r * 2.2f, y, x + r * 2.2f, y, x, y - r * 0.5f)
            shapes.triangle(x, y - r * 2.2f, x, y + r * 2.2f, x + r * 0.5f, y)
            shapes.triangle(x, y - r * 2.2f, x, y + r * 2.2f, x - r * 0.5f, y)
            shapes.circle(x, y, r * 0.6f, 8)
        }
    }

    private fun drawSleigh(shapes: ShapeRenderer) {
        val cycle = (time + SLEIGH_OFFSET) % SLEIGH_CYCLE
        val t = cycle / SLEIGH_DURATION
        if (t >= 1f) return
        val s = SLEIGH_SCALE
        val path = { p: Float -> worldHeight * 0.74f + MathUtils.sin(p * MathUtils.PI2) * 22f + (p - 0.5f) * 70f }
        val x = MathUtils.lerp(-260f, worldWidth + 260f, t)
        val y = path(t)
        val gallop = time * 14f

        for (i in 1..14) {
            val tt = t - i * 0.01f
            if (tt <= 0f) continue
            val tx = MathUtils.lerp(-260f, worldWidth + 260f, tt) - 40f * s
            tint.set(if (i % 2 == 0) Palette.GOLD_LIGHT else Palette.GOLD)
            tint.a = 0.8f * (1f - i / 14f)
            shapes.color = tint
            shapes.circle(tx + MathUtils.sin(i * 1.7f + time * 9f) * 5f, path(tt) - 10f * s + MathUtils.cos(i * 2.3f + time * 5f) * 6f, (3.6f - i * 0.2f) * s * 1.4f, 8)
        }

        for (r in 0 until 3) {
            val rx = x + (120f + r * 64f) * s
            val ry = y + (14f + MathUtils.sin(gallop + r) * 4f) * s
            shapes.color = Palette.GOLD_DARK
            shapes.rectLine(x + 50f * s, y + 18f * s, rx - 14f * s, ry, 1.6f)
            drawReindeer(shapes, rx, ry, gallop + r * 1.3f, s, r == 2)
        }

        shapes.color = Palette.SACK
        shapes.circle(x - 26f * s, y + 30f * s, 17f * s, 14)
        shapes.color = Cosmetics.suit
        shapes.rect(x - 2f * s, y + 24f * s, 22f * s, 22f * s)
        shapes.color = Palette.SKIN
        shapes.circle(x + 10f * s, y + 52f * s, 9f * s, 12)
        shapes.color = Palette.SNOW
        shapes.circle(x + 10f * s, y + 47f * s, 7f * s, 12)
        shapes.color = Cosmetics.suit
        shapes.triangle(x + 1f * s, y + 57f * s, x + 19f * s, y + 57f * s, x + 22f * s, y + 72f * s)
        shapes.color = Palette.SNOW
        shapes.circle(x + 22f * s, y + 73f * s, 3.5f * s, 8)

        shapes.color = Palette.SANTA_RED
        shapes.rect(x - 42f * s, y, 84f * s, 26f * s)
        shapes.rect(x - 42f * s, y + 22f * s, 24f * s, 18f * s)
        shapes.triangle(x + 42f * s, y, x + 42f * s, y + 26f * s, x + 56f * s, y + 30f * s)
        shapes.color = Palette.GOLD
        shapes.rect(x - 42f * s, y + 10f * s, 84f * s, 4f * s)
        shapes.rect(x - 48f * s, y - 12f * s, 106f * s, 4f * s)
        shapes.circle(x + 58f * s, y - 6f * s, 6f * s, 10)
        shapes.rect(x - 26f * s, y - 10f * s, 4f * s, 12f * s)
        shapes.rect(x + 24f * s, y - 10f * s, 4f * s, 12f * s)
    }

    private fun drawReindeer(shapes: ShapeRenderer, x: Float, y: Float, gallop: Float, s: Float, lead: Boolean) {
        val front = MathUtils.sin(gallop) * 9f * s
        val back = MathUtils.sin(gallop + MathUtils.PI) * 9f * s
        shapes.color = Palette.REINDEER_DARK
        shapes.rectLine(x + 14f * s, y - 3f * s, x + 19f * s + front, y - 22f * s, 2.6f * s)
        shapes.rectLine(x + 8f * s, y - 3f * s, x + 12f * s - front, y - 22f * s, 2.6f * s)
        shapes.rectLine(x - 14f * s, y - 3f * s, x - 19f * s + back, y - 20f * s, 2.6f * s)
        shapes.rectLine(x - 8f * s, y - 3f * s, x - 12f * s - back, y - 20f * s, 2.6f * s)
        shapes.color = Palette.REINDEER
        shapes.ellipse(x - 22f * s, y - 8f * s, 44f * s, 17f * s, 14)
        shapes.rectLine(x + 17f * s, y + 3f * s, x + 26f * s, y + 17f * s, 6f * s)
        shapes.ellipse(x + 22f * s, y + 13f * s, 16f * s, 8f * s, 10)
        shapes.color = Palette.ANTLER
        shapes.rectLine(x + 26f * s, y + 20f * s, x + 22f * s, y + 33f * s, 1.8f * s)
        shapes.rectLine(x + 29f * s, y + 20f * s, x + 34f * s, y + 32f * s, 1.8f * s)
        if (lead) {
            val glow = 0.5f + 0.5f * MathUtils.sin(time * 6f)
            tint.set(Palette.HEART)
            tint.a = 0.25f + 0.25f * glow
            shapes.color = tint
            shapes.circle(x + 38f * s, y + 17f * s, 9f * s, 12)
            shapes.color = Palette.HEART
            shapes.circle(x + 38f * s, y + 17f * s, 3.6f * s, 10)
        }
    }

    private fun drawGarland(shapes: ShapeRenderer) {
        val swags = maxOf(4, (worldWidth / 300f).toInt())
        val span = worldWidth / swags
        val top = worldHeight + 6f
        val drop = 36f
        var bulb = 0
        for (k in 0 until swags) {
            val x0 = k * span
            val steps = 16
            for (j in 0 until steps) {
                val a = j / steps.toFloat()
                val b = (j + 1) / steps.toFloat()
                val ya = top - drop * 4f * a * (1f - a)
                val yb = top - drop * 4f * b * (1f - b)
                shapes.color = Palette.PINE_DARK
                shapes.rectLine(x0 + a * span, ya, x0 + b * span, yb, 12f)
                shapes.color = if (j % 2 == 0) Palette.PINE else Palette.TREE_LIGHT
                shapes.circle(x0 + a * span, ya, 6.5f, 8)
            }
            for (j in 1 until 6) {
                val a = j / 6f
                val bx = x0 + a * span
                val by = top - drop * 4f * a * (1f - a) - 10f
                val color = BULBS[bulb % BULBS.size]
                val lit = ((bulb + (time * 3f).toInt()) % 3) == 0
                if (lit) {
                    tint.set(color)
                    tint.a = 0.32f
                    shapes.color = tint
                    shapes.circle(bx, by - 4f, 15f, 14)
                }
                shapes.color = Palette.COAL_SHINE
                shapes.rect(bx - 3f, by + 2f, 6f, 6f)
                tint.set(color)
                if (!lit) tint.mul(0.7f, 0.7f, 0.7f, 1f)
                shapes.color = tint
                shapes.ellipse(bx - 5.5f, by - 12f, 11f, 15f, 12)
                bulb++
            }
            shapes.color = Palette.SANTA_RED
            shapes.circle(x0, top - 6f, 9f, 12)
            shapes.triangle(x0, top - 8f, x0 - 9f, top - 26f, x0 - 1f, top - 24f)
            shapes.triangle(x0, top - 8f, x0 + 9f, top - 26f, x0 + 1f, top - 24f)
        }
    }

    private fun titleLeft(): Float = worldWidth / 2f - game.textWidth(TITLE, TITLE_SCALE) / 2f

    private fun letterX(left: Float, i: Int): Float = left + game.textWidth(TITLE.substring(0, i), TITLE_SCALE)

    private fun letterY(i: Int): Float {
        val t = time - TITLE_START - i * LETTER_STEP
        if (t <= 0f) return Float.NaN
        val progress = minOf(1f, t / DROP_TIME)
        val drop = (1f - easeOutBounce(progress)) * 300f
        return worldHeight * 0.83f + 30f + drop + MathUtils.sin(time * 2.4f + i * 0.55f) * 6f
    }

    private fun drawTitleShapes(shapes: ShapeRenderer) {
        val y = letterY(0)
        if (y.isNaN()) return
        val left = titleLeft()
        val x = letterX(left, 0)
        val w = game.textWidth("S", TITLE_SCALE)
        val sway = MathUtils.sin(time * 2f) * 4f
        shapes.color = Cosmetics.suitShade
        shapes.triangle(x - 6f, y - 4f, x + w + 8f, y - 4f, x + w + 26f + sway, y + 42f)
        shapes.color = Cosmetics.suit
        shapes.triangle(x - 2f, y - 2f, x + w + 4f, y - 2f, x + w + 22f + sway, y + 40f)
        shapes.color = Palette.SNOW
        Draw.roundedRect(shapes, x - 10f, y - 12f, w + 22f, 14f, 7f)
        shapes.circle(x + w + 24f + sway, y + 42f, 9f, 14)
    }

    private fun drawCoinPill(shapes: ShapeRenderer) {
        val text = game.data.coins.toString()
        val width = game.textWidth(text, 2f) + 74f
        val x = worldWidth - 24f - width
        val y = worldHeight - COIN_Y - 24f
        shapes.color = Palette.GOLD_DARK
        Draw.roundedRect(shapes, x - 3f, y - 3f, width + 6f, 54f, 27f)
        shapes.color = Palette.TOAST_BG
        Draw.roundedRect(shapes, x, y, width, 48f, 24f)
        val bob = MathUtils.sin(time * 3f) * 2f
        Draw.goldenGift(shapes, x + 30f, y + 24f + bob, 28f)
    }

    private fun drawButtonGlow(shapes: ShapeRenderer) {
        val b = gameModeButton.bounds
        if (b.width < 10f) return
        val pulse = (MathUtils.sin(time * 3f) + 1f) / 2f
        tint.set(Palette.GOLD)
        tint.a = 0.12f + 0.12f * pulse
        shapes.color = tint
        Draw.roundedRect(shapes, b.x - 16f, b.y - 18f, b.width + 32f, b.height + 34f, b.height * 0.4f)
        for (slot in slots) {
            if (slot.hover < 0.02f) continue
            val r = slot.button.bounds
            tint.set(Palette.GOLD_LIGHT)
            tint.a = 0.3f * slot.hover
            shapes.color = tint
            Draw.roundedRect(shapes, r.x - 10f, r.y - 12f, r.width + 20f, r.height + 22f, r.height * 0.35f)
        }
    }

    private fun drawSnowCap(shapes: ShapeRenderer, button: Button) {
        val b = button.bounds
        if (b.width < 20f) return
        val top = b.y + b.height + 4f
        val r = b.height * 0.25f
        val count = (b.width / 26f).toInt().coerceAtLeast(3)
        val left = b.x + r * 0.6f
        val right = b.x + b.width - r * 0.6f
        shapes.color = Palette.SNOW_SHADE
        shapes.rect(left, top - 6f, right - left, 6f)
        shapes.color = Palette.SNOW
        for (k in 0..count) {
            val px = MathUtils.lerp(left, right, k / count.toFloat())
            val size = 7f + ((k * 7) % 5) * 1.6f
            shapes.circle(px, top - 1f, size, 10)
        }
        shapes.rect(left, top - 6f, right - left, 6f)
        val drip = ((b.x * 0.37f).toInt() % 3) + 1
        for (k in 1..drip) {
            val px = MathUtils.lerp(left, right, k / (drip + 1f))
            shapes.circle(px, top - 9f, 5f, 8)
        }
    }

    private fun drawHolly(shapes: ShapeRenderer, button: Button) {
        val b = button.bounds
        if (b.width < 40f) return
        val sides = floatArrayOf(b.x + 8f, b.x + b.width - 8f)
        for ((k, x) in sides.withIndex()) {
            val dir = if (k == 0) -1f else 1f
            val y = b.y + b.height - 4f
            shapes.color = Palette.PINE
            leaf(shapes, x, y, x + dir * 30f, y + 14f)
            leaf(shapes, x, y, x + dir * 10f, y + 32f)
            shapes.color = Palette.SANTA_RED
            shapes.circle(x - dir * 2f, y + 4f, 6.5f, 12)
            shapes.circle(x + dir * 7f, y + 8f, 6f, 12)
            shapes.circle(x + dir * 1f, y + 12f, 5.5f, 12)
            shapes.color = Palette.GOLD_LIGHT
            shapes.circle(x - dir * 4f, y + 6f, 1.8f, 6)
        }
    }

    private fun leaf(shapes: ShapeRenderer, x1: Float, y1: Float, x2: Float, y2: Float) {
        val mx = (x1 + x2) / 2f
        val my = (y1 + y2) / 2f
        val nx = -(y2 - y1) * 0.35f
        val ny = (x2 - x1) * 0.35f
        shapes.triangle(x1, y1, mx + nx, my + ny, x2, y2)
        shapes.triangle(x1, y1, mx - nx, my - ny, x2, y2)
    }

    override fun drawTexts() {
        val left = titleLeft()
        for (i in TITLE.indices) {
            val y = letterY(i)
            if (y.isNaN() || TITLE[i] == ' ') continue
            val x = letterX(left, i)
            val second = i > TITLE.indexOf(' ')
            val letter = TITLE[i].toString()
            game.drawText(letter, x + 4f, y - 5f, TITLE_SCALE, if (second) Palette.SANTA_RED_DARK else Palette.GOLD_DARK, centered = false, shadow = false)
            game.drawText(letter, x, y, TITLE_SCALE, if (second) Palette.SNOW else Palette.GOLD, centered = false)
        }
        val subtitleAlpha = MathUtils.clamp((time - SUBTITLE_START) * 2f, 0f, 1f)
        game.drawText(I18n.t("menu.subtitle"), worldWidth / 2f, worldHeight * 0.83f - 64f, 1.5f, Palette.GOLD_LIGHT, alpha = subtitleAlpha)

        val coins = game.data.coins.toString()
        val width = game.textWidth(coins, 2f) + 74f
        game.drawText(coins, worldWidth - 24f - width + 52f, worldHeight - COIN_Y, 2f, Palette.GOLD_LIGHT, centered = false)
    }

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val u = t - 1f
        return 1f + (c1 + 1f) * u * u * u + c1 * u * u
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
        private const val TITLE = "SANTA GAME"
        private const val TITLE_SCALE = 5f
        private const val TITLE_START = 0.05f
        private const val LETTER_STEP = 0.06f
        private const val DROP_TIME = 0.55f
        private const val SUBTITLE_START = 0.7f
        private const val BUTTONS_START = 0.25f
        private const val BUTTON_STEP = 0.09f
        private const val BUTTON_TIME = 0.45f
        private const val HOVER_SPEED = 12f
        private const val HOVER_GROW = 0.06f
        private const val STAR_COUNT = 26
        private const val SLEIGH_CYCLE = 16f
        private const val SLEIGH_DURATION = 7f
        private const val SLEIGH_OFFSET = 13f
        private const val SLEIGH_SCALE = 0.75f
        private const val COIN_Y = 74f
        private val BULBS = listOf(
            Color.valueOf("ff4d5e"),
            Color.valueOf("ffd166"),
            Color.valueOf("4fc3f7"),
            Color.valueOf("7ed957"),
            Color.valueOf("c77dff")
        )
    }
}
