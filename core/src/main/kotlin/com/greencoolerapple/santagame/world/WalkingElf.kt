package com.greencoolerapple.santagame.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.Palette

class WalkingElf(private val hair: Color, private val band: Color, phase: Float) {
    enum class Action { IDLE, WALK, HAMMER, SAW, CARRY }

    var x = 0f
    var y = 0f
    var dir = 1f
    var scale = 1f
    var action = Action.IDLE
    var talking = false
    var bounce = 0f
    var targetX = 0f
    var wait = 0f

    var time = phase
        private set
    private var stride = phase
    private var lastCycle = 0

    val headY: Float
        get() = y + (110f + lift()) * scale

    val topY: Float
        get() = y + 172f * scale

    fun update(delta: Float) {
        time += delta
        bounce = maxOf(0f, bounce - delta * 3f)
        if (action == Action.WALK || action == Action.CARRY) stride += delta * 10f
    }

    fun walkTowards(target: Float, speed: Float, delta: Float): Boolean {
        val dx = target - x
        if (kotlin.math.abs(dx) < 3f) {
            x = target
            return true
        }
        dir = if (dx > 0f) 1f else -1f
        x += dir * minOf(kotlin.math.abs(dx), speed * delta)
        return false
    }

    fun hammerHit(): Boolean {
        if (action != Action.HAMMER) return false
        val cycle = (time * HAMMER_SPEED * 0.5f / MathUtils.PI).toInt()
        val hit = cycle != lastCycle
        lastCycle = cycle
        return hit
    }

    fun hammerHead(): Float = x + dir * 52f * scale

    fun contains(px: Float, py: Float): Boolean =
        kotlin.math.abs(px - x) < 40f * scale && py > y && py < topY

    private fun lift(): Float {
        val walk = if (action == Action.WALK || action == Action.CARRY) kotlin.math.abs(MathUtils.sin(stride)) * 4f else 0f
        return walk + MathUtils.sin(bounce * 10f) * bounce * 18f
    }

    private fun px(dx: Float): Float = x + dx * dir * scale

    private fun py(dy: Float): Float = y + (dy + lift()) * scale

    private fun circle(shapes: ShapeRenderer, dx: Float, dy: Float, r: Float, segments: Int = 12) {
        shapes.circle(px(dx), py(dy), r * scale, segments)
    }

    private fun line(shapes: ShapeRenderer, x1: Float, y1: Float, x2: Float, y2: Float, width: Float) {
        shapes.rectLine(px(x1), py(y1), px(x2), py(y2), width * scale)
    }

    private fun tri(shapes: ShapeRenderer, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        shapes.triangle(px(x1), py(y1), px(x2), py(y2), px(x3), py(y3))
    }

    private fun box(shapes: ShapeRenderer, x1: Float, y1: Float, x2: Float, y2: Float) {
        tri(shapes, x1, y1, x2, y1, x2, y2)
        tri(shapes, x1, y1, x2, y2, x1, y2)
    }

    fun draw(shapes: ShapeRenderer) {
        val s = scale
        shapes.color = Palette.SHADOW
        shapes.ellipse(x - 26f * s, y - 6f * s, 52f * s, 12f * s, 14)

        val moving = action == Action.WALK || action == Action.CARRY
        val swing = if (moving) MathUtils.sin(stride) * 12f else 0f
        drawLeg(shapes, -7f, -swing)
        drawLeg(shapes, 7f, swing)

        val back = backArm(swing)
        drawArm(shapes, -4f, back.first, back.second)

        shapes.color = Palette.ELF_GREEN_DARK
        tri(shapes, -26f, 30f, 26f, 30f, 0f, 92f)
        shapes.color = Palette.ELF_GREEN
        tri(shapes, -22f, 32f, 22f, 32f, 0f, 90f)
        shapes.ellipse(px(-21f).coerceAtMost(px(21f)), py(36f), 42f * s, 50f * s, 18)
        shapes.color = Palette.SANTA_RED
        for (k in 0 until 5) tri(shapes, -24f + k * 10f, 30f, -14f + k * 10f, 30f, -19f + k * 10f, 22f)
        shapes.color = Palette.BLACK
        box(shapes, -21f, 46f, 21f, 53f)
        shapes.color = Palette.GOLD
        box(shapes, -4f, 45f, 5f, 54f)

        drawHead(shapes)

        val front = frontArm(swing)
        drawTool(shapes, front.first, front.second)
        drawArm(shapes, 6f, front.first, front.second)
        if (action == Action.CARRY) {
            shapes.color = Palette.TABLE_TOP
            box(shapes, -46f, 136f, 54f, 146f)
            shapes.color = Palette.TABLE_FRONT
            box(shapes, -46f, 136f, 54f, 139f)
        }
    }

    private fun drawLeg(shapes: ShapeRenderer, offset: Float, swing: Float) {
        val footX = offset + swing * 0.5f
        for (k in 0 until 4) {
            val t0 = k / 4f
            val t1 = (k + 1) / 4f
            shapes.color = if (k % 2 == 0) Palette.SANTA_RED else Palette.SNOW
            line(shapes, MathUtils.lerp(offset, footX, t0), MathUtils.lerp(32f, 4f, t0), MathUtils.lerp(offset, footX, t1), MathUtils.lerp(32f, 4f, t1), 8f)
        }
        shapes.color = Palette.ELF_GREEN_DARK
        shapes.ellipse(px(footX - 6f).coerceAtMost(px(footX + 12f)), py(-2f), 18f * scale, 9f * scale, 10)
        tri(shapes, footX + 10f, 1f, footX + 20f, 10f, footX + 8f, 7f)
        shapes.color = Palette.GOLD
        circle(shapes, footX + 20f, 10f, 2.6f, 6)
    }

