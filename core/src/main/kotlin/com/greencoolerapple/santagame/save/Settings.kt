package com.greencoolerapple.santagame.save

import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.i18n.Language
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class Settings {
    var fps = DEFAULT_FPS
    var language = Language.system()

    val fpsLabel: String
        get() = if (fps <= 0) I18n.t("options.max") else fps.toString()

    fun cycleFps(step: Int) {
        val index = FPS_OPTIONS.indexOf(fps).let { if (it < 0) FPS_OPTIONS.indexOf(DEFAULT_FPS) else it }
        fps = FPS_OPTIONS[(index + step).mod(FPS_OPTIONS.size)]
    }

    fun cycleLanguage(step: Int) {
        val values = Language.entries
        language = values[(language.ordinal + step).mod(values.size)]
    }

    fun encode(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(fps)
            out.writeInt(language.ordinal)
        }
        return bytes.toByteArray()
    }

    companion object {
        const val DEFAULT_FPS = 60
        val FPS_OPTIONS = intArrayOf(30, 60, 90, 120, 0)

        fun decode(input: DataInputStream): Settings = Settings().apply {
            val value = input.readInt()
            fps = if (value in FPS_OPTIONS) value else DEFAULT_FPS
            if (input.available() >= 4) {
                val ordinal = input.readInt()
                language = Language.entries.getOrElse(ordinal) { Language.system() }
            }
        }
    }
}
