package com.greencoolerapple.santagame.world.house

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.greencoolerapple.santagame.Palette

class Guard(private val path: List<Vector2>, private val speed: Float, private val shirt: Color) {
    val position = Vector2(path[0])
    var facing = 0f
        private set
    var spotting = false
        private set

    private var target = 1 % path.size
    private var waitTimer = 0f
    private var suspicion = 0f
    private var lookBase = 0f
    private var lookTime = 0f
    private var walkTime = 0f

    init {
        facing = angleTo(path[target])
        lookBase = facing
    }

    fun canSee(point: Vector2, blockers: List<Rectangle>): Boolean {
        val distance = position.dst(point)
        if (distance < NOTICE_RADIUS) return true
        if (distance > VIEW_RANGE) return false
        val diff = MathUtils.atan2(point.y - position.y, point.x - position.x) * MathUtils.radiansToDegrees - facing
        if (kotlin.math.abs(wrap(diff)) > HALF_FOV) return false
        for (rect in blockers) {
            if (Intersector.intersectSegmentRectangle(position, point, rect)) return false
        }
        return true
    }

    fun update(delta: Float, seesSanta: Boolean, santa: Vector2) {
        spotting = seesSanta
        if (seesSanta) {
            facing = turn(facing, angleTo(santa), ALERT_TURN_SPEED * delta)
            suspicion = SUSPICION_TIME
            return
        }
        if (suspicion > 0f) {
            suspicion -= delta
            lookTime += delta
            facing = turn(facing, facing + MathUtils.sin(lookTime * 3f) * 40f, 120f * delta)
            if (suspicion <= 0f) lookBase = facing
            return
        }
        if (waitTimer > 0f) {
            waitTimer -= delta
            lookTime += delta
            facing = lookBase + MathUtils.sin(lookTime * 1.6f) * SWEEP_ANGLE
            return
        }

        val goal = path[target]
        val distance = position.dst(goal)
        val step = speed * delta
        if (distance <= step) {
            position.set(goal)
            target = (target + 1) % path.size
            waitTimer = WAIT_TIME
            lookTime = 0f
            lookBase = facing
        } else {
            position.x += (goal.x - position.x) / distance * step
            position.y += (goal.y - position.y) / distance * step
            facing = turn(facing, angleTo(goal), TURN_SPEED * delta)
            walkTime += delta
        }
    }

    fun drawCone(shapes: ShapeRenderer) {
        shapes.color = if (spotting) Palette.CONE_ALERT else Palette.CONE
        shapes.arc(position.x, position.y, VIEW_RANGE, facing - HALF_FOV, HALF_FOV * 2f, 24)
    }

    fun drawBody(shapes: ShapeRenderer) {
        val x = position.x
        val y = position.y
        val rad = facing * MathUtils.degreesToRadians
        val fx = MathUtils.cos(rad)
        val fy = MathUtils.sin(rad)
        val swing = MathUtils.sin(walkTime * 10f) * 5f

        shapes.color = Palette.SHADOW
        shapes.circle(x + 3f, y - 4f, BODY_RADIUS + 2f, 20)

        shapes.color = Palette.SKIN
        shapes.circle(x + fx * 10f - fy * (15f + swing), y + fy * 10f + fx * (15f + swing), 6f, 10)
        shapes.circle(x + fx * 10f + fy * (15f - swing), y + fy * 10f - fx * (15f - swing), 6f, 10)

        shapes.color = shirt
        shapes.circle(x, y, BODY_RADIUS, 22)

        shapes.color = Palette.GOLD
        shapes.circle(x + fx * 22f, y + fy * 22f, 5f, 10)

        shapes.color = Palette.SKIN
        shapes.circle(x, y, 10f, 16)
        shapes.color = Palette.HAIR
        shapes.circle(x - fx * 4f, y - fy * 4f, 8f, 14)
    }

    private fun angleTo(point: Vector2): Float =
        MathUtils.atan2(point.y - position.y, point.x - position.x) * MathUtils.radiansToDegrees

    private fun turn(current: Float, goal: Float, maxStep: Float): Float {
        val diff = wrap(goal - current)
        return current + MathUtils.clamp(diff, -maxStep, maxStep)
    }

    private fun wrap(angle: Float): Float {
        var a = angle % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    companion object {
        const val VIEW_RANGE = 240f
        const val HALF_FOV = 35f
        const val NOTICE_RADIUS = 48f
        const val BODY_RADIUS = 19f
        private const val SWEEP_ANGLE = 40f
        private const val WAIT_TIME = 1.2f
        private const val SUSPICION_TIME = 1.4f
        private const val TURN_SPEED = 260f
        private const val ALERT_TURN_SPEED = 300f
    }
}
