package com.greencoolerapple.santagame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame

class Button(
    var label: String,
    var sublabel: String? = null,
    var fill: Color = Palette.BUTTON,
    var edge: Color = Palette.BUTTON_EDGE,
    var labelScale: Float = 2.4f
) {
    val bounds = Rectangle()

    fun place(centerX: Float, centerY: Float, width: Float, height: Float) {
        bounds.set(centerX - width / 2f, centerY - height / 2f, width, height)
    }

    fun contains(x: Float, y: Float): Boolean = bounds.contains(x, y)

    fun drawShape(shapes: ShapeRenderer, fill: Color = this.fill, edge: Color = this.edge) {
        if (bounds.width <= 1f || bounds.height <= 1f) return
        val r = bounds.height * 0.25f
        shapes.color = edge
        Draw.roundedRect(shapes, bounds.x - 4f, bounds.y - 6f, bounds.width + 8f, bounds.height + 10f, r + 4f)
        shapes.color = fill
        Draw.roundedRect(shapes, bounds.x, bounds.y, bounds.width, bounds.height, r)
    }

    fun drawLabel(game: SantaGame, scale: Float = labelScale, alpha: Float = 1f) {
        val cx = bounds.x + bounds.width / 2f
        val cy = bounds.y + bounds.height / 2f
        val sub = sublabel
        if (sub == null) {
            game.drawText(label, cx, cy, scale, alpha = alpha)
        } else {
            game.drawText(label, cx, cy + bounds.height * 0.13f, scale, alpha = alpha)
            game.drawText(sub, cx, cy - bounds.height * 0.24f, scale * 0.55f, Palette.GOLD, alpha = alpha)
        }
    }
}
