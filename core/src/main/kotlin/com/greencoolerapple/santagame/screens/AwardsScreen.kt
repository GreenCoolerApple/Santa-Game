package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.awards.Award
import com.greencoolerapple.santagame.awards.AwardTracker
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.input.Action
import com.greencoolerapple.santagame.input.Touches
import com.greencoolerapple.santagame.ui.Button
import com.greencoolerapple.santagame.ui.Chalet
import com.greencoolerapple.santagame.ui.Draw
import com.greencoolerapple.santagame.world.Elf

class AwardsScreen(game: SantaGame) : MenuBase(game) {
    private class Line(val speaker: Int, val key: String)

    private val back = backButton()
    private val clip = Rectangle()
    private val scissor = Rectangle()
    private val panel = Rectangle()
    private val bubble = Rectangle()
    private val tint = Color()
    private val awards = Award.entries.sortedByDescending { game.awards.isUnlocked(it) }

    private val elves = listOf(
        Elf("elves.twig", Palette.ELF_HAIR, Palette.SANTA_RED, 0f),
        Elf("elves.holly", Palette.HAIR, Palette.GOLD, 1.3f),
        Elf("elves.biscuit", Palette.GOLD_DARK, Palette.SCARF_BLUE, 2.6f),
        Elf("elves.sprout", Palette.TRUNK, Palette.SNOW, 3.9f)
    )
    private val laughKeys = IntArray(elves.size)

    private val queue = ArrayDeque<Line>()
    private var current: Line? = null
    private var currentText = ""
    private var currentLines = listOf<String>()
    private var typed = 0f
    private var hold = 0f
    private var quiet = FIRST_CHAT
    private var lastThread = -1

    private var scroll = 0f
    private var velocity = 0f
    private var dragPointer = -1
    private var lastY = 0f

    override val buttons = listOf(back)

    override val showSanta: Boolean = false

    private val maxScroll: Float
        get() = maxOf(0f, awards.size * (CARD_HEIGHT + GAP) - GAP - clip.height)

    private val leftWidth: Float
        get() = panel.x - 24f

    private val tableTop: Float
        get() = worldHeight * 0.3f

    private val cardWidth: Float
        get() = clip.width - 16f

    override fun layout() {
        val w = worldWidth
        val h = worldHeight
        val delta = Gdx.graphics.deltaTime
        panel.set(w - PANEL_WIDTH - 28f, 28f, PANEL_WIDTH, h - 28f - HEADER)
        clip.set(panel.x + 18f, panel.y + 16f, panel.width - 36f, panel.height - 30f)
        back.place(150f, h - 52f, 220f, 58f)
        for ((i, elf) in elves.withIndex()) {
            elf.scale = ELF_SCALE
            elf.x = leftWidth * SEATS[i]
            elf.base = tableTop - 6f
            elf.update(delta)
        }
        updateChat(delta)
        updateScroll(delta)
    }

    private fun updateChat(delta: Float) {
        val line = current
        if (line == null) {
            quiet -= delta
            if (quiet <= 0f) startThread()
            return
        }
        if (currentLines.isEmpty()) currentLines = game.wrapText(currentText, SPEECH_SCALE, BUBBLE_WIDTH - 32f)
        typed += delta * CHARS_PER_SECOND
        elves[line.speaker].talking = typed < currentText.length
        if (typed >= currentText.length) {
            hold -= delta
            if (hold <= 0f) nextLine()
        }
    }

    private fun startThread() {
        var index = MathUtils.random(THREADS.size - 1)
        if (index == lastThread) index = (index + 1) % THREADS.size
        lastThread = index
        queue.clear()
        for ((speaker, key) in THREADS[index]) queue.addLast(Line(speaker, key))
        nextLine()
    }

    private fun nextLine() {
        current?.let { elves[it.speaker].talking = false }
        val next = queue.removeFirstOrNull()
        if (next == null) {
            val speaker = current?.speaker ?: -1
            current = null
            laughAll(speaker, everyone = true)
            quiet = MathUtils.random(QUIET_MIN, QUIET_MAX)
            return
        }
        show(next)
    }

    private fun show(line: Line) {
        current = line
        if (line.key == LAUGH_CUE) {
            currentText = ""
            currentLines = emptyList()
            typed = 0f
            hold = LAUGH_TIME * 0.8f
            laughAll(line.speaker, everyone = false)
            return
        }
        currentText = if (line.key == COUNT_LINE) I18n.t(line.key, game.awards.unlockedCount) else I18n.t(line.key)
        currentLines = emptyList()
        typed = 0f
        hold = HOLD_BASE + currentText.length * HOLD_PER_CHAR
    }

