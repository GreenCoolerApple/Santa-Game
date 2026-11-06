package com.greencoolerapple.santagame.world.city

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.shop.Cosmetics

class Boy {
    val position = Vector2()

    private var facing = 90f
    private var walkTime = 0f

    fun reset(start: Vector2) {
        position.set(start)
        facing = 45f
        walkTime = 0f
    }

    fun move(direction: Vector2, delta: Float, speed: Float, map: CityMap) {
        val length = direction.len()
        if (length < 0.05f) return
        val amount = minOf(1f, length)
        map.move(position, direction.x / length * amount * speed * delta, direction.y / length * amount * speed * delta, RADIUS)
        val goal = MathUtils.atan2(direction.y, direction.x) * MathUtils.radiansToDegrees
        var diff = (goal - facing) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        facing += MathUtils.clamp(diff, -900f * delta, 900f * delta)
        walkTime += delta * amount
    }

    fun draw(shapes: ShapeRenderer) {
        val x = position.x
        val y = position.y
        val rad = facing * MathUtils.degreesToRadians
        val fx = MathUtils.cos(rad)
        val fy = MathUtils.sin(rad)
        val swing = MathUtils.sin(walkTime * 14f) * 5f

        shapes.color = Palette.SHADOW
        shapes.circle(x + 3f, y - 4f, RADIUS + 2f, 18)

        shapes.color = Palette.SKIN
        shapes.circle(x + fx * swing - fy * 14f, y + fy * swing + fx * 14f, 5f, 10)
        shapes.circle(x - fx * swing + fy * 14f, y - fy * swing - fx * 14f, 5f, 10)

        shapes.color = Cosmetics.jacket
        shapes.circle(x, y, RADIUS, 20)
        shapes.color = Palette.SANTA_RED
        shapes.rectLine(x - fy * 10f + fx * 4f, y + fx * 10f + fy * 4f, x + fy * 10f + fx * 4f, y - fx * 10f + fy * 4f, 5f)
        shapes.rectLine(x - fx * 2f + fy * 6f, y - fy * 2f - fx * 6f, x - fx * 14f + fy * 9f, y - fy * 14f - fx * 9f, 4f)

        shapes.color = Palette.SNOW
        shapes.circle(x, y, 9.5f, 16)
        shapes.color = Palette.SANTA_RED
        shapes.circle(x - fx * 1f, y - fy * 1f, 7.5f, 14)
        shapes.color = Palette.SNOW
        shapes.circle(x - fx * 7f, y - fy * 7f, 3.8f, 10)
    }

    companion object {
        const val RADIUS = 15f
    }
}
