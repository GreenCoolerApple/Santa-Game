package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.ui.Button
import com.greencoolerapple.santagame.ui.Chalet
import com.greencoolerapple.santagame.ui.Draw
import com.greencoolerapple.santagame.world.Sparkles
import com.greencoolerapple.santagame.world.WalkingElf

class OptionsScreen(game: SantaGame) : MenuBase(game) {
    private class Row(val labelKey: String, val value: () -> String, val change: (Int) -> Unit) {
        val down = Button("<", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 2.2f)
        val up = Button(">", fill = Palette.BUTTON_DARK, edge = Palette.BUTTON_DARK_EDGE, labelScale = 2.2f)
        val card = Rectangle()
    }

    private class Dust(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val gold: Boolean)

    private enum class CarrierStep { TO_PILE, PICK, TO_SITE, PLACE }

    private val rows = listOf(
        Row("options.fps", { game.settings.fpsLabel }, { game.changeFps(it) }),
        Row("options.language", { game.settings.language.label }, { game.changeLanguage(it) }),
        Row("options.music", { game.settings.musicLabel }, { game.changeMusicVolume(it) })
    ) + if (game.isMobile) {
        listOf(
            Row("options.haptics", { game.settings.hapticsLabel }, { game.toggleHaptics() }),
            Row("options.hapticsIntensity", { game.settings.intensityLabel }, { game.changeHapticIntensity(it) })
        )
    } else {
        emptyList()
    }
    private val saveButton = Button("", labelScale = 1.9f)
    private val loadButton = Button("", fill = Palette.BUTTON_ALT, edge = Palette.BUTTON_ALT_EDGE, labelScale = 1.9f)
    private val controlsButton = Button("", fill = Palette.BUTTON_SHOP, edge = Palette.BUTTON_SHOP_EDGE, labelScale = 1.9f)
    private val back = backButton()

    private val bottomButtons = listOf(saveButton, loadButton) + if (game.isDesktop) listOf(controlsButton) else emptyList()

    override val buttons = rows.flatMap { listOf(it.down, it.up) } + bottomButtons + listOf(back)

    override val showSanta: Boolean = false

    private val panel = Rectangle()
    private val bubble = Rectangle()
    private val tint = Color()
    private val sparkles = Sparkles()
    private val dust = mutableListOf<Dust>()

    private val carrier = WalkingElf(Palette.ELF_HAIR, Palette.SANTA_RED, 0f)
    private val hammerer = WalkingElf(Palette.HAIR, Palette.GOLD, 0.7f)
    private val sawyer = WalkingElf(Palette.GOLD_DARK, Palette.SCARF_BLUE, 1.4f)
    private val foreman = WalkingElf(Palette.TRUNK, Palette.SNOW, 2.1f)
    private val wanderer = WalkingElf(Palette.ELF_HAIR, Palette.ELF_GREEN_DARK, 2.8f)
    private val elves = listOf(carrier, hammerer, sawyer, foreman, wanderer)

    private var placed = false
    private var built = 0
    private var celebrate = 0f
    private var carrierStep = CarrierStep.TO_PILE
    private var stepTimer = 0f

    private var speaker = -1
    private var speech = ""
    private var speechLines = listOf<String>()
    private var speechTimer = 0f
    private var chatTimer = FIRST_CHAT
    private var lastLine = -1

    private val leftWidth: Float
        get() = panel.x - 24f

    private val siteX: Float
        get() = leftWidth * 0.6f

    private val pileX: Float
        get() = leftWidth * 0.1f

    override fun layout() {
        val w = worldWidth
        val h = worldHeight
        val delta = Gdx.graphics.deltaTime
        panel.set(w - PANEL_WIDTH - 28f, 28f, PANEL_WIDTH, h - 28f - HEADER)
        back.place(150f, h - 52f, 220f, 58f)

        var y = panel.y + panel.height - 22f - ROW_HEIGHT / 2f
        for (row in rows) {
            row.card.set(panel.x + 16f, y - ROW_HEIGHT / 2f, panel.width - 32f, ROW_HEIGHT)
            row.down.place(row.card.x + DOWN_OFFSET, y, 60f, 50f)
            row.up.place(row.card.x + row.card.width - 40f, y, 60f, 50f)
            y -= ROW_HEIGHT + ROW_GAP
        }
        val gap = 12f
        val count = bottomButtons.size
        val bw = (panel.width - 32f - gap * (count - 1)) / count
        for ((i, button) in bottomButtons.withIndex()) {
            button.place(panel.x + 16f + bw / 2f + i * (bw + gap), panel.y + 52f, bw, 68f)
        }
        saveButton.label = I18n.t("options.save")
        loadButton.label = I18n.t("options.load")
        controlsButton.label = I18n.t("options.controls")

        if (!placed) placeElves()
        updateWorkshop(delta)
    }

