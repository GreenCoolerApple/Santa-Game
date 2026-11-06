package com.greencoolerapple.santagame.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils

class Sparkles {
    private class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val maxLife: Float, val color: Color)

    private val particles = mutableListOf<Particle>()
    private val tint = Color()

    fun burst(x: Float, y: Float, color: Color, count: Int = 16) {
        repeat(count) {
            val angle = MathUtils.random(0f, MathUtils.PI2)
            val speed = MathUtils.random(120f, 320f)
            val life = MathUtils.random(0.35f, 0.7f)
            particles += Particle(x, y, MathUtils.cos(angle) * speed, MathUtils.sin(angle) * speed, life, life, color)
        }
    }

    fun update(delta: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= delta
            if (p.life <= 0f) {
                iterator.remove()
                continue
            }
            p.vy -= 600f * delta
            p.x += p.vx * delta
            p.y += p.vy * delta
        }
    }

    fun draw(shapes: ShapeRenderer) {
        for (p in particles) {
            val t = p.life / p.maxLife
            tint.set(p.color)
            tint.a = t
            shapes.color = tint
            shapes.circle(p.x, p.y, 3f + 4f * t, 8)
        }
    }

    fun clear() {
        particles.clear()
    }
}