    private fun laughAll(speaker: Int, everyone: Boolean) {
        for ((i, elf) in elves.withIndex()) {
            if (i == speaker && !everyone) continue
            elf.laughTimer = LAUGH_TIME + MathUtils.random(0f, 0.5f)
            laughKeys[i] = MathUtils.random(LAUGHS.size - 1)
        }
    }

    override fun interceptTap(x: Float, y: Float): Boolean {
        for ((i, elf) in elves.withIndex()) {
            if (!elf.contains(x, y)) continue
            current?.let { elves[it.speaker].talking = false }
            queue.clear()
            elf.bounce = 1f
            game.haptics.play(Haptics.Pattern.TAP)
            val pool = TAPS + COUNT_LINE
            show(Line(i, pool[MathUtils.random(pool.size - 1)]))
            for ((j, other) in elves.withIndex()) {
                if (j != i) {
                    other.laughTimer = 0f
                }
            }
            queue.addLast(Line(i, LAUGH_CUE))
            return true
        }
        return false
    }

    private fun updateScroll(delta: Float) {
        val touches = game.touches
        if (dragPointer >= 0 && !touches.down[dragPointer]) dragPointer = -1
        if (dragPointer < 0) {
            for (i in 0 until Touches.MAX_POINTERS) {
                if (touches.justDown[i] && clip.contains(touches.x[i], touches.y[i])) {
                    dragPointer = i
                    lastY = touches.y[i]
                    velocity = 0f
                    break
                }
            }
        }
        if (dragPointer >= 0) {
            val y = touches.y[dragPointer]
            val moved = y - lastY
            lastY = y
            scroll += moved
            if (delta > 0f) velocity = MathUtils.lerp(velocity, moved / delta, 0.4f)
        } else {
            scroll += velocity * delta
            velocity *= maxOf(0f, 1f - delta * 5f)
        }
        if (game.controls.isPressed(Action.MOVE_DOWN)) scroll += KEY_SPEED * delta
        if (game.controls.isPressed(Action.MOVE_UP)) scroll -= KEY_SPEED * delta
        scroll = MathUtils.clamp(scroll, 0f, maxScroll)
    }

    private fun cardY(index: Int): Float = clip.y + clip.height + scroll - index * (CARD_HEIGHT + GAP) - CARD_HEIGHT

    private inline fun forEachVisibleCard(action: (Award, Float, Float) -> Unit) {
        for ((i, award) in awards.withIndex()) {
            val y = cardY(i)
            if (y > clip.y + clip.height || y + CARD_HEIGHT < clip.y) continue
            action(award, clip.x, y)
        }
    }

    override fun onTap(button: Button) {
        if (button == back) onBack()
    }

    override fun onBack() {
        game.setScreen(MainMenuScreen(game))
    }

    override fun drawContentShapes(shapes: ShapeRenderer) {
        val w = worldWidth
        val h = worldHeight
        Chalet.drawWall(shapes, w, h)
        Chalet.drawGarland(shapes, 0f, panel.x - 10f, h, time)
        Chalet.drawWreath(shapes, (270f + panel.x) / 2f, h - 58f)
        drawBanner(shapes)
        for (elf in elves) elf.draw(shapes)
        drawTable(shapes)
        for (elf in elves) elf.drawArms(shapes, tableTop)
        drawTableItems(shapes)
        drawSpeechBubble(shapes)
        drawHeader(shapes)
        Chalet.drawBoard(shapes, panel)

        shapes.flush()
        game.viewport.calculateScissors(shapes.transformMatrix, clip, scissor)
        if (ScissorStack.pushScissors(scissor)) {
            forEachVisibleCard { award, x, y -> drawCard(shapes, award, x, y) }
            shapes.flush()
            ScissorStack.popScissors()
        }
        Chalet.drawScrollbar(shapes, clip, scroll, maxScroll)
    }