    private fun placeElves() {
        placed = true
        for (elf in elves) elf.scale = ELF_SCALE
        carrier.x = pileX + 60f
        carrier.y = 44f
        hammerer.x = siteX + SITE_HALF + 40f
        hammerer.y = 52f
        hammerer.dir = -1f
        sawyer.x = pileX + 150f
        sawyer.y = 30f
        sawyer.dir = 1f
        foreman.x = siteX - 40f
        foreman.y = 20f
        foreman.targetX = foreman.x
        wanderer.x = leftWidth * 0.35f
        wanderer.y = 8f
        wanderer.targetX = wanderer.x
    }

    private fun updateWorkshop(delta: Float) {
        sparkles.update(delta)
        for (elf in elves) elf.update(delta)
        updateCarrier(delta)
        roam(foreman, siteX - SITE_HALF - 20f, siteX + 20f, delta)
        roam(wanderer, 40f, leftWidth - 40f, delta)

        val working = celebrate <= 0f
        hammerer.action = if (working) WalkingElf.Action.HAMMER else WalkingElf.Action.IDLE
        sawyer.action = if (working) WalkingElf.Action.SAW else WalkingElf.Action.IDLE
        if (hammerer.hammerHit() && working) {
            sparkles.burst(hammerer.hammerHead() - 10f, hammerer.y + 70f * ELF_SCALE, Palette.GOLD_LIGHT, 5)
            spawnDust(hammerer.hammerHead() - 14f, hammerer.y + 64f * ELF_SCALE, 3, false)
        }
        if (working && MathUtils.random() < delta * 14f) {
            spawnDust(sawyer.x + 50f * ELF_SCALE, sawyer.y + 38f * ELF_SCALE, 1, true)
        }

        val iterator = dust.iterator()
        while (iterator.hasNext()) {
            val d = iterator.next()
            d.life -= delta
            d.vy -= 220f * delta
            d.x += d.vx * delta
            d.y += d.vy * delta
            if (d.life <= 0f || d.y < 0f) iterator.remove()
        }

        if (celebrate > 0f) {
            celebrate -= delta
            if (celebrate <= 0f) built = 0
        }

        speechTimer = maxOf(0f, speechTimer - delta)
        if (speaker >= 0) {
            elves[speaker].talking = SPEECH_TIME - speechTimer < speech.length / CHARS_PER_SECOND
            if (speechTimer <= 0f) {
                elves[speaker].talking = false
                speaker = -1
            }
        }
        chatTimer -= delta
        if (chatTimer <= 0f && speaker < 0) {
            chatTimer = MathUtils.random(CHAT_MIN, CHAT_MAX)
            say(MathUtils.random(elves.size - 1), I18n.t(nextLine()))
        }
    }

    private fun nextLine(): String {
        var line = MathUtils.random(LINES.size - 1)
        if (line == lastLine) line = (line + 1) % LINES.size
        lastLine = line
        return LINES[line]
    }

