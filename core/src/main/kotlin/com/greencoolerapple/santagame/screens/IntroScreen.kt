package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.input.Touches
import kotlin.random.Random

class IntroScreen(game: SantaGame) : BaseScreen(game) {
    private class Star(val x: Float, val y: Float, val size: Float, val phase: Float)

    private val stars = Random(7).let { random ->
        List(STAR_COUNT) { Star(random.nextFloat(), 0.35f + random.nextFloat() * 0.65f, 1f + random.nextFloat() * 2f, random.nextFloat() * 6f) }
    }
    private val tint = Color()
    private var time = 0f
    private var leaving = -1f

    override fun render(delta: Float) {
        time += delta
        game.scenery.update(delta)
        game.touches.update(game.viewport)

        val skip = game.touches.firstJustDown() >= 0 || Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)
        if (leaving < 0f && (skip || time >= END_TIME)) leaving = 0f
        if (leaving >= 0f) {
            leaving += delta
            if (leaving >= FADE_OUT) {
                for (i in 0 until Touches.MAX_POINTERS) {
                    if (game.touches.down[i]) game.touches.claim(i)
                }
                game.setScreen(MainMenuScreen(game))
                return
            }
        }

        beginFrame()
        val w = worldWidth
        val h = worldHeight
        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)

        shapes.rect(0f, 0f, w, h, Palette.SKY_BOTTOM, Palette.SKY_BOTTOM, Palette.SKY_TOP, Palette.SKY_TOP)
        for (star in stars) {
            val twinkle = 0.4f + 0.6f * (MathUtils.sin(time * 3f + star.phase) + 1f) / 2f
            tint.set(1f, 1f, 1f, twinkle)
            shapes.color = tint
            shapes.circle(star.x * w, star.y * h, star.size, 6)
        }

        val rise = easeOut(minOf(1f, time / 2.2f))
        val moonX = w / 2f
        val moonY = MathUtils.lerp(h * 0.18f, h * 0.56f, rise)
        tint.set(Palette.MOON)
        tint.a = 0.12f
        shapes.color = tint
        shapes.circle(moonX, moonY, MOON_RADIUS + 60f, 48)
        shapes.circle(moonX, moonY, MOON_RADIUS + 28f, 48)
        shapes.color = Palette.MOON
        shapes.circle(moonX, moonY, MOON_RADIUS, 64)
        tint.set(Palette.MOON).lerp(Palette.SNOW_SHADE, 0.4f)
        shapes.color = tint
        shapes.circle(moonX - 50f, moonY + 40f, 26f, 20)
        shapes.circle(moonX + 60f, moonY - 30f, 18f, 18)
        shapes.circle(moonX + 10f, moonY - 70f, 12f, 14)

        drawSleigh(shapes, w, moonY)

        shapes.color = Palette.SNOW_SHADE
        shapes.ellipse(-w * 0.15f, -90f, w * 0.75f, 220f, 48)
        shapes.color = Palette.SNOW
        shapes.ellipse(w * 0.35f, -110f, w * 0.85f, 240f, 48)
        game.scenery.drawSnow(shapes, w)

        val fadeIn = maxOf(0f, 1f - time / FADE_IN)
        val fadeOut = if (leaving >= 0f) minOf(1f, leaving / FADE_OUT) else 0f
        val black = maxOf(fadeIn, fadeOut)
        if (black > 0f) {
            tint.set(0f, 0f, 0f, black)
            shapes.color = tint
            shapes.rect(-10f, -10f, w + 20f, h + 20f)
        }
        shapes.end()

        game.batch.begin()
        val presents = fadeWindow(time, 0.3f, 1.9f)
        if (presents > 0f) {
            game.drawText("GreenCoolerApple", w / 2f, h * 0.56f, 3.4f, Palette.SNOW, alpha = presents)
            game.drawText(I18n.t("intro.presents"), w / 2f, h * 0.47f, 1.8f, Palette.GOLD_LIGHT, alpha = presents)
        }
        drawTitle(w, h)
        if (time > 0.8f && leaving < 0f) {
            game.drawText(I18n.t("intro.skip"), w / 2f, 34f, 1.2f, Palette.SNOW_SHADE, alpha = minOf(1f, (time - 0.8f) * 2f) * 0.8f)
        }
        game.batch.end()
    }

    private fun drawSleigh(shapes: ShapeRenderer, w: Float, moonY: Float) {
        val t = (time - SLEIGH_START) / SLEIGH_DURATION
        if (t <= 0f || t >= 1f) return
        val x = MathUtils.lerp(-360f, w + 360f, t)
        val y = moonY - 30f + MathUtils.sin(t * MathUtils.PI2 * 1.5f) * 26f + (0.5f - t) * 60f
        val gallop = time * 14f

        for (i in 1..12) {
            val trailT = t - i * 0.012f
            if (trailT <= 0f) continue
            val tx = MathUtils.lerp(-360f, w + 360f, trailT) - 60f
            val ty = moonY - 30f + MathUtils.sin(trailT * MathUtils.PI2 * 1.5f) * 26f + (0.5f - trailT) * 60f - 18f
            tint.set(Palette.GOLD_LIGHT)
            tint.a = 0.7f * (1f - i / 12f)
            shapes.color = tint
            shapes.circle(tx + MathUtils.sin(i * 1.7f + time * 8f) * 6f, ty + MathUtils.cos(i * 2.3f) * 8f, 3.5f - i * 0.2f, 8)
        }

        shapes.color = Palette.SILHOUETTE
        for (r in 0 until 3) {
            val rx = x + 150f + r * 78f
            val ry = y + 16f + MathUtils.sin(gallop + r) * 4f
            drawReindeer(shapes, rx, ry, gallop + r * 1.3f)
            shapes.rectLine(x + 60f, y + 22f, rx - 18f, ry + 2f, 2f)
        }

        shapes.circle(x - 30f, y + 30f, 20f, 18)
        shapes.rect(x - 50f, y, 100f, 30f)
        shapes.rect(x - 50f, y + 26f, 30f, 22f)
        shapes.triangle(x + 50f, y, x + 50f, y + 30f, x + 66f, y + 34f)
        shapes.rect(x - 56f, y - 14f, 124f, 5f)
        shapes.circle(x + 68f, y - 6f, 8f, 12)
        shapes.rect(x - 30f, y - 12f, 5f, 14f)
        shapes.rect(x + 30f, y - 12f, 5f, 14f)

        shapes.rect(x - 4f, y + 28f, 28f, 26f)
        shapes.circle(x + 10f, y + 62f, 11f, 14)
        shapes.triangle(x, y + 68f, x + 20f, y + 68f, x + 22f, y + 86f)
        shapes.circle(x + 22f, y + 87f, 4f, 8)
        shapes.rectLine(x + 18f, y + 46f, x + 52f, y + 30f, 5f)
    }

    private fun drawReindeer(shapes: ShapeRenderer, x: Float, y: Float, gallop: Float) {
        shapes.ellipse(x - 26f, y - 9f, 52f, 20f, 16)
        val front = MathUtils.sin(gallop) * 10f
        val backLeg = MathUtils.sin(gallop + MathUtils.PI) * 10f
        shapes.rectLine(x + 16f, y - 4f, x + 22f + front, y - 26f, 3f)
        shapes.rectLine(x + 10f, y - 4f, x + 14f - front, y - 26f, 3f)
        shapes.rectLine(x - 16f, y - 4f, x - 22f + backLeg, y - 24f, 3f)
        shapes.rectLine(x - 10f, y - 4f, x - 14f - backLeg, y - 24f, 3f)
        shapes.rectLine(x + 20f, y + 4f, x + 30f, y + 20f, 7f)
        shapes.ellipse(x + 26f, y + 16f, 18f, 9f, 12)
        shapes.rectLine(x + 30f, y + 24f, x + 26f, y + 38f, 2f)
        shapes.rectLine(x + 27f, y + 33f, x + 20f, y + 38f, 2f)
        shapes.rectLine(x + 33f, y + 24f, x + 38f, y + 37f, 2f)
        shapes.rectLine(x + 36f, y + 32f, x + 43f, y + 36f, 2f)
        shapes.rectLine(x - 26f, y + 2f, x - 32f, y + 8f, 3f)
    }

    private fun drawTitle(w: Float, h: Float) {
        val title = "SANTA GAME"
        val scale = 6f
        val total = game.textWidth(title, scale)
        val left = w / 2f - total / 2f
        for (i in title.indices) {
            val t = time - TITLE_START - i * 0.07f
            if (t <= 0f) continue
            val progress = minOf(1f, t / 0.6f)
            val offset = (1f - easeOutBounce(progress)) * 360f
            val x = left + game.textWidth(title.substring(0, i), scale)
            game.drawText(title[i].toString(), x, h * 0.84f + offset + 40f, scale, Palette.GOLD, centered = false, alpha = minOf(1f, t * 6f))
        }
    }

    private fun fadeWindow(t: Float, start: Float, end: Float): Float {
        if (t <= start || t >= end) return 0f
        val fade = 0.35f
        return minOf(1f, (t - start) / fade, (end - t) / fade)
    }

    private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)

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
        private const val STAR_COUNT = 70
        private const val MOON_RADIUS = 170f
        private const val SLEIGH_START = 1.5f
        private const val SLEIGH_DURATION = 2.6f
        private const val TITLE_START = 3.6f
        private const val END_TIME = 6.2f
        private const val FADE_IN = 0.8f
        private const val FADE_OUT = 0.5f
    }
}