    private fun backArm(swing: Float): Pair<Float, Float> = when (action) {
        Action.CARRY -> -24f to 132f
        Action.SAW -> -10f to 52f
        else -> (-6f - swing * 0.8f) to 46f
    }

    private fun frontArm(swing: Float): Pair<Float, Float> = when (action) {
        Action.HAMMER -> {
            val a = -0.4f + kotlin.math.abs(MathUtils.sin(time * HAMMER_SPEED * 0.5f)) * 1.9f
            (14f + MathUtils.cos(a) * 30f) to (76f + MathUtils.sin(a) * 30f)
        }
        Action.SAW -> (30f + MathUtils.sin(time * 11f) * 12f) to 56f
        Action.CARRY -> 28f to 134f
        else -> if (talking) {
            (30f + MathUtils.sin(time * 8f) * 4f) to (78f + MathUtils.sin(time * 9f) * 10f)
        } else {
            (8f + swing * 0.8f) to 46f
        }
    }

    private fun drawArm(shapes: ShapeRenderer, shoulder: Float, hx: Float, hy: Float) {
        shapes.color = Palette.ELF_GREEN_DARK
        line(shapes, shoulder, 80f, hx, hy, 10f)
        shapes.color = Palette.SKIN
        circle(shapes, hx, hy, 6f, 10)
    }

    private fun drawTool(shapes: ShapeRenderer, hx: Float, hy: Float) {
        when (action) {
            Action.HAMMER -> {
                val a = -0.4f + kotlin.math.abs(MathUtils.sin(time * HAMMER_SPEED * 0.5f)) * 1.9f
                val ex = hx + MathUtils.cos(a + 0.2f) * 20f
                val ey = hy + MathUtils.sin(a + 0.2f) * 20f
                shapes.color = Palette.TRUNK
                line(shapes, hx, hy, ex, ey, 4f)
                shapes.color = Palette.ROCK_DARK
                val nx = -MathUtils.sin(a) * 9f
                val ny = MathUtils.cos(a) * 9f
                line(shapes, ex - nx, ey - ny, ex + nx, ey + ny, 8f)
            }
            Action.SAW -> {
                shapes.color = Palette.SLED_RUNNER
                tri(shapes, hx, hy - 4f, hx + 44f, hy - 14f, hx + 44f, hy - 4f)
                tri(shapes, hx, hy - 4f, hx + 44f, hy - 4f, hx, hy + 4f)
                shapes.color = Palette.SANTA_RED
                box(shapes, hx - 8f, hy - 5f, hx + 2f, hy + 7f)
            }
            else -> Unit
        }
    }

    private fun drawHead(shapes: ShapeRenderer) {
        shapes.color = Palette.SKIN
        tri(shapes, -16f, 112f, -16f, 100f, -38f, 124f)
        shapes.color = Palette.BLUSH
        tri(shapes, -17f, 110f, -17f, 104f, -32f, 119f)
        shapes.color = Palette.SKIN
        circle(shapes, 0f, 108f, 24f, 22)
        circle(shapes, 21f, 104f, 6f, 10)
        shapes.color = hair
        for (k in 0 until 4) circle(shapes, -16f + k * 9f, 124f, 7f, 8)
        shapes.color = Palette.BLUSH
        circle(shapes, 12f, 100f, 5f, 10)
        val blink = (time % 3.1f) < 0.12f
        shapes.color = Palette.BLACK
        if (blink) {
            box(shapes, 5f, 110f, 13f, 112f)
        } else {
            circle(shapes, 9f, 111f, 3.4f, 8)
            shapes.color = Palette.SNOW
            circle(shapes, 10f, 112.5f, 1.1f, 6)
        }
        val open = if (talking) kotlin.math.abs(MathUtils.sin(time * 15f)) else 0f
        shapes.color = Palette.MOUTH
        shapes.ellipse(px(4f).coerceAtMost(px(16f)), py(92f - open * 3f), 12f * scale, (3f + open * 6f) * scale, 10)
        if (open < 0.2f) {
            shapes.color = Palette.SKIN
            shapes.ellipse(px(3f).coerceAtMost(px(17f)), py(94f), 14f * scale, 3f * scale, 10)
        }

        val sway = MathUtils.sin(time * 2f) * 4f
        shapes.color = Palette.ELF_GREEN_DARK
        tri(shapes, -24f, 124f, 24f, 124f, -26f + sway, 168f)
        shapes.color = Palette.ELF_GREEN
        tri(shapes, -20f, 127f, 20f, 127f, -24f + sway, 164f)
        shapes.color = band
        box(shapes, -26f, 120f, 26f, 130f)
        shapes.color = Palette.GOLD
        circle(shapes, -28f + sway, 168f, 6f, 10)
    }

    companion object {
        const val HAMMER_SPEED = 9f
    }
}
