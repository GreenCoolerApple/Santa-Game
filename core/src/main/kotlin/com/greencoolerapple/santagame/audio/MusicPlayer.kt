package com.greencoolerapple.santagame.audio

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.greencoolerapple.santagame.save.Settings

class MusicPlayer(private val settings: () -> Settings) {
    enum class Track(private val fileName: String, val looping: Boolean) {
        INTRO("intro", false),
        MENU("menu", true),
        CATCH_GIFTS("catch", true),
        STOLEN_GIFTS("stolen", true),
        ESCAPE_CITY("escape", true),
        SLED_RACE("sled", true),
        SHOP("shop", true);

        val path: String
            get() = "music/$fileName." + if (Gdx.app.type == Application.ApplicationType.iOS) "m4a" else "ogg"
    }

    private var current: Track? = null
    private var music: Music? = null
    private var next: Track? = null
    private var fade = 0f
    private var target = 0f
    private var fadeSpeed = 1f
    private var duckLevel = 1f
    private var duckTarget = 1f

    fun duck(level: Float) {
        duckTarget = level.coerceIn(0f, 1f)
    }

    fun play(track: Track, fadeIn: Float = DEFAULT_FADE) {
        if (track == current && music != null) {
            next = null
            target = 1f
            fadeSpeed = 1f / maxOf(fadeIn, 0.01f)
            return
        }
        if (music == null) {
            start(track, fadeIn)
        } else {
            next = track
            target = 0f
            fadeSpeed = 1f / SWITCH_FADE
        }
    }

    fun stop(fadeOut: Float = DEFAULT_FADE) {
        next = null
        target = 0f
        fadeSpeed = 1f / maxOf(fadeOut, 0.01f)
    }

    fun update(delta: Float) {
        val queued = next
        if (music == null && queued != null) {
            next = null
            start(queued, DEFAULT_FADE)
        }
        val playing = music ?: return
        fade = if (fade < target) minOf(target, fade + fadeSpeed * delta) else maxOf(target, fade - fadeSpeed * delta)
        if (fade <= 0f && target <= 0f) {
            release()
            next?.let {
                next = null
                start(it, DEFAULT_FADE)
            }
            return
        }
        duckLevel = if (duckLevel < duckTarget) minOf(duckTarget, duckLevel + delta * DUCK_SPEED) else maxOf(duckTarget, duckLevel - delta * DUCK_SPEED)
        playing.volume = fade * duckLevel * settings().musicVolume / 100f
        if (!playing.isLooping && !playing.isPlaying && fade >= target) release()
    }

    private fun start(track: Track, fadeIn: Float) {
        val loaded = runCatching { Gdx.audio.newMusic(Gdx.files.internal(track.path)) }.getOrNull() ?: return
        current = track
        music = loaded
        fade = if (fadeIn <= 0f) 1f else 0f
        target = 1f
        fadeSpeed = 1f / maxOf(fadeIn, 0.01f)
        loaded.isLooping = track.looping
        duckLevel = duckTarget
        loaded.volume = fade * duckLevel * settings().musicVolume / 100f
        loaded.play()
    }

    private fun release() {
        music?.let {
            it.stop()
            it.dispose()
        }
        music = null
        current = null
    }

    fun dispose() {
        next = null
        release()
    }

    companion object {
        private const val DEFAULT_FADE = 0.8f
        private const val SWITCH_FADE = 0.35f
        private const val DUCK_SPEED = 2.5f
    }
}