    private fun drawBanner(shapes: ShapeRenderer) {
        val y = tableTop + 250f
        val left = 30f
        val right = leftWidth - 30f
        val count = ((right - left) / 46f).toInt().coerceAtLeast(4)
        val step = (right - left) / count
        for (k in 0 until count) {
            val x0 = left + k * step
            val sag = 18f * MathUtils.sin(MathUtils.PI * (k + 0.5f) / count)
            shapes.color = if (k % 2 == 0) Palette.SANTA_RED else Palette.ELF_GREEN
            shapes.triangle(x0 + 3f, y - sag, x0 + step - 3f, y - sag, x0 + step / 2f, y - sag - 34f)
            shapes.color = Palette.GOLD
            shapes.circle(x0 + step / 2f, y - sag - 12f, 3f, 6)
        }
        shapes.color = Palette.LOG_LINE
        for (k in 0 until count) {
            val sag1 = 18f * MathUtils.sin(MathUtils.PI * k / count)
            val sag2 = 18f * MathUtils.sin(MathUtils.PI * (k + 1) / count)
            shapes.rectLine(left + k * step, y - sag1, left + (k + 1) * step, y - sag2, 2f)
        }
    }

    private fun drawTable(shapes: ShapeRenderer) {
        val right = leftWidth
        val top = tableTop
        shapes.color = Palette.SHADOW
        shapes.rect(-10f, 0f, right + 20f, 30f)
        shapes.color = Palette.TABLE_DARK
        shapes.rect(-10f, 20f, right + 10f, top - 26f)
        shapes.color = Palette.TABLE_FRONT
        var px = 20f
        while (px + 120f < right) {
            Draw.roundedRect(shapes, px, 44f, 120f, top - 100f, 10f)
            px += 150f
        }
        shapes.color = Palette.TABLE_TOP
        Draw.roundedRect(shapes, -20f, top - 18f, right + 30f, 30f, 10f)
        shapes.color = Palette.ELF_GREEN_DARK
        shapes.rect(-20f, top - 46f, right + 30f, 30f)
        shapes.color = Palette.SANTA_RED
        var sx = -10f
        while (sx < right + 10f) {
            shapes.triangle(sx - 14f, top - 46f, sx + 14f, top - 46f, sx, top - 62f)
            sx += 28f
        }
        shapes.color = Palette.GOLD
        shapes.rect(-20f, top - 20f, right + 30f, 4f)
    }

    private fun drawTableItems(shapes: ShapeRenderer) {
        val top = tableTop
        for ((i, elf) in elves.withIndex()) {
            val mx = elf.x + if (i % 2 == 0) 26f else -26f
            shapes.color = if (i % 2 == 0) Palette.SANTA_RED else Palette.SNOW
            Draw.roundedRect(shapes, mx - 13f, top + 2f, 26f, 30f, 5f)
            shapes.rectLine(mx + 13f, top + 22f, mx + 20f, top + 16f, 4f)
            shapes.rectLine(mx + 20f, top + 16f, mx + 13f, top + 8f, 4f)
            shapes.color = Palette.TRUNK
            shapes.ellipse(mx - 11f, top + 26f, 22f, 7f, 10)
            for (k in 0 until 2) {
                val p = ((time * 0.6f + k * 0.5f + i * 0.21f) % 1f)
                tint.set(Palette.SNOW)
                tint.a = 0.4f * (1f - p)
                shapes.color = tint
                shapes.circle(mx + MathUtils.sin(p * 6f + k + i) * 5f, top + 40f + p * 40f, 4f + p * 5f, 8)
            }
        }
        val cx = (elves[1].x + elves[2].x) / 2f
        shapes.color = Palette.GOLD_DARK
        Draw.roundedRect(shapes, cx - 30f, top + 2f, 60f, 14f, 4f)
        AwardTracker.drawTrophy(shapes, cx, top + 50f, 1.25f)
        val shine = (MathUtils.sin(time * 3f) + 1f) / 2f
        tint.set(Palette.GOLD_LIGHT)
        tint.a = 0.5f + 0.5f * shine
        shapes.color = tint
        Draw.star(shapes, cx + 20f, top + 78f, 6f + shine * 2f, time)

        for (side in intArrayOf(0, 3)) {
            val px = elves[side].x + if (side == 0) -70f else 70f
            if (px < 30f || px > leftWidth - 30f) continue
            shapes.color = Palette.SNOW_SHADE
            shapes.ellipse(px - 26f, top + 2f, 52f, 12f, 16)
            shapes.color = Palette.TABLE_TOP
            for (k in 0 until 3) shapes.circle(px - 12f + k * 12f, top + 12f + (k % 2) * 4f, 8f, 12)
            shapes.color = Palette.TRUNK
            for (k in 0 until 3) shapes.circle(px - 12f + k * 12f, top + 13f + (k % 2) * 4f, 2f, 6)
        }
    }

