package com.greencoolerapple.santagame

class FrameLimiter {
    var fps = 60
        set(value) {
            field = value
            nextFrame = 0L
        }

    private var nextFrame = 0L

    fun sync() {
        if (fps <= 0) return
        val frame = 1_000_000_000L / fps
        var now = System.nanoTime()
        if (nextFrame == 0L) nextFrame = now
        val wait = nextFrame - now
        if (wait > 0L) {
            try {
                Thread.sleep(wait / 1_000_000L, (wait % 1_000_000L).toInt())
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            now = System.nanoTime()
        }
        nextFrame += frame
        if (nextFrame < now - frame) nextFrame = now + frame
    }
}
