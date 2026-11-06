package com.greencoolerapple.santagame.easter

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.awards.Award
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.ui.Button
import com.greencoolerapple.santagame.ui.Draw

class GreenApple(private val game: SantaGame) {
    private enum class State { HIDDEN, WAITING, ENTER, TALK, CHOICE, LEAVE }

    private var state = State.HIDDEN
    private var stateTime = 0f
    private var time = 0f
    private var delay = 0f
    private var topic = TOPICS[0]
    private var lines = listOf<String>()
    private var lineIndex = 0
    private var typed = 0f
    private var afterChoice = false
    private var rewarded = false
    private var rewardTime = -1f
    private var bounce = 0f
    private var width = 0f
    private var height = 0f

    private val choiceA = Button("", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 1.6f)
    private val choiceB = Button("", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 1.6f)
    private val tint = Color()

    val active: Boolean
        get() = state != State.HIDDEN && state != State.WAITING

    fun roll() {
        if (game.appleRolled) return
        game.appleRolled = true
        if (MathUtils.randomBoolean(APPEAR_CHANCE)) {
            state = State.WAITING
            delay = APPEAR_DELAY
        }
    }

    private fun appear() {
        topic = TOPICS.random()
        val greeting = if (game.data.appleMeetings == 0) "apple.helloFirst" else "apple.helloAgain"
        game.data.appleMeetings++
        game.persist()
        game.awards.unlock(Award.TALKING_APPLE)
        lines = listOf(I18n.t(greeting), I18n.t("apple.$topic.q"))
        lineIndex = 0
        typed = 0f
        afterChoice = false
        rewarded = false
        rewardTime = -1f
        setState(State.ENTER)
        game.haptics.play(Haptics.Pattern.AWARD)
    }

    private fun setState(next: State) {
        state = next
        stateTime = 0f
    }

    private val currentLine: String
        get() = lines.getOrElse(lineIndex) { "" }

    private val lineDone: Boolean
        get() = typed >= currentLine.length

    fun update(delta: Float, width: Float, height: Float) {
        this.width = width
        this.height = height
        time += delta
        stateTime += delta
        bounce = maxOf(0f, bounce - delta * 0.6f)
        when (state) {
            State.HIDDEN -> Unit
            State.WAITING -> {
                delay -= delta
                if (delay <= 0f) appear()
            }
            State.ENTER -> if (stateTime >= ENTER_TIME) startLine()
            State.TALK -> typed = minOf(currentLine.length.toFloat(), typed + delta * TYPE_SPEED)
            State.CHOICE -> Unit
            State.LEAVE -> if (stateTime >= LEAVE_TIME) setState(State.HIDDEN)
        }
        layoutChoices()
    }

    private fun startLine() {
        setState(State.TALK)
        typed = 0f
        bounce = 0.12f
        game.haptics.play(Haptics.Pattern.TAP)
        if (afterChoice && lineIndex == lines.lastIndex && !rewarded) {
            rewarded = true
            rewardTime = time
            game.earnCoins(REWARD)
        }
    }

    fun handleTap(x: Float, y: Float): Boolean {
        if (!active) return false
        when (state) {
            State.ENTER -> startLine()
            State.TALK -> when {
                !lineDone -> typed = currentLine.length.toFloat()
                lineIndex < lines.lastIndex -> {
                    lineIndex++
                    startLine()
                }
                !afterChoice -> {
                    setState(State.CHOICE)
                    game.haptics.play(Haptics.Pattern.TAP)
                }
                else -> leave()
            }
            State.CHOICE -> {
                val reply = when {
                    choiceA.contains(x, y) -> "apple.$topic.r1"
                    choiceB.contains(x, y) -> "apple.$topic.r2"
                    else -> null
                }
                if (reply != null) {
                    afterChoice = true
                    lines = listOf(I18n.t(reply), I18n.t("apple.bye"))
                    lineIndex = 0
                    startLine()
                }
            }
            else -> Unit
        }
        return true
    }

    fun handleBack(): Boolean {
        if (!active) return false
        if (state != State.LEAVE) leave()
        return true
    }

    private fun leave() {
        setState(State.LEAVE)
        game.haptics.play(Haptics.Pattern.TAP)
    }

    private fun appleX(): Float {
        val base = width - 150f
        return if (state == State.LEAVE) base + easeIn(stateTime / LEAVE_TIME) * 320f else base
    }

    private fun appleY(): Float {
        val base = 150f + kotlin.math.abs(MathUtils.sin(time * 2.4f)) * 5f
        return when (state) {
            State.ENTER -> base - (1f - easeOutBack(minOf(1f, stateTime / ENTER_TIME))) * 300f
            State.LEAVE -> base + MathUtils.sin(minOf(1f, stateTime / LEAVE_TIME) * MathUtils.PI) * 60f
            else -> base
        }
    }

