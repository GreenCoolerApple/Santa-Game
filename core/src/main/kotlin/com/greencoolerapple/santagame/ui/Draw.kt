package com.greencoolerapple.santagame.ui

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.Palette

object Draw {
    fun roundedRect(shapes: ShapeRenderer, x: Float, y: Float, width: Float, height: Float, radius: Float) {
        val r = minOf(radius, width / 2f, height / 2f)
        shapes.rect(x + r, y, width - r * 2f, height)
        shapes.rect(x, y + r, r, height - r * 2f)
        shapes.rect(x + width - r, y + r, r, height - r * 2f)
        shapes.circle(x + r, y + r, r, 16)
        shapes.circle(x + width - r, y + r, r, 16)
        shapes.circle(x + r, y + height - r, r, 16)
        shapes.circle(x + width - r, y + height - r, r, 16)
    }

    fun ring(shapes: ShapeRenderer, cx: Float, cy: Float, inner: Float, outer: Float, segments: Int = 64) {
        val step = MathUtils.PI2 / segments
        for (i in 0 until segments) {
            val a0 = i * step
            val a1 = a0 + step
            val c0 = MathUtils.cos(a0)
            val s0 = MathUtils.sin(a0)
            val c1 = MathUtils.cos(a1)
            val s1 = MathUtils.sin(a1)
            shapes.triangle(cx + c0 * inner, cy + s0 * inner, cx + c0 * outer, cy + s0 * outer, cx + c1 * outer, cy + s1 * outer)
            shapes.triangle(cx + c0 * inner, cy + s0 * inner, cx + c1 * outer, cy + s1 * outer, cx + c1 * inner, cy + s1 * inner)
        }
    }

    fun goldenGift(shapes: ShapeRenderer, cx: Float, cy: Float, size: Float) {
        val half = size / 2f
        shapes.color = Palette.GOLD_DARK
        shapes.rect(cx - half, cy - half - size * 0.06f, size, size * 0.8f)
        shapes.color = Palette.GOLD
        shapes.rect(cx - half, cy - half, size, size * 0.74f)
        shapes.rect(cx - half - size * 0.06f, cy + size * 0.12f, size * 1.12f, size * 0.22f)
        shapes.color = Palette.GOLD_LIGHT
        shapes.rect(cx - size * 0.09f, cy - half, size * 0.18f, size * 0.96f)
        shapes.circle(cx - size * 0.2f, cy + half + size * 0.04f, size * 0.17f, 12)
        shapes.circle(cx + size * 0.2f, cy + half + size * 0.04f, size * 0.17f, 12)
    }

    fun heart(shapes: ShapeRenderer, cx: Float, cy: Float, size: Float) {
        shapes.circle(cx - size * 0.25f, cy, size * 0.28f, 16)
        shapes.circle(cx + size * 0.25f, cy, size * 0.28f, 16)
        shapes.triangle(cx - size * 0.52f, cy - size * 0.08f, cx + size * 0.52f, cy - size * 0.08f, cx, cy - size * 0.62f)
    }

    fun star(shapes: ShapeRenderer, cx: Float, cy: Float, outer: Float, rotation: Float) {
        val inner = outer * 0.45f
        for (i in 0 until 5) {
            val a = rotation + i * 72f
            val ox = cx + MathUtils.cosDeg(a + 90f) * outer
            val oy = cy + MathUtils.sinDeg(a + 90f) * outer
            val lx = cx + MathUtils.cosDeg(a + 54f) * inner
            val ly = cy + MathUtils.sinDeg(a + 54f) * inner
            val rx = cx + MathUtils.cosDeg(a + 126f) * inner
            val ry = cy + MathUtils.sinDeg(a + 126f) * inner
            shapes.triangle(ox, oy, lx, ly, rx, ry)
            shapes.triangle(cx, cy, lx, ly, rx, ry)
        }
    }
}
