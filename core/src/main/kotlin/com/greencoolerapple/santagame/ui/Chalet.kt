package com.greencoolerapple.santagame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.greencoolerapple.santagame.Palette

object Chalet {
    val BULBS = listOf(
        Color.valueOf("ff4d5e"),
        Color.valueOf("ffd166"),
        Color.valueOf("4fc3f7"),
        Color.valueOf("7ed957"),
        Color.valueOf("c77dff")
    )

    private val tint = Color()

    fun drawWall(shapes: ShapeRenderer, w: Float, h: Float) {
        var y = 0f
        var row = 0
        while (y < h) {
            shapes.color = if (row % 2 == 0) Palette.LOG else Palette.LOG_DARK
            shapes.rect(0f, y, w, 34f)
            shapes.color = Palette.LOG_LINE
            shapes.rect(0f, y, w, 3f)
            var kx = (row * 173f) % 260f
            while (kx < w) {
                shapes.circle(kx, y + 17f, 3f, 8)
                kx += 260f
            }
            y += 34f
            row++
        }
        shapes.color = Palette.FLOOR_WOOD
        shapes.rect(0f, 0f, w, 30f)
        shapes.color = Palette.LOG_LINE
        shapes.rect(0f, 28f, w, 4f)
        var px = 0f
        while (px < w) {
            shapes.rect(px, 0f, 3f, 28f)
            px += 120f
        }
    }

    fun drawGarland(shapes: ShapeRenderer, left: Float, right: Float, h: Float, time: Float) {
        val width = right - left
        val swags = maxOf(2, (width / 260f).toInt())
        val span = width / swags
        val top = h + 4f
        var bulb = 0
        for (k in 0 until swags) {
            val x0 = left + k * span
            for (j in 0 until 12) {
                val a = j / 12f
                val b = (j + 1) / 12f
                shapes.color = Palette.PINE_DARK
                shapes.rectLine(x0 + a * span, top - 120f * a * (1f - a), x0 + b * span, top - 120f * b * (1f - b), 10f)
            }
            for (j in 1 until 4) {
                val a = j / 4f
                val bx = x0 + a * span
                val by = top - 120f * a * (1f - a) - 9f
                val color = BULBS[bulb % BULBS.size]
                if ((bulb + (time * 3f).toInt()) % 3 == 0) {
                    tint.set(color)
                    tint.a = 0.3f
                    shapes.color = tint
                    shapes.circle(bx, by - 3f, 13f, 12)
                }
                shapes.color = color
                shapes.ellipse(bx - 5f, by - 11f, 10f, 14f, 10)
                bulb++
            }
        }
    }

    fun drawWreath(shapes: ShapeRenderer, x: Float, y: Float) {
        shapes.color = Palette.PINE_DARK
        Draw.ring(shapes, x, y, 18f, 36f, 28)
        shapes.color = Palette.PINE
        for (k in 0 until 12) {
            val a = k * MathUtils.PI2 / 12f + 0.2f
            shapes.circle(x + MathUtils.cos(a) * 27f, y + MathUtils.sin(a) * 27f, 9f, 10)
        }
        for (k in 0 until 6) {
            val a = k * MathUtils.PI2 / 6f
            shapes.color = if (k % 2 == 0) Palette.SANTA_RED else Palette.GOLD
            shapes.circle(x + MathUtils.cos(a) * 27f, y + MathUtils.sin(a) * 27f, 5f, 8)
        }
        shapes.color = Palette.SANTA_RED
        shapes.triangle(x, y - 28f, x - 18f, y - 50f, x - 4f, y - 50f)
        shapes.triangle(x, y - 28f, x + 18f, y - 50f, x + 4f, y - 50f)
        shapes.circle(x, y - 28f, 7f, 10)
    }

