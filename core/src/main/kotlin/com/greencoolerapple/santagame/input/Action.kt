package com.greencoolerapple.santagame.input

import com.badlogic.gdx.Input
import com.greencoolerapple.santagame.i18n.I18n

enum class Action(private val key: String, val defaultPrimary: Int, val defaultAlternative: Int) {
    MOVE_UP("moveUp", Input.Keys.W, Input.Keys.UP),
    MOVE_DOWN("moveDown", Input.Keys.S, Input.Keys.DOWN),
    MOVE_LEFT("moveLeft", Input.Keys.A, Input.Keys.LEFT),
    MOVE_RIGHT("moveRight", Input.Keys.D, Input.Keys.RIGHT),
    PAUSE("pause", Input.Keys.P, Input.Keys.UNKNOWN),
    CONFIRM("confirm", Input.Keys.SPACE, Input.Keys.ENTER),
    SCREENSHOT("screenshot", Input.Keys.F3, Input.Keys.UNKNOWN);

    val title: String
        get() = I18n.t("controls.$key")

    companion object {
        const val SLOTS = 2

        fun defaults(): IntArray = IntArray(entries.size * SLOTS) { i ->
            val action = entries[i / SLOTS]
            if (i % SLOTS == 0) action.defaultPrimary else action.defaultAlternative
        }
    }
}
