package com.greencoolerapple.santagame.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.greencoolerapple.santagame.save.Settings

class Controls(private val settings: () -> Settings) {
    fun key(action: Action, slot: Int): Int = settings().bindings[action.ordinal * Action.SLOTS + slot]

    fun isPressed(action: Action): Boolean {
        for (slot in 0 until Action.SLOTS) {
            val code = key(action, slot)
            if (code > 0 && Gdx.input.isKeyPressed(code)) return true
        }
        return false
    }

    fun isJustPressed(action: Action): Boolean {
        for (slot in 0 until Action.SLOTS) {
            val code = key(action, slot)
            if (code > 0 && Gdx.input.isKeyJustPressed(code)) return true
        }
        return false
    }

    fun bind(action: Action, slot: Int, code: Int) {
        val bindings = settings().bindings
        if (code > 0) {
            for (i in bindings.indices) {
                if (bindings[i] == code) bindings[i] = Input.Keys.UNKNOWN
            }
        }
        bindings[action.ordinal * Action.SLOTS + slot] = code
    }

    fun resetDefaults() {
        Action.defaults().copyInto(settings().bindings)
    }

    companion object {
        fun keyName(code: Int): String = if (code <= 0) "-" else Input.Keys.toString(code) ?: "?"
    }
}