    private fun updateCarrier(delta: Float) {
        when (carrierStep) {
            CarrierStep.TO_PILE -> {
                carrier.action = WalkingElf.Action.WALK
                if (carrier.walkTowards(pileX + 46f, CARRY_SPEED, delta)) {
                    carrierStep = CarrierStep.PICK
                    stepTimer = 0.6f
                    carrier.action = WalkingElf.Action.IDLE
                }
            }
            CarrierStep.PICK -> {
                stepTimer -= delta
                if (stepTimer <= 0f) carrierStep = CarrierStep.TO_SITE
            }
            CarrierStep.TO_SITE -> {
                carrier.action = WalkingElf.Action.CARRY
                if (carrier.walkTowards(siteX - SITE_HALF - 34f, CARRY_SPEED * 0.8f, delta)) {
                    carrierStep = CarrierStep.PLACE
                    stepTimer = 0.5f
                    carrier.action = WalkingElf.Action.IDLE
                    carrier.dir = 1f
                }
            }
            CarrierStep.PLACE -> {
                stepTimer -= delta
                if (stepTimer <= 0f) {
                    if (celebrate <= 0f && built < WALL_PLANKS) {
                        built++
                        sparkles.burst(siteX, SITE_BASE + built * PLANK_HEIGHT, Palette.TABLE_TOP, 8)
                        if (built == WALL_PLANKS) finishBuilding()
                    }
                    carrierStep = CarrierStep.TO_PILE
                }
            }
        }
    }

    private fun finishBuilding() {
        celebrate = CELEBRATE_TIME
        for (elf in elves) elf.bounce = 1f
        sparkles.burst(siteX, SITE_BASE + WALL_PLANKS * PLANK_HEIGHT + 60f, Palette.GOLD, 30)
        sparkles.burst(siteX, SITE_BASE + WALL_PLANKS * PLANK_HEIGHT + 60f, Palette.SANTA_RED, 16)
        say(elves.indexOf(foreman), I18n.t("opt.elf.done"))
    }

    private fun roam(elf: WalkingElf, min: Float, max: Float, delta: Float) {
        if (elf.wait > 0f) {
            elf.wait -= delta
            elf.action = WalkingElf.Action.IDLE
            return
        }
        elf.action = WalkingElf.Action.WALK
        if (elf.walkTowards(elf.targetX, ROAM_SPEED, delta)) {
            elf.wait = MathUtils.random(1f, 3.2f)
            elf.targetX = MathUtils.random(min, max)
        }
    }

    private fun spawnDust(x: Float, y: Float, count: Int, gold: Boolean) {
        repeat(count) {
            dust += Dust(x, y, MathUtils.random(-60f, 60f), MathUtils.random(20f, 110f), MathUtils.random(0.5f, 1f), gold)
        }
    }

    private fun say(index: Int, text: String) {
        if (speaker >= 0) elves[speaker].talking = false
        speaker = index
        speech = text
        speechLines = game.wrapText(text, SPEECH_SCALE, BUBBLE_WIDTH - 28f)
        speechTimer = SPEECH_TIME
        elves[index].talking = true
    }

    override fun interceptTap(x: Float, y: Float): Boolean {
        if (x > leftWidth) return false
        for ((i, elf) in elves.withIndex()) {
            if (!elf.contains(x, y)) continue
            elf.bounce = 1f
            game.haptics.play(Haptics.Pattern.TAP)
            say(i, I18n.t(nextLine()))
            chatTimer = MathUtils.random(CHAT_MIN, CHAT_MAX)
            return true
        }
        return false
    }

    override fun onTap(button: Button) {
        for (row in rows) {
            if (button == row.down) {
                row.change(-1)
                return
            }
            if (button == row.up) {
                row.change(1)
                return
            }
        }
        when (button) {
            saveButton -> game.setScreen(SaveSlotsScreen(game, SaveSlotsScreen.Mode.SAVE))
            loadButton -> game.setScreen(SaveSlotsScreen(game, SaveSlotsScreen.Mode.LOAD))
            controlsButton -> game.setScreen(ControlsScreen(game))
            back -> onBack()
        }
    }

    override fun onBack() {
        game.setScreen(MainMenuScreen(game))
    }

