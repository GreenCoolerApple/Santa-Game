package com.greencoolerapple.santagame.save

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class Settings {
    var fps = DEFAULT_FPS

    val fpsLabel: String
        get() = if (fps <= 0) "Max" else fps.toString()

    fun cycleFps(step: Int) {
        val index = FPS_OPTIONS.indexOf(fps).let { if (it < 0) FPS_OPTIONS.indexOf(DEFAULT_FPS) else it }
        fps = FPS_OPTIONS[(index + step).mod(FPS_OPTIONS.size)]
    }

    fun encode(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(fps)
        }
        return bytes.toByteArray()
    }

    companion object {
        const val DEFAULT_FPS = 60
        val FPS_OPTIONS = intArrayOf(30, 60, 90, 120, 0)

        fun decode(input: DataInputStream): Settings = Settings().apply {
            val value = input.readInt()
            fps = if (value in FPS_OPTIONS) value else DEFAULT_FPS
        }
    }
}