    private fun bubbleFor(line: Line): Rectangle {
        val elf = elves[line.speaker]
        val lines = maxOf(1, currentLines.size)
        val height = 22f + lines * LINE_HEIGHT
        val x = MathUtils.clamp(elf.x - BUBBLE_WIDTH / 2f, 12f, leftWidth - BUBBLE_WIDTH - 12f)
        val y = elf.headY + 96f * ELF_SCALE
        bubble.set(x, y, BUBBLE_WIDTH, height)
        return bubble
    }

    private fun drawSpeechBubble(shapes: ShapeRenderer) {
        val line = current ?: return
        if (line.key == LAUGH_CUE) return
        val r = bubbleFor(line)
        val elf = elves[line.speaker]
        Chalet.drawBubble(shapes, r, elf.x, elf.headY + 78f * ELF_SCALE)
        Chalet.drawNameTag(shapes, r.x + 12f, r.y + r.height - 12f, game.textWidth(I18n.t(elf.nameKey), 1.05f) + 22f)
    }

    private fun drawHeader(shapes: ShapeRenderer) {
        val x = panel.x + 10f
        val y = worldHeight - 104f
        val width = panel.width - 20f
        val progress = game.awards.unlockedCount / Award.entries.size.toFloat()
        shapes.color = Palette.LOG_LINE
        Draw.roundedRect(shapes, x - 3f, y - 3f, width + 6f, 30f, 15f)
        shapes.color = Palette.TOAST_BG
        Draw.roundedRect(shapes, x, y, width, 24f, 12f)
        if (progress > 0f) {
            shapes.color = Palette.GOLD
            Draw.roundedRect(shapes, x, y, maxOf(24f, width * progress), 24f, 12f)
            shapes.color = Palette.GOLD_LIGHT
            Draw.roundedRect(shapes, x + 6f, y + 14f, maxOf(12f, width * progress - 12f), 5f, 2.5f)
        }
        AwardTracker.drawTrophy(shapes, panel.x + 34f, worldHeight - 46f, 0.9f)
    }

    private fun drawCard(shapes: ShapeRenderer, award: Award, x: Float, y: Float) {
        val unlocked = game.awards.isUnlocked(award)
        shapes.color = if (unlocked) Palette.GOLD else Palette.SHOP_CARD_EDGE
        Draw.roundedRect(shapes, x - 3f, y - 3f, cardWidth + 6f, CARD_HEIGHT + 6f, 18f)
        shapes.color = if (unlocked) Palette.SHOP_CARD else Palette.SHOP_CARD_LOCKED
        Draw.roundedRect(shapes, x, y, cardWidth, CARD_HEIGHT, 16f)
        val cy = y + CARD_HEIGHT / 2f
        shapes.color = if (unlocked) Palette.PANEL_GREEN_DARK else Palette.LOCK_BG
        shapes.circle(x + 46f, cy, 34f, 26)
        shapes.color = if (unlocked) Palette.PANEL_GREEN else Palette.SHOP_INK_SOFT
        shapes.circle(x + 46f, cy, 30f, 26)
        if (unlocked) {
            AwardTracker.drawTrophy(shapes, x + 46f, cy, 0.85f)
            shapes.color = Palette.SANTA_RED
            shapes.triangle(x + cardWidth - 40f, y + CARD_HEIGHT, x + cardWidth, y + CARD_HEIGHT, x + cardWidth, y + CARD_HEIGHT - 40f)
            shapes.color = Palette.GOLD
            Draw.star(shapes, x + cardWidth - 12f, y + CARD_HEIGHT - 12f, 7f, 0f)
        } else {
            AwardTracker.drawLock(shapes, x + 46f, cy + 4f)
        }
    }

