package com.greencoolerapple.santagame.world

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.ui.Draw

class Santa(var x: Float, private val baseY: Float) {
    var targetX = x
    val catchBounds = Rectangle()

    private var bobTime = 0f
    private var lean = 0f

    fun update(delta: Float, minX: Float, maxX: Float) {
        val previous = x
        targetX = MathUtils.clamp(targetX, minX + HALF_WIDTH, maxX - HALF_WIDTH)
        x = MathUtils.lerp(x, targetX, minOf(1f, delta * FOLLOW_SPEED))
        val velocity = if (delta > 0f) (x - previous) / delta else 0f
        lean = MathUtils.lerp(lean, MathUtils.clamp(velocity / 900f, -1f, 1f), minOf(1f, delta * 10f))
        bobTime += delta * (4f + kotlin.math.abs(velocity) / 120f)
        catchBounds.set(x - HALF_WIDTH, baseY + 40f, HALF_WIDTH * 2f, 130f)
    }

    fun idle(delta: Float) {
        bobTime += delta * 3f
        catchBounds.set(x - HALF_WIDTH, baseY + 40f, HALF_WIDTH * 2f, 130f)
    }

    fun draw(shapes: ShapeRenderer) {
        val cx = x
        val y = baseY + kotlin.math.abs(MathUtils.sin(bobTime)) * 5f
        val tilt = lean * 10f

        shapes.color = Palette.BLACK
        shapes.rect(cx - 40f, baseY, 32f, 20f)
        shapes.rect(cx + 8f, baseY, 32f, 20f)

        shapes.color = Palette.SANTA_RED
        shapes.rect(cx - 44f, y + 18f, 88f, 64f)
        shapes.circle(cx - 50f + tilt, y + 66f, 15f, 20)
        shapes.circle(cx + 50f + tilt, y + 66f, 15f, 20)
        shapes.color = Palette.BLACK
        shapes.circle(cx - 56f + tilt, y + 56f, 10f, 16)
        shapes.circle(cx + 56f + tilt, y + 56f, 10f, 16)

        shapes.color = Palette.SNOW
        shapes.rect(cx - 46f, y + 16f, 92f, 12f)
        shapes.rect(cx - 6f, y + 28f, 12f, 54f)

        shapes.color = Palette.BLACK
        shapes.rect(cx - 44f, y + 42f, 88f, 12f)
        shapes.color = Palette.GOLD
        shapes.rect(cx - 11f, y + 39f, 22f, 18f)
        shapes.color = Palette.BLACK
        shapes.rect(cx - 5f, y + 44f, 10f, 8f)

        val hx = cx + tilt
        shapes.color = Palette.SNOW
        shapes.circle(hx, y + 88f, 32f, 28)
        shapes.color = Palette.SKIN
        shapes.circle(hx, y + 104f, 24f, 28)
        shapes.color = Palette.SNOW
        shapes.ellipse(hx - 20f, y + 90f, 40f, 12f, 20)
        shapes.color = Palette.NOSE
        shapes.circle(hx, y + 100f, 5.5f, 14)
        shapes.color = Palette.BLACK
        shapes.circle(hx - 9f, y + 109f, 3.5f, 10)
        shapes.circle(hx + 9f, y + 109f, 3.5f, 10)

        shapes.color = Palette.SANTA_RED
        shapes.triangle(hx - 26f, y + 120f, hx + 26f, y + 120f, hx + 30f - tilt * 2f, y + 166f)
        shapes.color = Palette.SANTA_RED_DARK
        shapes.triangle(hx + 6f, y + 120f, hx + 26f, y + 120f, hx + 30f - tilt * 2f, y + 166f)
        shapes.color = Palette.SNOW
        shapes.rect(hx - 30f, y + 114f, 60f, 13f)
        shapes.circle(hx + 30f - tilt * 2f, y + 166f, 9f, 16)
    }

    fun drawDizzy(shapes: ShapeRenderer, time: Float) {
        val cy = baseY + 178f
        for (i in 0 until 3) {
            val angle = time * 220f + i * 120f
            val sx = x + MathUtils.cosDeg(angle) * 44f
            val sy = cy + MathUtils.sinDeg(angle) * 12f
            shapes.color = Palette.GOLD
            Draw.star(shapes, sx, sy, 11f, time * 180f + i * 40f)
        }
    }

    companion object {
        const val HALF_WIDTH = 58f
        private const val FOLLOW_SPEED = 12f
    }
}
