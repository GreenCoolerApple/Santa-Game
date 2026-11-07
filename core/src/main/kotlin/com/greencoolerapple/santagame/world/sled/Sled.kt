package com.greencoolerapple.santagame.world.sled

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.shop.Cosmetics

class Sled(val rider: Rider) {
    enum class Rider { SANTA, REINDEER, ELF, SNOWMAN }

    var x = 0f
    var y = 0f
    var speed = 0f
    var steer = 0f
    var spin = 0f
    var boost = 0f
    var stun = 0f
    var crashes = 0
    var gifts = 0
    var finished = false
    var finishTime = 0f
    var place = 0

    private var cos = 1f
    private var sin = 0f
    private var ox = 0f
    private var oy = 0f
    private val tint = Color()

    val angle: Float
        get() = -steer * 18f + spin

    fun reset(startX: Float, startY: Float) {
        x = startX
        y = startY
        speed = 0f
        steer = 0f
        spin = 0f
        boost = 0f
        stun = 0f
        crashes = 0
        gifts = 0
        finished = false
        finishTime = 0f
        place = 0
    }

    fun draw(shapes: ShapeRenderer, time: Float) {
        ox = x
        oy = y
        cos = MathUtils.cosDeg(angle)
        sin = MathUtils.sinDeg(angle)

        shapes.color = Palette.SHADOW
        shapes.ellipse(x - 24f + 6f, y - 40f - 6f, 48f, 84f, 20)

        if (boost > 0f) {
            for (k in 0 until 4) {
                val flicker = MathUtils.sin(time * 40f + k * 1.7f) * 4f
                tint.set(Palette.BOOST_GLOW)
                tint.a = 0.5f - k * 0.1f
                shapes.color = tint
                circle(shapes, 0f, -46f - k * 14f, 14f - k * 2.5f + flicker * 0.3f)
            }
        }

        shapes.color = Palette.SLED_RUNNER
        rect(shapes, -20f, -38f, 5f, 80f)
        rect(shapes, 15f, -38f, 5f, 80f)
        circle(shapes, -17.5f, 42f, 6f)
        circle(shapes, 17.5f, 42f, 6f)

        val player = rider == Rider.SANTA
        shapes.color = if (player) Cosmetics.sledShade else Palette.SLED_WOOD_DARK
        rect(shapes, -18f, -32f, 36f, 66f)
        for (k in 0 until 4) {
            shapes.color = when {
                !player -> Palette.SLED_WOOD
                Cosmetics.sledStriped && k % 2 == 1 -> Palette.SNOW
                else -> Cosmetics.sled
            }
            rect(shapes, -16f, -28f + k * 15f, 32f, 11f)
        }

        when (rider) {
            Rider.SANTA -> drawSanta(shapes)
            Rider.REINDEER -> drawReindeer(shapes)
            Rider.ELF -> drawElf(shapes)
            Rider.SNOWMAN -> drawSnowman(shapes)
        }
    }

    private fun drawSanta(shapes: ShapeRenderer) {
        shapes.color = Palette.SACK
        circle(shapes, 0f, -22f, 13f)
        shapes.color = Cosmetics.suitShade
        circle(shapes, 0f, -2f, 17f)
        shapes.color = Cosmetics.suit
        circle(shapes, 0f, 0f, 15f)
        shapes.color = Palette.BLACK
        rect(shapes, -15f, -3f, 30f, 5f)
        shapes.color = Palette.SKIN
        circle(shapes, -15f, 12f, 5f)
        circle(shapes, 15f, 12f, 5f)
        circle(shapes, 0f, 14f, 10f)
        shapes.color = Palette.SNOW
        circle(shapes, 0f, 20f, 9f)
        shapes.color = Cosmetics.suit
        circle(shapes, 0f, 10f, 8f)
        shapes.color = Palette.SNOW
        circle(shapes, 0f, 3f, 4f)
    }

