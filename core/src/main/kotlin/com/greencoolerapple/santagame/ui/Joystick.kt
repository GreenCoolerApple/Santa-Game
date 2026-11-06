package com.greencoolerapple.santagame.ui

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.input.Touches

class Joystick {
    val direction = Vector2()

    private var pointer = -1
    private val base = Vector2()
    private val knob = Vector2()

    val active: Boolean
        get() = pointer >= 0

    fun update(touches: Touches) {
        if (pointer >= 0 && !touches.down[pointer]) release()
        if (pointer < 0) {
            val i = touches.firstJustDown()
            if (i >= 0) {
                pointer = i
                touches.claim(i)
                base.set(touches.x[i], touches.y[i])
            }
        }
        if (pointer >= 0) {
            knob.set(touches.x[pointer], touches.y[pointer]).sub(base)
            if (knob.len() > RADIUS) knob.setLength(RADIUS)
            direction.set(knob).scl(1f / RADIUS)
            if (direction.len() < DEAD_ZONE) direction.setZero()
            knob.add(base)
        }
    }

    fun release() {
        pointer = -1
        direction.setZero()
    }

    fun draw(shapes: ShapeRenderer) {
        if (!active) return
        shapes.color = Palette.JOYSTICK_BASE
        shapes.circle(base.x, base.y, RADIUS + 20f, 32)
        shapes.color = Palette.JOYSTICK_KNOB
        shapes.circle(knob.x, knob.y, 34f, 24)
    }

    companion object {
        const val RADIUS = 70f
        private const val DEAD_ZONE = 0.12f
    }
}
