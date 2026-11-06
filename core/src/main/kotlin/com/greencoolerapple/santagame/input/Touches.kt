package com.greencoolerapple.santagame.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport

class Touches {
    val down = BooleanArray(MAX_POINTERS)
    val justDown = BooleanArray(MAX_POINTERS)
    val claimed = BooleanArray(MAX_POINTERS)
    val x = FloatArray(MAX_POINTERS)
    val y = FloatArray(MAX_POINTERS)

    private val previous = BooleanArray(MAX_POINTERS)
    private val point = Vector2()

    fun update(viewport: Viewport) {
        for (i in 0 until MAX_POINTERS) {
            val touched = Gdx.input.isTouched(i)
            justDown[i] = touched && !previous[i]
            if (!touched) claimed[i] = false
            previous[i] = touched
            down[i] = touched
            if (touched) {
                point.set(Gdx.input.getX(i).toFloat(), Gdx.input.getY(i).toFloat())
                viewport.unproject(point)
                x[i] = point.x
                y[i] = point.y
            }
        }
    }

    fun firstJustDown(): Int {
        for (i in 0 until MAX_POINTERS) {
            if (justDown[i] && !claimed[i]) return i
        }
        return -1
    }

    fun claim(pointer: Int) {
        claimed[pointer] = true
    }

    fun isFree(pointer: Int): Boolean = down[pointer] && !claimed[pointer]

    companion object {
        const val MAX_POINTERS = 5
    }
}
