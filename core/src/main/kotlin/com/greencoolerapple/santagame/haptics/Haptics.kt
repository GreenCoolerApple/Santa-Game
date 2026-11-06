package com.greencoolerapple.santagame.haptics

import com.badlogic.gdx.Gdx
import com.greencoolerapple.santagame.save.Settings

class Haptics(private val settings: () -> Settings) {
    enum class Pulse(val milliseconds: Int, val amplitude: Int) {
        TICK(10, 70),
        LIGHT(22, 115),
        MEDIUM(40, 175),
        HEAVY(75, 255)
    }

    class Step(val delay: Float, val pulse: Pulse)

    enum class Pattern(vararg val steps: Step) {
        TAP(Step(0f, Pulse.TICK)),
        COLLECT(Step(0f, Pulse.LIGHT)),
        HIT(Step(0f, Pulse.MEDIUM), Step(0.09f, Pulse.LIGHT)),
        IMPACT(Step(0f, Pulse.HEAVY)),
        CAUGHT(Step(0f, Pulse.HEAVY), Step(0.14f, Pulse.MEDIUM), Step(0.3f, Pulse.LIGHT)),
        SUCCESS(Step(0f, Pulse.LIGHT), Step(0.1f, Pulse.LIGHT), Step(0.22f, Pulse.MEDIUM)),
        ERROR(Step(0f, Pulse.MEDIUM), Step(0.12f, Pulse.MEDIUM)),
        AWARD(Step(0f, Pulse.LIGHT), Step(0.12f, Pulse.MEDIUM)),
        PREVIEW(Step(0f, Pulse.MEDIUM))
    }

    private class Scheduled(var delay: Float, val pulse: Pulse)

    private val queue = mutableListOf<Scheduled>()

    fun play(pattern: Pattern) {
        if (!settings().haptics) return
        for (step in pattern.steps) {
            if (step.delay <= 0f) fire(step.pulse) else queue += Scheduled(step.delay, step.pulse)
        }
    }

    fun update(delta: Float) {
        if (queue.isEmpty()) return
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            item.delay -= delta
            if (item.delay <= 0f) {
                iterator.remove()
                fire(item.pulse)
            }
        }
    }

    private fun fire(pulse: Pulse) {
        val current = settings()
        if (!current.haptics) return
        val factor = current.hapticIntensity / 100f
        val amplitude = (pulse.amplitude * factor).toInt().coerceIn(1, 255)
        val duration = (pulse.milliseconds * (0.6f + 0.4f * factor)).toInt().coerceAtLeast(5)
        runCatching { Gdx.input.vibrate(duration, amplitude, true) }
    }
}