    fun drawWindow(shapes: ShapeRenderer, x: Float, y: Float, ww: Float, wh: Float, time: Float) {
        shapes.color = Palette.LOG_LINE
        shapes.rect(x - 10f, y - 10f, ww + 20f, wh + 20f)
        shapes.color = Palette.WINDOW_NIGHT
        shapes.rect(x, y, ww, wh)
        shapes.color = Palette.MOON
        shapes.circle(x + ww * 0.7f, y + wh * 0.72f, 16f, 16)
        shapes.color = Palette.SNOW
        for (k in 0 until 9) {
            val fx = x + 10f + (k * 47f) % (ww - 20f)
            val fy = y + wh - ((time * (30f + k * 4f) + k * 37f) % wh)
            shapes.circle(fx + MathUtils.sin(time + k) * 4f, fy, 2.5f, 6)
        }
        shapes.rect(x, y, ww, 18f)
        shapes.color = Palette.LOG_LINE
        shapes.rect(x + ww / 2f - 4f, y, 8f, wh)
        shapes.rect(x, y + wh / 2f - 4f, ww, 8f)
        shapes.color = Palette.SNOW
        Draw.roundedRect(shapes, x - 18f, y - 22f, ww + 36f, 16f, 8f)
        shapes.color = Palette.SANTA_RED
        shapes.triangle(x - 10f, y + wh + 10f, x + 30f, y + wh + 10f, x - 10f, y + wh - 40f)
        shapes.triangle(x + ww + 10f, y + wh + 10f, x + ww - 30f, y + wh + 10f, x + ww + 10f, y + wh - 40f)
    }

    fun drawBoard(shapes: ShapeRenderer, panel: Rectangle) {
        shapes.color = Palette.SHOP_PANEL_EDGE
        Draw.roundedRect(shapes, panel.x - 8f, panel.y - 8f, panel.width + 16f, panel.height + 16f, 22f)
        val stripe = 22f
        var sx = panel.x - 8f
        var k = 0
        val end = panel.x + panel.width + 8f
        while (sx < end) {
            shapes.color = if (k % 2 == 0) Palette.SANTA_RED else Palette.SNOW
            val sw = minOf(stripe, end - sx)
            shapes.rect(sx, panel.y + panel.height + 2f, sw, 6f)
            shapes.rect(sx, panel.y - 8f, sw, 6f)
            sx += stripe
            k++
        }
        shapes.color = Palette.SHOP_PANEL
        Draw.roundedRect(shapes, panel.x, panel.y, panel.width, panel.height, 16f)
    }

    fun drawScrollbar(shapes: ShapeRenderer, clip: Rectangle, scroll: Float, maxScroll: Float) {
        if (maxScroll <= 0f) return
        val trackX = clip.x + clip.width - 7f
        shapes.color = Palette.PANEL_GREEN_DARK
        Draw.roundedRect(shapes, trackX, clip.y, 7f, clip.height, 3.5f)
        val thumb = clip.height * clip.height / (clip.height + maxScroll)
        val thumbY = clip.y + (clip.height - thumb) * (1f - scroll / maxScroll)
        shapes.color = Palette.GOLD
        Draw.roundedRect(shapes, trackX, thumbY, 7f, thumb, 3.5f)
    }

    fun drawBubble(shapes: ShapeRenderer, r: Rectangle, tailX: Float, tailBottom: Float) {
        shapes.color = Palette.BUBBLE_EDGE
        Draw.roundedRect(shapes, r.x - 4f, r.y - 4f, r.width + 8f, r.height + 8f, 18f)
        shapes.triangle(tailX, tailBottom - 4f, tailX - 16f, r.y + 2f, tailX + 16f, r.y + 2f)
        shapes.color = Palette.BUBBLE
        Draw.roundedRect(shapes, r.x, r.y, r.width, r.height, 14f)
        shapes.triangle(tailX, tailBottom + 4f, tailX - 10f, r.y + 4f, tailX + 10f, r.y + 4f)
    }

    fun drawNameTag(shapes: ShapeRenderer, x: Float, y: Float, width: Float) {
        shapes.color = Palette.SANTA_RED
        Draw.roundedRect(shapes, x, y, width, 26f, 13f)
    }
}