    override fun drawContentShapes(shapes: ShapeRenderer) {
        val w = worldWidth
        val h = worldHeight
        Chalet.drawWall(shapes, w, h)
        shapes.color = Palette.FLOOR_WOOD
        shapes.rect(0f, 0f, w, FLOOR_TOP)
        shapes.color = Palette.LOG_LINE
        shapes.rect(0f, FLOOR_TOP - 4f, w, 6f)
        var fx = 0f
        while (fx < w) {
            shapes.rect(fx, 0f, 3f, FLOOR_TOP - 4f)
            fx += 140f
        }
        Chalet.drawGarland(shapes, 0f, panel.x - 10f, h, time)
        drawBlueprint(shapes, leftWidth * 0.2f, h * 0.66f)
        drawToolRack(shapes, leftWidth * 0.72f, h * 0.7f)

        drawPile(shapes)
        drawSite(shapes)
        drawSawhorse(shapes)

        for (elf in elves.sortedByDescending { it.y }) elf.draw(shapes)

        for (d in dust) {
            tint.set(if (d.gold) Palette.WARM_LIGHT else Palette.TABLE_TOP)
            tint.a = minOf(1f, d.life * 2f)
            shapes.color = tint
            shapes.rect(d.x - 2f, d.y - 2f, 4f, 4f)
        }
        sparkles.draw(shapes)
        drawSpeech(shapes)

        Chalet.drawBoard(shapes, panel)
        for (row in rows) {
            val c = row.card
            shapes.color = Palette.SHOP_CARD_EDGE
            Draw.roundedRect(shapes, c.x - 3f, c.y - 3f, c.width + 6f, c.height + 6f, 16f)
            shapes.color = Palette.SHOP_CARD
            Draw.roundedRect(shapes, c.x, c.y, c.width, c.height, 14f)
            val left = row.down.bounds.x + row.down.bounds.width + 8f
            val right = row.up.bounds.x - 8f
            shapes.color = Palette.SHOP_PANEL
            Draw.roundedRect(shapes, left, c.y + 10f, right - left, c.height - 20f, (c.height - 20f) / 2f)
        }
    }

    private fun drawBlueprint(shapes: ShapeRenderer, cx: Float, cy: Float) {
        val w = 150f
        val h = 110f
        shapes.color = Palette.LOG_LINE
        shapes.rect(cx - w / 2f + 4f, cy - h / 2f - 4f, w, h)
        shapes.color = Palette.SCARF_BLUE
        shapes.rect(cx - w / 2f, cy - h / 2f, w, h)
        shapes.color = Palette.SNOW
        shapes.rectLine(cx - 40f, cy - 30f, cx + 40f, cy - 30f, 2f)
        shapes.rectLine(cx - 40f, cy - 30f, cx - 40f, cy + 10f, 2f)
        shapes.rectLine(cx + 40f, cy - 30f, cx + 40f, cy + 10f, 2f)
        shapes.rectLine(cx - 50f, cy + 10f, cx, cy + 40f, 2f)
        shapes.rectLine(cx + 50f, cy + 10f, cx, cy + 40f, 2f)
        shapes.rectLine(cx - 50f, cy + 10f, cx + 50f, cy + 10f, 2f)
        shapes.rectLine(cx - 10f, cy - 30f, cx - 10f, cy - 6f, 2f)
        shapes.rectLine(cx + 6f, cy - 30f, cx + 6f, cy - 6f, 2f)
        shapes.rectLine(cx - 10f, cy - 6f, cx + 6f, cy - 6f, 2f)
        shapes.rectLine(cx - 66f, cy - 46f, cx + 66f, cy - 46f, 1.5f)
        shapes.color = Palette.SANTA_RED
        shapes.circle(cx - w / 2f + 8f, cy + h / 2f - 8f, 5f, 8)
        shapes.circle(cx + w / 2f - 8f, cy + h / 2f - 8f, 5f, 8)
    }

    private fun drawToolRack(shapes: ShapeRenderer, cx: Float, cy: Float) {
        shapes.color = Palette.TABLE_DARK
        Draw.roundedRect(shapes, cx - 90f, cy + 30f, 180f, 14f, 5f)
        shapes.color = Palette.GOLD_DARK
        for (k in 0 until 4) shapes.circle(cx - 66f + k * 44f, cy + 30f, 4f, 8)
        shapes.color = Palette.TRUNK
        shapes.rect(cx - 69f, cy - 40f, 6f, 70f)
        shapes.color = Palette.ROCK_DARK
        shapes.rect(cx - 82f, cy + 14f, 32f, 14f)
        shapes.color = Palette.SLED_RUNNER
        shapes.triangle(cx - 26f, cy + 26f, cx - 18f, cy - 46f, cx - 26f, cy - 46f)
        shapes.triangle(cx - 26f, cy + 26f, cx - 18f, cy + 26f, cx - 18f, cy - 46f)
        shapes.color = Palette.SANTA_RED
        shapes.rect(cx - 30f, cy + 12f, 16f, 18f)
        shapes.color = Palette.GOLD
        shapes.rectLine(cx + 22f, cy + 28f, cx + 22f, cy - 26f, 6f)
        shapes.color = Palette.ROCK
        shapes.rect(cx + 14f, cy - 40f, 16f, 16f)
        shapes.color = Palette.SANTA_RED
        Draw.ring(shapes, cx + 66f, cy - 2f, 14f, 22f, 18)
    }

