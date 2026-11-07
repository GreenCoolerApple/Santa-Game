package com.greencoolerapple.santagame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.world.WalkingElf

class MenuVillage(private val game: SantaGame) {
    private val elves = listOf(
        WalkingElf(Palette.ELF_HAIR, Palette.SANTA_RED, 0f),
        WalkingElf(Palette.HAIR, Palette.GOLD, 1.1f),
        WalkingElf(Palette.GOLD_DARK, Palette.SCARF_BLUE, 2.2f),
        WalkingElf(Palette.TRUNK, Palette.SNOW, 3.3f),
        WalkingElf(Palette.ELF_HAIR, Palette.ELF_GREEN_DARK, 4.4f)
    )
    private val busy = BooleanArray(elves.size)
    private val tint = Color()
    private val bubble = Rectangle()

    private var placed = false
    private var width = 0f
    private var time = 0f

    private var first = -1
    private var second = -1
    private var pair = 0
    private var stage = 0
    private var stageTimer = 0f
    private var chatTimer = FIRST_CHAT
    private var meetX = 0f
    private var speaker = -1
    private var lines = listOf<String>()

    fun update(delta: Float, worldWidth: Float) {
        time += delta
        width = worldWidth
        if (!placed) {
            placed = true
            for ((i, elf) in elves.withIndex()) {
                elf.scale = SCALE
                elf.y = 6f + i * 9f
                elf.x = MathUtils.random(0.05f, 0.95f) * worldWidth
                elf.targetX = elf.x
                elf.wait = MathUtils.random(0f, 2f)
            }
        }
        for ((i, elf) in elves.withIndex()) {
            elf.update(delta)
            if (!busy[i]) roam(elf, delta)
        }
        updateChat(delta)
    }

    private fun roam(elf: WalkingElf, delta: Float) {
        if (elf.wait > 0f) {
            elf.wait -= delta
            elf.action = WalkingElf.Action.IDLE
            return
        }
        elf.action = WalkingElf.Action.WALK
        if (elf.walkTowards(elf.targetX, SPEED, delta)) {
            elf.wait = MathUtils.random(0.8f, 3f)
            elf.targetX = MathUtils.random(0.04f, 0.96f) * width
        }
    }

    private fun updateChat(delta: Float) {
        when (stage) {
            0 -> {
                chatTimer -= delta
                if (chatTimer > 0f) return
                first = MathUtils.random(elves.size - 1)
                second = (first + 1 + MathUtils.random(elves.size - 2)) % elves.size
                busy[first] = true
                busy[second] = true
                meetX = MathUtils.clamp((elves[first].x + elves[second].x) / 2f, 0.08f * width, 0.92f * width)
                pair = MathUtils.random(PAIRS - 1)
                stage = 1
            }
            1 -> {
                val a = elves[first]
                val b = elves[second]
                a.action = WalkingElf.Action.WALK
                b.action = WalkingElf.Action.WALK
                val aDone = a.walkTowards(meetX - MEET_GAP, SPEED * 1.2f, delta)
                val bDone = b.walkTowards(meetX + MEET_GAP, SPEED * 1.2f, delta)
                if (aDone) a.action = WalkingElf.Action.IDLE
                if (bDone) b.action = WalkingElf.Action.IDLE
                if (aDone && bDone) {
                    a.dir = 1f
                    b.dir = -1f
                    talk(first, "menu.elf.a${pair + 1}")
                    stage = 2
                }
            }
            2, 3 -> {
                stageTimer -= delta
                val elf = elves[speaker]
                elf.talking = TALK_TIME - stageTimer < lineLength() / CHARS_PER_SECOND
                if (stageTimer <= 0f) {
                    elf.talking = false
                    if (stage == 2) {
                        talk(second, "menu.elf.b${pair + 1}")
                        stage = 3
                    } else {
                        elves[first].bounce = 1f
                        elves[second].bounce = 1f
                        busy[first] = false
                        busy[second] = false
                        elves[first].wait = 0.6f
                        elves[second].wait = 0.6f
                        speaker = -1
                        stage = 0
                        chatTimer = MathUtils.random(CHAT_MIN, CHAT_MAX)
                    }
                }
            }
        }
    }

    private fun lineLength(): Int = lines.sumOf { it.length }

    private fun talk(index: Int, key: String) {
        speaker = index
        lines = game.wrapText(I18n.t(key), TEXT_SCALE, BUBBLE_MAX - 24f)
        stageTimer = TALK_TIME
        elves[index].talking = true
    }

    fun drawChalets(shapes: ShapeRenderer, worldWidth: Float) {
        drawChalet(shapes, worldWidth * 0.27f, SantaGame.GROUND_HEIGHT - 10f, 0.62f, 0f)
        drawChalet(shapes, worldWidth * 0.73f, SantaGame.GROUND_HEIGHT - 6f, 0.55f, 1.7f)
    }

