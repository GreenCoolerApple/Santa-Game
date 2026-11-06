package com.greencoolerapple.santagame.world.house

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.greencoolerapple.santagame.Palette

class SneakySanta {
    val position = Vector2()

    private var facing = 0f
    private var walkTime = 0f
    private val probe = Circle(0f, 0f, RADIUS)

    fun reset(spawn: Vector2) {
        position.set(spawn)
        facing = 0f
        walkTime = 0f
    }

    fun move(direction: Vector2, delta: Float, speed: Float, blockers: List<Rectangle>, area: Rectangle) {
        val length = direction.len()
        if (length < 0.05f) return
        val amount = minOf(1f, length)
        val dx = direction.x / length * amount * speed * delta
        val dy = direction.y / length * amount * speed * delta

        if (canStand(position.x + dx, position.y, blockers, area)) position.x += dx
        if (canStand(position.x, position.y + dy, blockers, area)) position.y += dy

        val goal = MathUtils.atan2(direction.y, direction.x) * MathUtils.radiansToDegrees
        var diff = (goal - facing) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        facing += MathUtils.clamp(diff, -720f * delta, 720f * delta)
        walkTime += delta * amount
    }

    private fun canStand(x: Float, y: Float, blockers: List<Rectangle>, area: Rectangle): Boolean {
        if (x - RADIUS < area.x || x + RADIUS > area.x + area.width) return false
        if (y - RADIUS < area.y || y + RADIUS > area.y + area.height) return false
        probe.setPosition(x, y)
        for (rect in blockers) {
            if (Intersector.overlaps(probe, rect)) return false
        }
        return true
    }

    fun draw(shapes: ShapeRenderer, carried: Int) {
        val x = position.x
        val y = position.y
        val rad = facing * MathUtils.degreesToRadians
        val fx = MathUtils.cos(rad)
        val fy = MathUtils.sin(rad)
        val swing = MathUtils.sin(walkTime * 12f) * 5f

        shapes.color = Palette.SHADOW
        shapes.circle(x + 3f, y - 4f, RADIUS + 3f, 20)

        if (carried > 0) {
            val sack = 11f + minOf(carried, 8) * 1.6f
            shapes.color = Palette.SACK
            shapes.circle(x - fx * 20f, y - fy * 20f, sack, 18)
            shapes.color = Palette.GOLD
            shapes.circle(x - fx * (20f - sack * 0.6f), y - fy * (20f - sack * 0.6f), 3f, 8)
        }

        shapes.color = Palette.BLACK
        shapes.circle(x + fx * 8f - fy * (16f + swing), y + fy * 8f + fx * (16f + swing), 6.5f, 10)
        shapes.circle(x + fx * 8f + fy * (16f - swing), y + fy * 8f - fx * (16f - swing), 6.5f, 10)

        shapes.color = Palette.SNOW
        shapes.circle(x, y, RADIUS, 24)
        shapes.color = Palette.SANTA_RED
        shapes.circle(x, y, RADIUS - 4f, 24)

        shapes.color = Palette.SNOW
        shapes.circle(x + fx * 7f, y + fy * 7f, 9f, 16)
        shapes.circle(x, y, 12f, 18)
        shapes.color = Palette.SANTA_RED
        shapes.circle(x - fx * 1f, y - fy * 1f, 8.5f, 16)
        shapes.color = Palette.SNOW
        shapes.circle(x - fx * 8f, y - fy * 8f, 4.5f, 10)
    }

    companion object {
        const val RADIUS = 20f
    }
}