    private fun drawPile(shapes: ShapeRenderer) {
        val x = pileX
        for (k in 0 until 6) {
            shapes.color = if (k % 2 == 0) Palette.TABLE_TOP else Palette.TABLE_FRONT
            Draw.roundedRect(shapes, x - 50f + (k % 2) * 6f, 66f + k * 13f, 100f, 12f, 4f)
        }
        shapes.color = Palette.SNOW
        Draw.roundedRect(shapes, x - 44f, 142f, 92f, 8f, 4f)
    }

    private fun drawSawhorse(shapes: ShapeRenderer) {
        val x = sawyer.x + 60f * ELF_SCALE
        val y = sawyer.y
        shapes.color = Palette.TABLE_DARK
        shapes.rectLine(x - 20f, y, x - 8f, y + 44f, 5f)
        shapes.rectLine(x + 4f, y, x - 8f, y + 44f, 5f)
        shapes.rectLine(x + 40f, y, x + 52f, y + 44f, 5f)
        shapes.rectLine(x + 64f, y, x + 52f, y + 44f, 5f)
        shapes.color = Palette.TABLE_TOP
        Draw.roundedRect(shapes, x - 20f, y + 42f, 94f, 10f, 3f)
    }

    private fun drawSite(shapes: ShapeRenderer) {
        val cx = siteX
        val base = SITE_BASE
        val left = cx - SITE_HALF
        val right = cx + SITE_HALF
        val wallTop = base + WALL_PLANKS * PLANK_HEIGHT
        val done = built >= WALL_PLANKS

        for (k in 0 until built) {
            val py = base + k * PLANK_HEIGHT
            shapes.color = if (k % 2 == 0) Palette.LOG else Palette.LOG_DARK
            shapes.rect(left, py, SITE_HALF * 2f, PLANK_HEIGHT)
            shapes.color = Palette.LOG_LINE
            shapes.rect(left, py, SITE_HALF * 2f, 2f)
            shapes.color = Palette.LOG_END
            shapes.circle(left - 3f, py + PLANK_HEIGHT / 2f, PLANK_HEIGHT / 2f, 10)
            shapes.circle(right + 3f, py + PLANK_HEIGHT / 2f, PLANK_HEIGHT / 2f, 10)
        }

        shapes.color = Palette.TABLE_DARK
        for (post in floatArrayOf(left, cx, right)) shapes.rect(post - 5f, base, 10f, wallTop - base + 6f)
        shapes.rect(left - 8f, wallTop, SITE_HALF * 2f + 16f, 10f)
        shapes.rectLine(left - 20f, wallTop + 6f, cx, wallTop + 90f, 8f)
        shapes.rectLine(right + 20f, wallTop + 6f, cx, wallTop + 90f, 8f)
        shapes.rectLine(cx, wallTop + 6f, cx, wallTop + 88f, 6f)

        if (done) {
            val glow = (MathUtils.sin(time * 6f) + 1f) / 2f
            shapes.color = Palette.SANTA_RED_DARK
            shapes.triangle(left - 26f, wallTop + 4f, right + 26f, wallTop + 4f, cx, wallTop + 96f)
            shapes.color = Palette.SNOW
            shapes.triangle(left - 6f, wallTop + 26f, right + 6f, wallTop + 26f, cx, wallTop + 100f)
            shapes.color = Palette.LOG_LINE
            shapes.rect(cx - 20f, base, 40f, 64f)
            shapes.color = Palette.SANTA_RED
            shapes.rect(cx - 16f, base, 32f, 60f)
            tint.set(Palette.WARM_GLOW)
            tint.a = 0.25f + glow * 0.2f
            shapes.color = tint
            shapes.circle(left + 46f, base + 100f, 40f, 20)
            shapes.circle(right - 46f, base + 100f, 40f, 20)
            shapes.color = Palette.WARM_LIGHT
            shapes.rect(left + 28f, base + 84f, 36f, 34f)
            shapes.rect(right - 64f, base + 84f, 36f, 34f)
            shapes.color = Palette.GOLD
            Draw.star(shapes, cx, wallTop + 112f, 16f + glow * 3f, time)
        }

        val lx = right + 18f
        shapes.color = Palette.TABLE_TOP
        shapes.rect(lx, base, 6f, wallTop - base + 30f)
        shapes.rect(lx + 34f, base, 6f, wallTop - base + 30f)
        var ry = base + 22f
        while (ry < wallTop + 20f) {
            shapes.rect(lx, ry, 40f, 5f)
            ry += 30f
        }
    }

