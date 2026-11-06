package com.greencoolerapple.santagame.world

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame

class Scenery {
    private class Flake(var x: Float, var y: Float, val radius: Float, val speed: Float, val sway: Float, var phase: Float)

    private val flakes = List(FLAKE_COUNT) {
        Flake(
            x = MathUtils.random(0f, 1f),
            y = MathUtils.random(0f, SantaGame.WORLD_HEIGHT),
            radius = MathUtils.random(1.5f, 4.5f),
            speed = MathUtils.random(25f, 70f),
            sway = MathUtils.random(10f, 30f),
            phase = MathUtils.random(0f, MathUtils.PI2)
        )
    }

    private val treeSlots = floatArrayOf(0.06f, 0.17f, 0.31f, 0.52f, 0.68f, 0.83f, 0.95f)
    private val treeScales = floatArrayOf(1.1f, 0.8f, 1.3f, 0.9f, 1.2f, 0.75f, 1.05f)

    fun update(delta: Float) {
        for (flake in flakes) {
            flake.phase += delta
            flake.y -= flake.speed * delta
            if (flake.y < -10f) {
                flake.y = SantaGame.WORLD_HEIGHT + 10f
                flake.x = MathUtils.random(0f, 1f)
            }
        }
    }

    fun drawBackground(shapes: ShapeRenderer, width: Float, height: Float) {
        shapes.rect(0f, 0f, width, height, Palette.SKY_BOTTOM, Palette.SKY_BOTTOM, Palette.SKY_TOP, Palette.SKY_TOP)

        shapes.color = Palette.MOON
        shapes.circle(width * 0.82f, height * 0.8f, 46f, 40)
        shapes.color = Palette.SKY_TOP
        shapes.circle(width * 0.82f + 18f, height * 0.8f + 12f, 40f, 40)

        val ground = SantaGame.GROUND_HEIGHT
        shapes.color = Palette.SNOW_SHADE
        shapes.ellipse(-width * 0.1f, ground - 60f, width * 0.7f, 150f, 48)
        shapes.ellipse(width * 0.45f, ground - 70f, width * 0.7f, 170f, 48)

        for (i in treeSlots.indices) {
            drawTree(shapes, width * treeSlots[i], ground + 10f, treeScales[i])
        }

        shapes.color = Palette.SNOW
        shapes.rect(0f, 0f, width, ground)
    }

    fun drawSnow(shapes: ShapeRenderer, width: Float) {
        shapes.color = Palette.SNOW
        for (flake in flakes) {
            val x = flake.x * width + MathUtils.sin(flake.phase) * flake.sway
            shapes.circle(x, flake.y, flake.radius, 8)
        }
    }

    private fun drawTree(shapes: ShapeRenderer, x: Float, base: Float, scale: Float) {
        val w = 90f * scale
        val h = 70f * scale
        shapes.color = Palette.TRUNK
        shapes.rect(x - 8f * scale, base, 16f * scale, 22f * scale)
        for (layer in 0 until 3) {
            val layerY = base + 18f * scale + layer * h * 0.5f
            val layerW = w * (1f - layer * 0.22f)
            shapes.color = if (layer % 2 == 0) Palette.PINE else Palette.PINE_DARK
            shapes.triangle(x - layerW / 2f, layerY, x + layerW / 2f, layerY, x, layerY + h)
            shapes.color = Palette.SNOW
            shapes.triangle(x - layerW * 0.14f, layerY + h * 0.72f, x + layerW * 0.14f, layerY + h * 0.72f, x, layerY + h)
        }
    }

    companion object {
        private const val FLAKE_COUNT = 140
    }
}
