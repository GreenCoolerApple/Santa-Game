package com.greencoolerapple.santagame.world

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.greencoolerapple.santagame.Palette

class FallingItem(val type: Type, var x: Float, var y: Float, val speed: Float) {
    enum class Type { GIFT, COAL }

    val size = if (type == Type.GIFT) MathUtils.random(44f, 56f) else MathUtils.random(36f, 44f)
    val bounds = Rectangle()

    private val colorIndex = MathUtils.random(Palette.GIFT_BOXES.size - 1)
    private var sway = MathUtils.random(0f, MathUtils.PI2)

    fun update(delta: Float) {
        y -= speed * delta
        sway += delta * 3f
        bounds.set(x - size / 2f, y - size / 2f, size, size)
    }

    fun draw(shapes: ShapeRenderer) {
        when (type) {
            Type.GIFT -> drawGift(shapes)
            Type.COAL -> drawCoal(shapes)
        }
    }

    private fun drawGift(shapes: ShapeRenderer) {
        val half = size / 2f
        val left = x - half
        val bottom = y - half
        val ribbon = size * 0.18f
        val wobble = MathUtils.sin(sway) * 3f

        shapes.color = Palette.GIFT_BOXES[colorIndex]
        shapes.rect(left, bottom, size, size * 0.82f)
        shapes.rect(left - 4f, bottom + size * 0.72f, size + 8f, size * 0.2f)

        shapes.color = Palette.GIFT_RIBBONS[colorIndex]
        shapes.rect(x - ribbon / 2f, bottom, ribbon, size * 0.92f)
        shapes.rect(left, bottom + size * 0.38f, size, ribbon)

        val top = bottom + size * 0.92f
        shapes.triangle(x, top, x - half * 0.75f, top + half * 0.5f + wobble, x - half * 0.7f, top - 2f)
        shapes.triangle(x, top, x + half * 0.75f, top + half * 0.5f - wobble, x + half * 0.7f, top - 2f)
        shapes.circle(x, top, ribbon * 0.6f, 12)
    }

    private fun drawCoal(shapes: ShapeRenderer) {
        val r = size / 2f
        shapes.color = Palette.COAL
        shapes.circle(x, y, r, 18)
        shapes.circle(x - r * 0.5f, y + r * 0.35f, r * 0.6f, 14)
        shapes.circle(x + r * 0.45f, y - r * 0.3f, r * 0.65f, 14)
        shapes.color = Palette.COAL_SHINE
        shapes.circle(x - r * 0.3f, y + r * 0.3f, r * 0.18f, 8)
        shapes.circle(x + r * 0.35f, y - r * 0.1f, r * 0.12f, 8)
    }
}