    private fun drawSpeech(shapes: ShapeRenderer) {
        if (speaker < 0) return
        val elf = elves[speaker]
        val lines = maxOf(1, speechLines.size)
        val height = 20f + lines * LINE_HEIGHT
        val x = MathUtils.clamp(elf.x - BUBBLE_WIDTH / 2f, 12f, leftWidth - BUBBLE_WIDTH - 12f)
        bubble.set(x, elf.topY + 26f, BUBBLE_WIDTH, height)
        Chalet.drawBubble(shapes, bubble, elf.x, elf.topY + 6f)
    }

    override fun drawTexts() {
        val h = worldHeight
        game.drawText(I18n.t("options.title"), panel.x + panel.width / 2f, h - 50f, 3.2f, Palette.GOLD)

        for (row in rows) {
            val c = row.card
            val label = I18n.t(row.labelKey)
            val labelScale = fit(label, 1.5f, row.down.bounds.x - c.x - 28f)
            game.drawText(label, c.x + 18f, c.y + c.height / 2f + 10f * labelScale / 1.5f, labelScale, Palette.SHOP_INK, centered = false, shadow = false)
            val left = row.down.bounds.x + row.down.bounds.width + 8f
            val right = row.up.bounds.x - 8f
            val value = row.value()
            game.drawText(value, (left + right) / 2f, c.y + c.height / 2f, fit(value, 1.7f, right - left - 24f), Palette.GOLD_LIGHT)
        }

        if (speaker >= 0) {
            var budget = ((SPEECH_TIME - speechTimer) * CHARS_PER_SECOND).toInt()
            for ((i, line) in speechLines.withIndex()) {
                if (budget <= 0) break
                val text = if (budget >= line.length) line else line.substring(0, budget)
                budget -= line.length + 1
                game.drawText(text, bubble.x + 14f, bubble.y + bubble.height - 14f - i * LINE_HEIGHT, SPEECH_SCALE, Palette.TEXT_DARK, centered = false, shadow = false)
            }
        }
    }

    private fun fit(text: String, scale: Float, maxWidth: Float): Float {
        val width = game.textWidth(text, scale)
        return if (width <= maxWidth || width <= 0f) scale else scale * maxWidth / width
    }

    companion object {
        private const val PANEL_WIDTH = 620f
        private const val HEADER = 110f
        private const val ROW_HEIGHT = 64f
        private const val ROW_GAP = 12f
        private const val DOWN_OFFSET = 270f
        private const val FLOOR_TOP = 120f
        private const val SITE_BASE = 96f
        private const val SITE_HALF = 110f
        private const val PLANK_HEIGHT = 22f
        private const val WALL_PLANKS = 7
        private const val ELF_SCALE = 0.78f
        private const val CARRY_SPEED = 110f
        private const val ROAM_SPEED = 70f
        private const val CELEBRATE_TIME = 4f
        private const val BUBBLE_WIDTH = 250f
        private const val SPEECH_SCALE = 1f
        private const val LINE_HEIGHT = 22f
        private const val SPEECH_TIME = 3.2f
        private const val CHARS_PER_SECOND = 40f
        private const val FIRST_CHAT = 1.2f
        private const val CHAT_MIN = 2.5f
        private const val CHAT_MAX = 4.5f
        private val LINES = List(10) { "opt.elf.${it + 1}" }
    }
}