    override fun drawTexts() {
        val h = worldHeight
        game.drawText(I18n.t("awards.title"), panel.x + panel.width / 2f + 20f, h - 46f, 3f, Palette.GOLD)
        game.drawText("${game.awards.unlockedCount} / ${Award.entries.size}", panel.x + panel.width / 2f, h - 92f, 1.3f, Palette.SNOW)
        game.drawText(I18n.t("awards.elvesHint"), leftWidth / 2f, 40f, 1.25f, Palette.GOLD_LIGHT, alpha = 0.6f + 0.4f * MathUtils.sin(time * 2.5f))

        for ((i, elf) in elves.withIndex()) {
            if (!elf.laughing) continue
            val t = 1f - elf.laughTimer / (LAUGH_TIME + 0.5f)
            game.drawText(I18n.t(LAUGHS[laughKeys[i]]), elf.x + 46f * ELF_SCALE, elf.headY + 40f + t * 30f, 1.2f, Palette.GOLD_LIGHT, alpha = minOf(1f, elf.laughTimer * 2f))
        }

        val line = current
        if (line != null && line.key != LAUGH_CUE) {
            val r = bubble
            game.drawText(I18n.t(elves[line.speaker].nameKey), r.x + 23f, r.y + r.height + 6f, 1.05f, Palette.SNOW, centered = false)
            var budget = typed.toInt()
            for ((i, text) in currentLines.withIndex()) {
                if (budget <= 0) break
                val shown = if (budget >= text.length) text else text.substring(0, budget)
                budget -= text.length + 1
                game.drawText(shown, r.x + 16f, r.y + r.height - 16f - i * LINE_HEIGHT, SPEECH_SCALE, Palette.TEXT_DARK, centered = false, shadow = false)
            }
        }

        val batch = game.batch
        batch.flush()
        game.viewport.calculateScissors(batch.transformMatrix, clip, scissor)
        if (ScissorStack.pushScissors(scissor)) {
            forEachVisibleCard { award, x, y ->
                val unlocked = game.awards.isUnlocked(award)
                game.drawText(award.title, x + 92f, y + CARD_HEIGHT - 16f, 1.5f, if (unlocked) Palette.SHOP_INK else Palette.SHOP_INK_SOFT, centered = false, shadow = false)
                val lines = game.wrapText(award.description, 0.95f, cardWidth - 92f - 30f)
                for ((i, text) in lines.take(2).withIndex()) {
                    game.drawText(text, x + 92f, y + CARD_HEIGHT - 48f - i * 19f, 0.95f, Palette.SHOP_INK_SOFT, centered = false, shadow = false)
                }
                if (!unlocked) {
                    game.drawText(I18n.t("awards.locked"), x + cardWidth - 60f, y + 16f, 0.95f, Palette.SHOP_INK_SOFT, shadow = false)
                }
            }
            batch.flush()
            ScissorStack.popScissors()
        }
    }

    companion object {
        private const val PANEL_WIDTH = 560f
        private const val HEADER = 140f
        private const val CARD_HEIGHT = 84f
        private const val GAP = 10f
        private const val KEY_SPEED = 700f
        private const val ELF_SCALE = 0.82f
        private const val BUBBLE_WIDTH = 300f
        private const val SPEECH_SCALE = 1.1f
        private const val LINE_HEIGHT = 24f
        private const val CHARS_PER_SECOND = 38f
        private const val HOLD_BASE = 1.2f
        private const val HOLD_PER_CHAR = 0.025f
        private const val LAUGH_TIME = 1.6f
        private const val FIRST_CHAT = 1.2f
        private const val QUIET_MIN = 3.5f
        private const val QUIET_MAX = 6.5f
        private const val LAUGH_CUE = "laugh"
        private const val COUNT_LINE = "elves.tap.count"

        private val SEATS = floatArrayOf(0.15f, 0.38f, 0.62f, 0.85f)
        private val LAUGHS = listOf("elves.laugh1", "elves.laugh2", "elves.laugh3")
        private val TAPS = listOf(
            "elves.tap.1", "elves.tap.2", "elves.tap.3", "elves.tap.4",
            "elves.tap.5", "elves.tap.6", "elves.tap.7", "elves.tap.8"
        )
        private val THREADS = listOf(
            listOf(0 to "elves.c1.1", 1 to "elves.c1.2", 2 to "elves.c1.3"),
            listOf(3 to "elves.c2.1", 2 to "elves.c2.2"),
            listOf(1 to "elves.c3.1", 0 to "elves.c3.2"),
            listOf(2 to "elves.c4.1", 3 to "elves.c4.2"),
            listOf(0 to "elves.c5.1", 1 to "elves.c5.2", 0 to "elves.c5.3"),
            listOf(3 to "elves.c6.1", 2 to "elves.c6.2", 3 to "elves.c6.3"),
            listOf(1 to "elves.c7.1", 3 to "elves.c7.2", 1 to "elves.c7.3")
        )
    }
}
