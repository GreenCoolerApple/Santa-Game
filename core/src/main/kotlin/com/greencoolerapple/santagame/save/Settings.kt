package com.greencoolerapple.santagame.save

import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.i18n.Language
import com.greencoolerapple.santagame.input.Action
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class Settings {
    var fps = DEFAULT_FPS
    var language = Language.system()
    var haptics = true
    var hapticIntensity = DEFAULT_INTENSITY
    var musicVolume = DEFAULT_MUSIC
    val bindings: IntArray = Action.defaults()

    val fpsLabel: String
        get() = if (fps <= 0) I18n.t("options.max") else fps.toString()

    val hapticsLabel: String
        get() = I18n.t(if (haptics) "options.on" else "options.off")

    val intensityLabel: String
        get() = "$hapticIntensity%"

    val musicLabel: String
        get() = if (musicVolume == 0) I18n.t("options.off") else "$musicVolume%"

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

    fun cycleMusic(step: Int) {
        val index = MUSIC_OPTIONS.indexOf(musicVolume).let { if (it < 0) MUSIC_OPTIONS.indexOf(DEFAULT_MUSIC) else it }
        musicVolume = MUSIC_OPTIONS[(index + step).mod(MUSIC_OPTIONS.size)]
    }

    fun encode(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(fps)
            out.writeInt(language.ordinal)
            out.writeBoolean(haptics)
            out.writeInt(hapticIntensity)
            out.writeInt(musicVolume)
            out.writeInt(bindings.size)
            for (code in bindings) out.writeInt(code)
        }
        return bytes.toByteArray()
    }

    companion object {
        const val DEFAULT_FPS = 60
        const val DEFAULT_INTENSITY = 75
        const val DEFAULT_MUSIC = 75
        val MUSIC_OPTIONS = intArrayOf(0, 25, 50, 75, 100)
        private const val MAX_KEY_CODE = 255
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
            if (input.available() >= 4) {
                val volume = input.readInt()
                musicVolume = if (volume in MUSIC_OPTIONS) volume else DEFAULT_MUSIC
            }
            if (input.available() >= 4) {
                val count = input.readInt()
                for (i in 0 until count) {
                    if (input.available() < 4) break
                    val code = input.readInt()
                    if (i < bindings.size && code in 0..MAX_KEY_CODE) bindings[i] = code
                }
            }
        }
    }
}