    private fun drawChalet(shapes: ShapeRenderer, cx: Float, base: Float, s: Float, phase: Float) {
        val half = 90f * s
        val wall = 92f * s
        var row = 0
        var y = base
        while (y < base + wall - 0.5f) {
            shapes.color = if (row % 2 == 0) Palette.LOG else Palette.LOG_DARK
            shapes.rect(cx - half, y, half * 2f, minOf(14f * s, base + wall - y))
            shapes.color = Palette.LOG_END
            shapes.circle(cx - half - 2f, y + 7f * s, 7f * s, 8)
            shapes.circle(cx + half + 2f, y + 7f * s, 7f * s, 8)
            y += 14f * s
            row++
        }
        val top = base + wall
        shapes.color = Palette.BRICK
        shapes.rect(cx + half * 0.45f, top + 20f * s, 22f * s, 60f * s)
        for (k in 0 until 3) {
            val p = ((time * 0.3f + phase + k * 0.33f) % 1f)
            tint.set(Palette.SNOW)
            tint.a = 0.4f * (1f - p)
            shapes.color = tint
            shapes.circle(cx + half * 0.45f + 11f * s + p * 26f * s, top + 86f * s + p * 70f * s, (7f + p * 12f) * s, 10)
        }
        shapes.color = Palette.ROOF
        shapes.triangle(cx - half - 26f * s, top - 4f * s, cx + half + 26f * s, top - 4f * s, cx, top + 86f * s)
        shapes.color = Palette.SNOW
        shapes.triangle(cx - half - 14f * s, top + 8f * s, cx + half + 14f * s, top + 8f * s, cx, top + 92f * s)
        var sx = cx - half - 26f * s
        while (sx <= cx + half + 26f * s) {
            shapes.circle(sx, top, 6f * s, 8)
            sx += 12f * s
        }

        val glow = 0.25f + 0.1f * MathUtils.sin(time * 3f + phase)
        tint.set(Palette.WARM_GLOW)
        tint.a = glow
        shapes.color = tint
        shapes.circle(cx - half * 0.55f, base + wall * 0.5f, 34f * s, 16)
        shapes.color = Palette.LOG_LINE
        shapes.rect(cx - half * 0.55f - 20f * s, base + wall * 0.3f, 40f * s, 36f * s)
        shapes.color = Palette.WARM_LIGHT
        shapes.rect(cx - half * 0.55f - 16f * s, base + wall * 0.3f + 4f * s, 32f * s, 28f * s)
        shapes.color = Palette.LOG_LINE
        shapes.rect(cx - half * 0.55f - 1.5f * s, base + wall * 0.3f + 4f * s, 3f * s, 28f * s)
        shapes.color = Palette.SANTA_RED_DARK
        shapes.rect(cx + half * 0.25f, base, 34f * s, 58f * s)
        shapes.color = Palette.GOLD
        shapes.circle(cx + half * 0.25f + 27f * s, base + 28f * s, 3f * s, 6)

        var bulb = 0
        var bx = cx - half - 16f * s
        while (bx <= cx + half + 16f * s) {
            val color = Chalet.BULBS[bulb % Chalet.BULBS.size]
            val lit = (bulb + (time * 3f).toInt()) % 2 == 0
            tint.set(color)
            if (!lit) tint.mul(0.6f, 0.6f, 0.6f, 1f)
            shapes.color = tint
            shapes.circle(bx, top - 9f * s, 4f * s, 8)
            bx += 20f * s
            bulb++
        }
    }

    fun drawElves(shapes: ShapeRenderer) {
        for (elf in elves.sortedByDescending { it.y }) elf.draw(shapes)
    }

    fun drawBubble(shapes: ShapeRenderer) {
        if (speaker < 0) return
        val elf = elves[speaker]
        val textWidth = lines.maxOfOrNull { game.textWidth(it, TEXT_SCALE) } ?: 0f
        val w = textWidth + 24f
        val h = 14f + lines.size * LINE_HEIGHT
        val x = MathUtils.clamp(elf.x - w / 2f, 8f, width - w - 8f)
        bubble.set(x, elf.topY + 18f, w, h)
        Chalet.drawBubble(shapes, bubble, elf.x, elf.topY + 4f)
    }

    fun drawTexts() {
        if (speaker < 0) return
        var budget = ((TALK_TIME - stageTimer) * CHARS_PER_SECOND).toInt()
        for ((i, line) in lines.withIndex()) {
            if (budget <= 0) break
            val text = if (budget >= line.length) line else line.substring(0, budget)
            budget -= line.length + 1
            game.drawText(text, bubble.x + 12f, bubble.y + bubble.height - 10f - i * LINE_HEIGHT, TEXT_SCALE, Palette.TEXT_DARK, centered = false, shadow = false)
        }
    }

    companion object {
        private const val SCALE = 0.42f
        private const val SPEED = 46f
        private const val MEET_GAP = 22f
        private const val PAIRS = 6
        private const val TALK_TIME = 2.6f
        private const val CHARS_PER_SECOND = 34f
        private const val FIRST_CHAT = 2.5f
        private const val CHAT_MIN = 3f
        private const val CHAT_MAX = 6f
        private const val TEXT_SCALE = 0.85f
        private const val LINE_HEIGHT = 19f
        private const val BUBBLE_MAX = 230f
    }
}
