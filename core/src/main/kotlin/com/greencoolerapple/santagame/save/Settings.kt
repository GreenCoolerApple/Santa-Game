package com.greencoolerapple.santagame.save

import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.i18n.Language
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class Settings {
    var fps = DEFAULT_FPS
    var language = Language.system()
    var haptics = true
    var hapticIntensity = DEFAULT_INTENSITY

    val fpsLabel: String
        get() = if (fps <= 0) I18n.t("options.max") else fps.toString()

    val hapticsLabel: String
        get() = I18n.t(if (haptics) "options.on" else "options.off")

    val intensityLabel: String
        get() = "$hapticIntensity%"

    fun cycleFps(step: Int) {
        val index = FPS_OPTIONS.indexOf(fps).let { if (it < 0) FPS_OPTIONS.indexOf(DEFAULT_FPS) else it }
        fps = FPS_OPTIONS[(index + step).mod(FPS_OPTIONS.size)]
    }

    fun cycleLanguage(step: Int) {
        val values = Language.entries
        language = values[(language.ordinal + step).mod(values.size)]
    }

    fun cycleIntensity(step: Int) {
        val index = INTENSITY_OPTIONS.indexOf(hapticIntensity).let { if (it < 0) INTENSITY_OPTIONS.indexOf(DEFAULT_INTENSITY) else it }
        hapticIntensity = INTENSITY_OPTIONS[(index + step).mod(INTENSITY_OPTIONS.size)]
    }

    fun encode(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(fps)
            out.writeInt(language.ordinal)
            out.writeBoolean(haptics)
            out.writeInt(hapticIntensity)
        }
        return bytes.toByteArray()
    }

    companion object {
        const val DEFAULT_FPS = 60
        const val DEFAULT_INTENSITY = 75
        val FPS_OPTIONS = intArrayOf(30, 60, 90, 120, 0)
        val INTENSITY_OPTIONS = intArrayOf(25, 50, 75, 100)

        fun decode(input: DataInputStream): Settings = Settings().apply {
            val value = input.readInt()
            fps = if (value in FPS_OPTIONS) value else DEFAULT_FPS
            if (input.available() >= 4) {
                val ordinal = input.readInt()
                language = Language.entries.getOrElse(ordinal) { Language.system() }
            }
            if (input.available() >= 5) {
                haptics = input.readBoolean()
                val intensity = input.readInt()
                hapticIntensity = if (intensity in INTENSITY_OPTIONS) intensity else DEFAULT_INTENSITY
            }
        }
    }
}
