package com.greencoolerapple.santagame.world.city

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.greencoolerapple.santagame.Palette

class Sick(start: Vector2) {
    enum class State { WANDER, CHASE, SEARCH }

    val position = Vector2(start)
    var state = State.WANDER
        private set
    var facing = MathUtils.random(0f, 360f)
        private set

    private val path = mutableListOf<Vector2>()
    private val lastSeen = Vector2()
    private val goal = Vector2()
    private val previous = Vector2()
    private var repathTimer = 0f
    private var searchTimer = 0f
    private var stuckTimer = 0f
    private var walkTime = MathUtils.random(0f, 10f)
    private var lookTime = 0f

    val chasing: Boolean
        get() = state == State.CHASE

    fun canSee(target: Vector2, map: CityMap): Boolean {
        val distance = position.dst(target)
        if (distance < HEARING) return true
        if (distance > SIGHT) return false
        if (state != State.CHASE) {
            val angle = MathUtils.atan2(target.y - position.y, target.x - position.x) * MathUtils.radiansToDegrees
            if (kotlin.math.abs(wrap(angle - facing)) > HALF_FOV) return false
        }
        return map.lineOfSight(position, target)
    }

    fun update(delta: Float, target: Vector2, map: CityMap, wanderSpeed: Float, chaseSpeed: Float) {
        previous.set(position)
        val sees = canSee(target, map)

        if (sees) {
            state = State.CHASE
            lastSeen.set(target)
            repathTimer -= delta
            val close = position.dst(target) < DIRECT_RANGE && map.lineOfSight(position, target)
            if (close) {
                path.clear()
                steer(target, chaseSpeed, delta, map)
            } else {
                if (repathTimer <= 0f || path.isEmpty()) {
                    map.findPath(position, target, path)
                    repathTimer = REPATH_TIME
                }
                followPath(chaseSpeed, delta, map)
            }
        } else {
            when (state) {
                State.CHASE -> {
                    if (path.isEmpty()) map.findPath(position, lastSeen, path)
                    followPath(chaseSpeed * 0.9f, delta, map)
                    if (path.isEmpty() || position.dst(lastSeen) < 30f) {
                        state = State.SEARCH
                        searchTimer = SEARCH_TIME
                        lookTime = 0f
                    }
                }
                State.SEARCH -> {
                    searchTimer -= delta
                    lookTime += delta
                    facing += MathUtils.sin(lookTime * 2.4f) * 160f * delta + 70f * delta
                    if (searchTimer <= 0f) {
                        state = State.WANDER
                        path.clear()
                    }
                }
                State.WANDER -> {
                    if (path.isEmpty()) {
                        map.randomOpenPointNear(position, WANDER_RADIUS, goal)
                        map.findPath(position, goal, path)
                    }
                    followPath(wanderSpeed, delta, map)
                }
            }
        }

        val moved = position.dst(previous)
        if (state != State.SEARCH && moved < 2f * delta * 10f) {
            stuckTimer += delta
            if (stuckTimer > STUCK_TIME) {
                stuckTimer = 0f
                path.clear()
                if (state == State.WANDER) {
                    map.randomOpenPointNear(position, WANDER_RADIUS, goal)
                    map.findPath(position, goal, path)
                }
            }
        } else {
            stuckTimer = 0f
        }
        walkTime += delta * (moved / maxOf(delta, 0.0001f)) / 60f
    }

    fun pushAway(other: Sick, map: CityMap) {
        val dx = position.x - other.position.x
        val dy = position.y - other.position.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val min = RADIUS * 2f
        if (distance <= 0.01f || distance >= min) return
        val push = (min - distance) / 2f
        map.move(position, dx / distance * push, dy / distance * push, RADIUS)
    }

    private fun followPath(speed: Float, delta: Float, map: CityMap) {
        if (path.isEmpty()) return
        val next = path[0]
        if (position.dst(next) < 8f) {
            path.removeAt(0)
            return
        }
        steer(next, speed, delta, map)
    }

    private fun steer(point: Vector2, speed: Float, delta: Float, map: CityMap) {
        val distance = position.dst(point)
        if (distance < 0.5f) return
        val step = minOf(distance, speed * delta)
        val dx = (point.x - position.x) / distance * step
        val dy = (point.y - position.y) / distance * step
        map.move(position, dx, dy, RADIUS)
        val goalAngle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees
        facing += MathUtils.clamp(wrap(goalAngle - facing), -480f * delta, 480f * delta)
    }

    fun draw(shapes: ShapeRenderer) {
        val x = position.x
        val y = position.y
        val rad = facing * MathUtils.degreesToRadians
        val fx = MathUtils.cos(rad)
        val fy = MathUtils.sin(rad)
        val swing = MathUtils.sin(walkTime * 9f) * 4f
        val lurch = MathUtils.sin(walkTime * 4.5f) * 3f

        shapes.color = Palette.SHADOW
        shapes.circle(x + 4f, y - 5f, RADIUS + 2f, 18)

        shapes.color = Palette.SICK_SKIN
        val reach = 24f + swing
        shapes.circle(x + fx * reach - fy * 11f, y + fy * reach + fx * 11f, 6f, 10)
        shapes.circle(x + fx * (24f - swing) + fy * 11f, y + fy * (24f - swing) - fx * 11f, 6f, 10)
        shapes.color = Palette.SICK_CLOTHES
        shapes.rectLine(x - fy * 11f, y + fx * 11f, x + fx * (reach - 6f) - fy * 11f, y + fy * (reach - 6f) + fx * 11f, 7f)
        shapes.rectLine(x + fy * 11f, y - fx * 11f, x + fx * (18f - swing) + fy * 11f, y + fy * (18f - swing) - fx * 11f, 7f)

        shapes.circle(x - fy * lurch * 0.3f, y + fx * lurch * 0.3f, RADIUS, 20)
        shapes.color = Palette.SICK_PATCH
        shapes.circle(x - fx * 6f + fy * 5f, y - fy * 6f - fx * 5f, 5f, 10)

        shapes.color = Palette.SICK_SKIN
        shapes.circle(x + fx * 2f, y + fy * 2f, 10f, 16)
        shapes.color = Palette.SICK_HAIR
        shapes.circle(x - fx * 3f, y - fy * 3f, 7f, 12)
        shapes.color = if (state == State.CHASE) Palette.METER_DANGER else Palette.SICK_EYES
        shapes.circle(x + fx * 9f - fy * 4f, y + fy * 9f + fx * 4f, 2.2f, 6)
        shapes.circle(x + fx * 9f + fy * 4f, y + fy * 9f - fx * 4f, 2.2f, 6)
    }

    private fun wrap(angle: Float): Float {
        var a = angle % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    companion object {
        const val RADIUS = 17f
        const val SIGHT = 330f
        private const val HEARING = 85f
        private const val HALF_FOV = 70f
        private const val DIRECT_RANGE = 140f
        private const val REPATH_TIME = 0.3f
        private const val SEARCH_TIME = 2.6f
        private const val WANDER_RADIUS = 420f
        private const val STUCK_TIME = 1.2f
    }
}