    private fun drawReindeer(shapes: ShapeRenderer) {
        shapes.color = Palette.REINDEER_DARK
        circle(shapes, 0f, -4f, 17f)
        shapes.color = Palette.REINDEER
        circle(shapes, 0f, -2f, 15f)
        shapes.color = Palette.ANTLER
        line(shapes, -6f, 14f, -18f, 30f, 4f)
        line(shapes, -12f, 22f, -22f, 20f, 3f)
        line(shapes, 6f, 14f, 18f, 30f, 4f)
        line(shapes, 12f, 22f, 22f, 20f, 3f)
        shapes.color = Palette.REINDEER
        circle(shapes, 0f, 14f, 10f)
        shapes.color = Palette.REINDEER_DARK
        circle(shapes, -10f, 16f, 4f)
        circle(shapes, 10f, 16f, 4f)
        shapes.color = Palette.HEART
        circle(shapes, 0f, 24f, 4.5f)
    }

    private fun drawElf(shapes: ShapeRenderer) {
        shapes.color = Palette.ELF_GREEN_DARK
        circle(shapes, 0f, -2f, 16f)
        shapes.color = Palette.ELF_GREEN
        circle(shapes, 0f, 0f, 14f)
        shapes.color = Palette.SANTA_RED
        rect(shapes, -14f, -3f, 28f, 5f)
        shapes.color = Palette.SKIN
        circle(shapes, -13f, 12f, 4.5f)
        circle(shapes, 13f, 12f, 4.5f)
        circle(shapes, 0f, 13f, 9f)
        triangle(shapes, -9f, 13f, -16f, 18f, -8f, 17f)
        triangle(shapes, 9f, 13f, 16f, 18f, 8f, 17f)
        shapes.color = Palette.ELF_GREEN
        triangle(shapes, -8f, 11f, 8f, 11f, 0f, -6f)
        shapes.color = Palette.GOLD
        circle(shapes, 0f, -7f, 3.5f)
    }

    private fun drawSnowman(shapes: ShapeRenderer) {
        shapes.color = Palette.SNOW_SHADE
        circle(shapes, 0f, -6f, 17f)
        shapes.color = Palette.SNOW
        circle(shapes, -1f, -4f, 15f)
        shapes.color = Palette.SCARF_BLUE
        rect(shapes, -13f, 4f, 26f, 6f)
        rect(shapes, 6f, -6f, 6f, 12f)
        shapes.color = Palette.SNOW_SHADE
        circle(shapes, 0f, 16f, 11f)
        shapes.color = Palette.SNOW
        circle(shapes, -1f, 17f, 9.5f)
        shapes.color = Palette.BLACK
        circle(shapes, 0f, 17f, 7f)
        shapes.color = Palette.COAL_SHINE
        circle(shapes, 0f, 17f, 4.5f)
        shapes.color = Palette.CARROT
        triangle(shapes, -2.5f, 24f, 2.5f, 24f, 0f, 34f)
    }

    private fun px(dx: Float, dy: Float): Float = ox + dx * cos - dy * sin

    private fun py(dx: Float, dy: Float): Float = oy + dx * sin + dy * cos

    private fun circle(shapes: ShapeRenderer, dx: Float, dy: Float, r: Float) {
        shapes.circle(px(dx, dy), py(dx, dy), r, 14)
    }

    private fun rect(shapes: ShapeRenderer, dx: Float, dy: Float, w: Float, h: Float) {
        val x1 = px(dx, dy)
        val y1 = py(dx, dy)
        val x2 = px(dx + w, dy)
        val y2 = py(dx + w, dy)
        val x3 = px(dx + w, dy + h)
        val y3 = py(dx + w, dy + h)
        val x4 = px(dx, dy + h)
        val y4 = py(dx, dy + h)
        shapes.triangle(x1, y1, x2, y2, x3, y3)
        shapes.triangle(x1, y1, x3, y3, x4, y4)
    }

    private fun triangle(shapes: ShapeRenderer, ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float) {
        shapes.triangle(px(ax, ay), py(ax, ay), px(bx, by), py(bx, by), px(cx, cy), py(cx, cy))
    }

    private fun line(shapes: ShapeRenderer, ax: Float, ay: Float, bx: Float, by: Float, width: Float) {
        shapes.rectLine(px(ax, ay), py(ax, ay), px(bx, by), py(bx, by), width)
    }

    companion object {
        const val RADIUS = 20f
    }
}
