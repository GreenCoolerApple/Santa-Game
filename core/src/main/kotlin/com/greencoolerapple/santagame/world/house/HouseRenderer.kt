package com.greencoolerapple.santagame.world.house

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.Palette

object HouseRenderer {
    private val glow = Color()

    fun drawRoom(shapes: ShapeRenderer, house: HouseLayout, width: Float, height: Float, time: Float, exitOpen: Boolean) {
        shapes.rect(0f, 0f, width, height, Palette.SKY_BOTTOM, Palette.SKY_BOTTOM, Palette.SKY_TOP, Palette.SKY_TOP)

        val room = house.room
        val inner = house.interior
        shapes.color = Palette.WALL
        shapes.rect(room.x, room.y, room.width, room.height)
        shapes.color = Palette.FLOOR
        shapes.rect(inner.x, inner.y, inner.width, inner.height)
        shapes.color = Palette.FLOOR_LINE
        var lineY = inner.y + 40f
        while (lineY < inner.y + inner.height) {
            shapes.rect(inner.x, lineY, inner.width, 3f)
            lineY += 40f
        }

        val rugX = inner.x + inner.width * 0.5f
        val rugY = inner.y + inner.height * 0.5f
        shapes.color = Palette.RUG_BORDER
        shapes.ellipse(rugX - 230f, rugY - 135f, 460f, 270f, 40)
        shapes.color = Palette.RUG
        shapes.ellipse(rugX - 216f, rugY - 121f, 432f, 242f, 40)

        drawFireplace(shapes, house, time, exitOpen)

        for (piece in house.furniture) {
            val r = piece.bounds
            val base = Palette.FURNITURE[piece.style % Palette.FURNITURE.size]
            shapes.color = Palette.SHADOW
            shapes.rect(r.x + 5f, r.y - 6f, r.width, r.height)
            shapes.color = base
            shapes.rect(r.x, r.y, r.width, r.height)
            glow.set(base).lerp(Color.WHITE, 0.18f)
            shapes.color = glow
            shapes.rect(r.x + 8f, r.y + 8f, r.width - 16f, r.height - 16f)
        }
    }

    private fun drawFireplace(shapes: ShapeRenderer, house: HouseLayout, time: Float, exitOpen: Boolean) {
        val f = house.fireplace
        shapes.color = Palette.BRICK
        shapes.rect(f.x - 20f, f.y - 14f, f.width + 14f, f.height + 28f)
        shapes.color = Palette.HEARTH
        shapes.rect(f.x - 20f, f.y + 6f, f.width - 6f, f.height - 12f)

        val flicker = 0.85f + MathUtils.sin(time * 11f) * 0.08f + MathUtils.sin(time * 17f) * 0.05f
        val cx = f.x + 4f
        val cy = f.y + f.height / 2f
        if (exitOpen) {
            glow.set(Palette.FIRE_CORE)
            glow.a = 0.18f + 0.12f * (MathUtils.sin(time * 5f) + 1f) / 2f
            shapes.color = glow
            shapes.circle(cx + 20f, cy, 90f, 32)
        }
        shapes.color = Palette.FIRE
        shapes.circle(cx, cy - 18f, 18f * flicker, 14)
        shapes.circle(cx, cy + 16f, 16f * flicker, 14)
        shapes.circle(cx + 6f, cy, 20f * flicker, 14)
        shapes.color = Palette.FIRE_CORE
        shapes.circle(cx + 4f, cy, 10f * flicker, 12)
    }

    fun drawTree(shapes: ShapeRenderer, house: HouseLayout, gifts: Int, time: Float) {
        val x = house.tree.x
        val y = house.tree.y
        val r = HouseLayout.TREE_RADIUS

        for (i in 0 until gifts) {
            val angle = 200f + i * 32f
            val gx = x + MathUtils.cosDeg(angle) * (r + 14f)
            val gy = y + MathUtils.sinDeg(angle) * (r + 14f)
            val box = Palette.GIFT_BOXES[i % Palette.GIFT_BOXES.size]
            val ribbon = Palette.GIFT_RIBBONS[i % Palette.GIFT_RIBBONS.size]
            shapes.color = Palette.SHADOW
            shapes.rect(gx - 11f, gy - 14f, 24f, 24f)
            shapes.color = box
            shapes.rect(gx - 13f, gy - 12f, 26f, 26f)
            shapes.color = ribbon
            shapes.rect(gx - 3f, gy - 12f, 6f, 26f)
            shapes.rect(gx - 13f, gy - 2f, 26f, 6f)
        }

        shapes.color = Palette.SHADOW
        shapes.circle(x + 5f, y - 7f, r + 2f, 32)
        shapes.color = Palette.TREE
        shapes.circle(x, y, r, 32)
        shapes.color = Palette.TREE_LIGHT
        shapes.circle(x, y, r * 0.7f, 28)
        shapes.color = Palette.TREE
        shapes.circle(x, y, r * 0.42f, 24)

        for (i in 0 until 8) {
            val angle = i * 45f + 20f
            val dist = if (i % 2 == 0) r * 0.82f else r * 0.55f
            val blink = (MathUtils.sin(time * 3f + i) + 1f) / 2f
            glow.set(Palette.GIFT_BOXES[i % Palette.GIFT_BOXES.size]).lerp(Color.WHITE, blink * 0.4f)
            shapes.color = glow
            shapes.circle(x + MathUtils.cosDeg(angle) * dist, y + MathUtils.sinDeg(angle) * dist, 5f, 10)
        }

        shapes.color = Palette.GOLD
        shapes.circle(x, y, 9f, 5)
        shapes.circle(x, y, 6f, 12)
    }
}
