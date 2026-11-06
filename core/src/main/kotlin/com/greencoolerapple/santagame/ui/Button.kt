package com.greencoolerapple.santagame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame

class Button(var label: String, var sublabel: String? = null) {
    val bounds = Rectangle()

    fun place(centerX: Float, centerY: Float, width: Float, height: Float) {
        bounds.set(centerX - width / 2f, centerY - height / 2f, width, height)
    }

    fun contains(x: Float, y: Float): Boolean = bounds.contains(x, y)

    fun drawShape(shapes: ShapeRenderer, fill: Color = Palette.BUTTON, edge: Color = Palette.BUTTON_EDGE) {
        val r = bounds.height * 0.25f
        shapes.color = edge
        roundedRect(shapes, bounds.x - 4f, bounds.y - 6f, bounds.width + 8f, bounds.height + 10f, r + 4f)
        shapes.color = fill
        roundedRect(shapes, bounds.x, bounds.y, bounds.width, bounds.height, r)
    }

    fun drawLabel(game: SantaGame, scale: Float) {
        val cx = bounds.x + bounds.width / 2f
        val cy = bounds.y + bounds.height / 2f
        val sub = sublabel
        if (sub == null) {
            game.drawText(label, cx, cy, scale)
        } else {
            game.drawText(label, cx, cy + bounds.height * 0.13f, scale)
            game.drawText(sub, cx, cy - bounds.height * 0.24f, scale * 0.55f, Palette.GOLD)
        }
    }

    companion object {
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
    }
}