    private val bubbleRight: Float
        get() = width - 250f

    private fun bubbleLines(): List<String> = game.wrapText(currentLine, TEXT_SCALE, BUBBLE_WIDTH - 44f)

    private fun bubbleHeight(): Float = bubbleLines().size * LINE_HEIGHT + 70f

    private fun layoutChoices() {
        val cx = bubbleRight - BUBBLE_WIDTH / 2f
        choiceA.place(cx, 150f, BUBBLE_WIDTH, 54f)
        choiceB.place(cx, 86f, BUBBLE_WIDTH, 54f)
        choiceA.label = I18n.t("apple.$topic.a1")
        choiceB.label = I18n.t("apple.$topic.a2")
    }

    private val showBubble: Boolean
        get() = state == State.TALK || state == State.CHOICE

    fun drawShapes(shapes: ShapeRenderer) {
        if (!active) return
        val dim = when (state) {
            State.ENTER -> minOf(1f, stateTime / ENTER_TIME)
            State.LEAVE -> 1f - minOf(1f, stateTime / LEAVE_TIME)
            else -> 1f
        }
        tint.set(0f, 0f, 0f, 0.45f * dim)
        shapes.color = tint
        shapes.rect(0f, 0f, width, height)

        if (showBubble) {
            val bh = bubbleHeight()
            val bx = bubbleRight - BUBBLE_WIDTH
            val by = BUBBLE_BOTTOM
            shapes.color = Palette.BUBBLE_EDGE
            Draw.roundedRect(shapes, bx - 4f, by - 4f, BUBBLE_WIDTH + 8f, bh + 8f, 26f)
            shapes.triangle(bubbleRight - 30f, by + 30f, bubbleRight + 30f, by + 20f, bubbleRight - 4f, by + 70f)
            shapes.color = Palette.BUBBLE
            Draw.roundedRect(shapes, bx, by, BUBBLE_WIDTH, bh, 22f)
            shapes.triangle(bubbleRight - 32f, by + 34f, bubbleRight + 22f, by + 26f, bubbleRight - 8f, by + 64f)
        }
        if (state == State.CHOICE) {
            choiceA.drawShape(shapes)
            choiceB.drawShape(shapes)
        }

        val talking = state == State.TALK && !lineDone && ((time * 12f).toInt() % 2 == 0)
        drawApple(shapes, appleX(), appleY(), APPLE_RADIUS * (1f + bounce), talking)

        if (rewardTime >= 0f && time - rewardTime < REWARD_SHOW) {
            val t = (time - rewardTime) / REWARD_SHOW
            Draw.goldenGift(shapes, appleX() - 40f, appleY() + 110f + t * 50f, 30f)
        }
    }

    fun drawTexts() {
        if (!active) return
        if (showBubble) {
            val bx = bubbleRight - BUBBLE_WIDTH
            val top = BUBBLE_BOTTOM + bubbleHeight()
            game.drawText(I18n.t("apple.name"), bx + 22f, top - 14f, 1.15f, Palette.APPLE_DARK, centered = false, shadow = false)
            var remaining = typed.toInt()
            for ((i, line) in bubbleLines().withIndex()) {
                if (remaining <= 0) break
                val visible = if (remaining >= line.length) line else line.substring(0, remaining)
                remaining -= line.length + 1
                game.drawText(visible, bx + 22f, top - 46f - i * LINE_HEIGHT, TEXT_SCALE, Palette.TEXT_DARK, centered = false, shadow = false)
            }
            if (state == State.TALK && lineDone) {
                val blink = (MathUtils.sin(time * 5f) + 1f) / 2f
                game.drawText(I18n.t("apple.tap"), bubbleRight - 90f, BUBBLE_BOTTOM + 18f, 0.95f, Palette.TEXT_DIM, alpha = 0.5f + blink * 0.5f, shadow = false)
            }
        }
        if (state == State.CHOICE) {
            choiceA.drawLabel(game)
            choiceB.drawLabel(game)
        }
        if (rewardTime >= 0f && time - rewardTime < REWARD_SHOW) {
            val t = (time - rewardTime) / REWARD_SHOW
            game.drawText(I18n.t("apple.reward", REWARD), appleX() + 4f, appleY() + 110f + t * 50f, 2f, Palette.GOLD_LIGHT, alpha = 1f - t * t)
        }
    }

