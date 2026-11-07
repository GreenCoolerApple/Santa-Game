package com.greencoolerapple.santagame.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.ui.Draw

class Elf(val nameKey: String, private val hair: Color, private val band: Color, private val phase: Float) {
    var x = 0f
    var base = 0f
    var scale = 1f
    var talking = false
    var laughTimer = 0f
    var bounce = 0f

    private var blinkTimer = 2f + phase
    private var time = phase

    val headX: Float
        get() = x

    val headY: Float
        get() = base + (118f + bob()) * scale

    val laughing: Boolean
        get() = laughTimer > 0f

    fun update(delta: Float) {
        time += delta
        laughTimer = maxOf(0f, laughTimer - delta)
        bounce = maxOf(0f, bounce - delta * 3f)
        blinkTimer -= delta
        if (blinkTimer < -0.14f) blinkTimer = 2.6f + MathUtils.random(0f, 2.4f)
    }

    fun contains(px: Float, py: Float): Boolean {
        val dx = px - x
        val dy = py - (base + 90f * scale)
        val r = 80f * scale
        return dx * dx + dy * dy < r * r
    }

    private fun bob(): Float {
        val laugh = if (laughing) kotlin.math.abs(MathUtils.sin(time * 14f)) * 7f else 0f
        return MathUtils.sin(time * 2f) * 2.5f + laugh + MathUtils.sin(bounce * 10f) * bounce * 14f
    }

    fun draw(shapes: ShapeRenderer) {
        val s = scale
        val b = bob() * s
        val cx = x
        val hy = headY

        shapes.color = Palette.ELF_GREEN_DARK
        shapes.ellipse(cx - 56f * s, base - 30f * s + b * 0.5f, 112f * s, 132f * s, 24)
        shapes.color = Palette.ELF_GREEN
        shapes.ellipse(cx - 50f * s, base - 26f * s + b * 0.5f, 100f * s, 122f * s, 24)
        shapes.color = Palette.SANTA_RED
        for (k in 0 until 6) {
            val a = MathUtils.PI + k * MathUtils.PI / 5f
            shapes.circle(cx + MathUtils.cos(a) * 34f * s, hy - 34f * s + MathUtils.sin(a) * 8f * s, 10f * s, 10)
        }

        shapes.color = Palette.SKIN
        shapes.triangle(cx - 30f * s, hy + 2f * s, cx - 30f * s, hy - 16f * s, cx - 62f * s, hy + 26f * s)
        shapes.triangle(cx + 30f * s, hy + 2f * s, cx + 30f * s, hy - 16f * s, cx + 62f * s, hy + 26f * s)
        shapes.circle(cx, hy, 35f * s, 26)

        shapes.color = hair
        for (k in 0 until 5) shapes.circle(cx - 26f * s + k * 13f * s, hy + 23f * s, 9f * s, 10)

        shapes.color = Palette.BLUSH
        shapes.circle(cx - 20f * s, hy - 9f * s, 7f * s, 12)
        shapes.circle(cx + 20f * s, hy - 9f * s, 7f * s, 12)

        for (side in intArrayOf(-1, 1)) {
            val ex = cx + side * 13f * s
            val ey = hy + 5f * s
            shapes.color = Palette.BLACK
            when {
                laughing -> {
                    shapes.rectLine(ex - 6f * s, ey - 2f * s, ex, ey + 4f * s, 2.6f * s)
                    shapes.rectLine(ex, ey + 4f * s, ex + 6f * s, ey - 2f * s, 2.6f * s)
                }
                blinkTimer < 0f -> shapes.rect(ex - 5f * s, ey - 1f * s, 10f * s, 2.4f * s)
                else -> {
                    shapes.ellipse(ex - 4.5f * s, ey - 6f * s, 9f * s, 12f * s, 12)
                    shapes.color = Palette.SNOW
                    shapes.circle(ex + 1.5f * s, ey + 2f * s, 2f * s, 8)
                }
            }
        }
        shapes.color = Palette.NOSE
        shapes.circle(cx, hy - 3f * s, 5f * s, 10)

        val open = when {
            laughing -> 0.75f + kotlin.math.abs(MathUtils.sin(time * 14f)) * 0.25f
            talking -> kotlin.math.abs(MathUtils.sin(time * 16f))
            else -> 0f
        }
        shapes.color = Palette.MOUTH
        shapes.ellipse(cx - 9f * s, hy - 20f * s - open * 6f * s, 18f * s, (6f + open * 10f) * s, 12)
        if (open < 0.2f) {
            shapes.color = Palette.SKIN
            shapes.ellipse(cx - 10f * s, hy - 15f * s, 20f * s, 6f * s, 12)
        } else {
            shapes.color = Palette.TONGUE
            shapes.ellipse(cx - 5f * s, hy - 20f * s - open * 6f * s, 10f * s, 4f * s, 10)
        }

        val sway = MathUtils.sin(time * 1.8f + phase) * 7f * s
        shapes.color = Palette.ELF_GREEN_DARK
        shapes.triangle(cx - 38f * s, hy + 23f * s, cx + 38f * s, hy + 23f * s, cx + 38f * s + sway, hy + 74f * s)
        shapes.color = Palette.ELF_GREEN
        shapes.triangle(cx - 33f * s, hy + 26f * s, cx + 30f * s, hy + 26f * s, cx + 35f * s + sway, hy + 69f * s)
        shapes.triangle(cx + 38f * s + sway, hy + 74f * s, cx + 30f * s + sway, hy + 60f * s, cx + 62f * s + sway, hy + 57f * s)
        shapes.color = band
        Draw.roundedRect(shapes, cx - 41f * s, hy + 16f * s, 82f * s, 14f * s, 7f * s)
        shapes.color = Palette.GOLD
        shapes.circle(cx + 64f * s + sway, hy + 56f * s, 7.5f * s, 12)
    }

    fun drawArms(shapes: ShapeRenderer, tableTop: Float) {
        val s = scale
        val b = bob() * s * 0.5f
        val wave = if (talking) MathUtils.sin(time * 9f) * 10f * s else 0f
        shapes.color = Palette.ELF_GREEN_DARK
        shapes.rectLine(cx(-44f), base + 70f * s + b, cx(-60f), tableTop + 6f * s, 18f * s)
        if (talking) {
            shapes.rectLine(cx(44f), base + 70f * s + b, cx(72f), base + 66f * s + wave, 18f * s)
        } else {
            shapes.rectLine(cx(44f), base + 70f * s + b, cx(60f), tableTop + 6f * s, 18f * s)
        }
        shapes.color = Palette.SKIN
        shapes.circle(cx(-60f), tableTop + 4f * s, 10f * s, 12)
        if (talking) shapes.circle(cx(76f), base + 74f * s + wave, 10f * s, 12) else shapes.circle(cx(60f), tableTop + 4f * s, 10f * s, 12)
    }

    private fun cx(offset: Float): Float = x + offset * scale
}