    private fun drawApple(shapes: ShapeRenderer, cx: Float, cy: Float, r: Float, talking: Boolean) {
        tint.set(Palette.SHADOW)
        shapes.color = tint
        shapes.ellipse(cx - r * 0.85f, cy - r * 1.12f, r * 1.7f, r * 0.3f, 24)

        drawBody(shapes, cx + r * 0.04f, cy - r * 0.05f, r, Palette.APPLE_DARK, 1.04f)
        drawBody(shapes, cx, cy, r, Palette.APPLE, 1f)
        shapes.color = Palette.APPLE_LIGHT
        shapes.circle(cx - r * 0.42f, cy + r * 0.38f, r * 0.18f, 16)

        shapes.color = Palette.APPLE_STEM
        shapes.rect(cx - r * 0.05f, cy + r * 0.66f, r * 0.1f, r * 0.3f)
        shapes.color = Palette.APPLE_LEAF
        shapes.triangle(cx + r * 0.04f, cy + r * 0.86f, cx + r * 0.5f, cy + r * 1.06f, cx + r * 0.22f, cy + r * 0.74f)
        shapes.triangle(cx + r * 0.04f, cy + r * 0.86f, cx + r * 0.5f, cy + r * 1.06f, cx + r * 0.36f, cy + r * 1.12f)

        shapes.color = Palette.GLASSES
        Draw.roundedRect(shapes, cx - r * 0.6f, cy, r * 0.5f, r * 0.3f, r * 0.1f)
        Draw.roundedRect(shapes, cx + r * 0.1f, cy, r * 0.5f, r * 0.3f, r * 0.1f)
        shapes.rect(cx - r * 0.12f, cy + r * 0.17f, r * 0.24f, r * 0.06f)
        shapes.color = Palette.GLASSES_SHINE
        shapes.rect(cx - r * 0.5f, cy + r * 0.2f, r * 0.16f, r * 0.04f)
        shapes.rect(cx + r * 0.2f, cy + r * 0.2f, r * 0.16f, r * 0.04f)

        shapes.color = Palette.MOUTH
        if (talking) {
            shapes.ellipse(cx - r * 0.17f, cy - r * 0.5f, r * 0.34f, r * 0.26f, 16)
            shapes.color = Palette.TONGUE
            shapes.ellipse(cx - r * 0.1f, cy - r * 0.5f, r * 0.2f, r * 0.1f, 12)
        } else {
            shapes.ellipse(cx - r * 0.2f, cy - r * 0.36f, r * 0.4f, r * 0.08f, 12)
        }
        shapes.color = Palette.BLUSH
        shapes.circle(cx - r * 0.6f, cy - r * 0.26f, r * 0.1f, 12)
        shapes.circle(cx + r * 0.6f, cy - r * 0.26f, r * 0.1f, 12)

        shapes.color = Palette.SANTA_RED
        shapes.triangle(cx - r * 0.6f, cy + r * 0.62f, cx + r * 0.4f, cy + r * 0.7f, cx - r * 0.55f, cy + r * 1.42f)
        shapes.color = Palette.SNOW
        Draw.roundedRect(shapes, cx - r * 0.7f, cy + r * 0.54f, r * 1.18f, r * 0.2f, r * 0.1f)
        shapes.circle(cx - r * 0.58f, cy + r * 1.44f, r * 0.13f, 12)
    }

    private fun drawBody(shapes: ShapeRenderer, cx: Float, cy: Float, r: Float, color: Color, k: Float) {
        shapes.color = color
        shapes.circle(cx - r * 0.36f, cy + r * 0.18f, r * 0.64f * k, 24)
        shapes.circle(cx + r * 0.36f, cy + r * 0.18f, r * 0.64f * k, 24)
        shapes.circle(cx, cy - r * 0.22f, r * 0.8f * k, 28)
    }

    private fun easeIn(t: Float): Float = t * t

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val u = t - 1f
        return 1f + c3 * u * u * u + c1 * u * u
    }

    companion object {
        private const val APPEAR_CHANCE = 0.5f
        private const val APPEAR_DELAY = 1.2f
        private const val ENTER_TIME = 0.7f
        private const val LEAVE_TIME = 0.8f
        private const val TYPE_SPEED = 42f
        private const val APPLE_RADIUS = 62f
        private const val BUBBLE_WIDTH = 520f
        private const val BUBBLE_BOTTOM = 196f
        private const val TEXT_SCALE = 1.35f
        private const val LINE_HEIGHT = 30f
        private const val REWARD = 5
        private const val REWARD_SHOW = 1.4f
        private val TOPICS = listOf("snow", "sleigh", "tree", "cookies", "gift", "reindeer", "sick", "brazil")
    }
}
