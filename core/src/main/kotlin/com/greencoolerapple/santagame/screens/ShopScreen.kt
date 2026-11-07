package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.input.Action
import com.greencoolerapple.santagame.input.Touches
import com.greencoolerapple.santagame.shop.ShopItem
import com.greencoolerapple.santagame.ui.Button
import com.greencoolerapple.santagame.ui.Draw
import com.greencoolerapple.santagame.world.Sparkles

class ShopScreen(game: SantaGame) : MenuBase(game) {
    private val back = backButton()
    private val clip = Rectangle()
    private val scissor = Rectangle()
    private val panel = Rectangle()
    private val tabRects = List(ShopItem.Tab.entries.size) { Rectangle() }
    private val sparkles = Sparkles()
    private val tint = Color()
    private val mouse = Vector2()
    private val zoom = Matrix4()
    private val identity = Matrix4()

    private var tab = ShopItem.Tab.SKINS
    private var items = ShopItem.inTab(tab)

    private var introTime = if (introSeen) SHORT_INTRO_START else 0f
    private var doorKnocked = introSeen
    private var scroll = 0f
    private var velocity = 0f
    private var dragPointer = -1
    private var lastY = 0f
    private var pressX = 0f
    private var pressY = 0f
    private var dragDistance = 0f
    private var flashItem: ShopItem? = null
    private var flashTimer = 0f
    private var hoveredItem: ShopItem? = null

    private var speech = ""
    private var speechLines = listOf<String>()
    private var typed = 0f
    private var idleTimer = 0f
    private var blinkTimer = BLINK_EVERY
    private var elfX = 0f
    private var tableTop = 0f
    private var bubbleWidth = 0f

    override val buttons = listOf(back)

    override val showSanta: Boolean = false

    private val inIntro: Boolean
        get() = introTime < INTRO_END

    private val rows: Int
        get() = items.size

    private val maxScroll: Float
        get() = maxOf(0f, rows * (CARD_HEIGHT + GAP) - GAP - clip.height)

    override fun show() {
        introSeen = true
        val greeting = if (game.data.coinsEarned == 0 && game.data.owned == 0L) "shop.elf.hello" else HELLOS[MathUtils.random(HELLOS.size - 1)]
        say(I18n.t(greeting))
    }

    private fun say(text: String) {
        speech = text
        typed = 0f
        idleTimer = 0f
        speechLines = emptyList()
    }

    override fun layout() {
        val delta = Gdx.graphics.deltaTime
        val w = worldWidth
        val h = worldHeight
        if (inIntro) {
            val before = introTime
            introTime += delta
            if (before < DOOR_START && introTime >= DOOR_START && !doorKnocked) {
                doorKnocked = true
                game.haptics.play(Haptics.Pattern.TAP)
            }
        }

        panel.set(w - PANEL_WIDTH - 28f, 28f, PANEL_WIDTH, h - 28f - TAB_ZONE)
        clip.set(panel.x + 18f, panel.y + 16f, panel.width - 36f, panel.height - 30f)
        val tabWidth = (panel.width - 20f - (tabRects.size - 1) * 8f) / tabRects.size
        for ((i, rect) in tabRects.withIndex()) {
            val lift = if (ShopItem.Tab.entries[i] == tab) 8f else 0f
            rect.set(panel.x + 10f + i * (tabWidth + 8f), panel.y + panel.height - 6f, tabWidth, TAB_HEIGHT + lift)
        }
        tableTop = h * 0.3f
        elfX = (panel.x - 24f) * 0.42f
        bubbleWidth = minOf(panel.x - 64f, 600f)

        if (inIntro) {
            back.place(-1000f, -1000f, 10f, 10f)
            return
        }
        back.place(150f, h - 52f, 220f, 58f)

        flashTimer = maxOf(0f, flashTimer - delta)
        sparkles.update(delta)
        updateElf(delta)
        updateHover()
        updateInput(delta)
    }

    private fun updateElf(delta: Float) {
        if (speechLines.isEmpty() && speech.isNotEmpty()) {
            speechLines = game.wrapText(speech, SPEECH_SCALE, bubbleWidth - 44f)
        }
        typed += delta * CHARS_PER_SECOND
        idleTimer += delta
        if (idleTimer > IDLE_TIME) say(I18n.t(IDLES[MathUtils.random(IDLES.size - 1)]))
        blinkTimer -= delta
        if (blinkTimer < -BLINK_TIME) blinkTimer = BLINK_EVERY + MathUtils.random(0f, 2f)
    }

    private val talking: Boolean
        get() = typed < speech.length

    private fun updateHover() {
        hoveredItem = null
        if (!game.isDesktop) return
        mouse.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        game.viewport.unproject(mouse)
        if (!clip.contains(mouse.x, mouse.y)) return
        hoveredItem = itemAt(mouse.x, mouse.y)
    }

    private fun updateInput(delta: Float) {
        val controls = game.controls
        if (controls.isJustPressed(Action.MOVE_LEFT)) selectTab(ShopItem.Tab.entries[(tab.ordinal + 2) % 3])
        if (controls.isJustPressed(Action.MOVE_RIGHT)) selectTab(ShopItem.Tab.entries[(tab.ordinal + 1) % 3])

        val touches = game.touches
        if (dragPointer >= 0 && !touches.down[dragPointer]) {
            if (dragDistance < TAP_SLOP) itemAt(pressX, pressY)?.let { select(it) }
            dragPointer = -1
        }
        if (dragPointer < 0) {
            for (i in 0 until Touches.MAX_POINTERS) {
                if (touches.justDown[i] && clip.contains(touches.x[i], touches.y[i])) {
                    dragPointer = i
                    lastY = touches.y[i]
                    pressX = touches.x[i]
                    pressY = touches.y[i]
                    dragDistance = 0f
                    velocity = 0f
                    break
                }
            }
        }
        if (dragPointer >= 0) {
            val y = touches.y[dragPointer]
            val moved = y - lastY
            lastY = y
            dragDistance += kotlin.math.abs(moved)
            scroll += moved
            if (delta > 0f) velocity = MathUtils.lerp(velocity, moved / delta, 0.4f)
        } else {
            scroll += velocity * delta
            velocity *= maxOf(0f, 1f - delta * 5f)
        }
        if (controls.isPressed(Action.MOVE_DOWN)) scroll += KEY_SPEED * delta
        if (controls.isPressed(Action.MOVE_UP)) scroll -= KEY_SPEED * delta
        scroll = MathUtils.clamp(scroll, 0f, maxScroll)
    }

    override fun interceptTap(x: Float, y: Float): Boolean {
        if (inIntro) {
            if (introTime < SKIP_TO) introTime = SKIP_TO
            return true
        }
        for ((i, rect) in tabRects.withIndex()) {
            if (rect.contains(x, y)) {
                selectTab(ShopItem.Tab.entries[i])
                return true
            }
        }
        return false
    }

    override fun interceptBack(): Boolean {
        if (!inIntro) return false
        if (introTime < SKIP_TO) introTime = SKIP_TO
        return true
    }

    private fun selectTab(next: ShopItem.Tab) {
        if (next == tab) return
        tab = next
        items = ShopItem.inTab(tab)
        scroll = 0f
        velocity = 0f
        dragPointer = -1
        game.haptics.play(Haptics.Pattern.TAP)
        say(I18n.t("shop.elf.tab.${tab.name.lowercase()}"))
    }

    private fun itemAt(x: Float, y: Float): ShopItem? {
        if (!clip.contains(x, y)) return null
        for ((i, item) in items.withIndex()) {
            val cy = cardY(i)
            if (x >= clip.x && x <= clip.x + cardWidth && y >= cy && y <= cy + CARD_HEIGHT) return item
        }
        return null
    }

    private fun select(item: ShopItem) {
        when {
            game.owns(item) && item.category == ShopItem.Category.PERK -> {
                game.haptics.play(Haptics.Pattern.TAP)
                say(I18n.t("shop.elf.alreadyActive"))
            }
            game.owns(item) && game.isEquipped(item) -> {
                game.haptics.play(Haptics.Pattern.TAP)
                say(I18n.t("shop.elf.alreadyEquipped"))
            }
            game.owns(item) -> {
                game.equip(item)
                game.haptics.play(Haptics.Pattern.TAP)
                say(I18n.t("shop.elf.equipped", item.title))
                flash(item)
            }
            game.buy(item) -> {
                game.haptics.play(Haptics.Pattern.SUCCESS)
                say(I18n.t("shop.elf.bought", item.title))
                flash(item)
            }
            else -> {
                game.haptics.play(Haptics.Pattern.ERROR)
                say(I18n.t("shop.elf.notEnough", item.price - game.data.coins))
            }
        }
    }

    private fun flash(item: ShopItem) {
        flashItem = item
        flashTimer = FLASH_TIME
        val index = items.indexOf(item)
        if (index >= 0) {
            val y = MathUtils.clamp(cardY(index) + CARD_HEIGHT / 2f, clip.y, clip.y + clip.height)
            sparkles.burst(clip.x + 52f, y, Palette.GOLD, 20)
            sparkles.burst(clip.x + 52f, y, Palette.SANTA_RED, 10)
        }
    }

    private val cardWidth: Float
        get() = clip.width - 16f

    private fun cardY(index: Int): Float = clip.y + clip.height + scroll - index * (CARD_HEIGHT + GAP) - CARD_HEIGHT

    private inline fun forEachVisibleCard(action: (ShopItem, Float, Float) -> Unit) {
        for ((i, item) in items.withIndex()) {
            val y = cardY(i)
            if (y > clip.y + clip.height || y + CARD_HEIGHT < clip.y) continue
            val lift = if (item == hoveredItem) 3f else 0f
            action(item, clip.x, y + lift)
        }
    }

    override fun onTap(button: Button) {
        if (button == back) onBack()
    }

    override fun onBack() {
        game.setScreen(MainMenuScreen(game))
    }

    override fun drawContentShapes(shapes: ShapeRenderer) {
        if (introTime < INTERIOR_AT) {
            drawExterior(shapes)
            return
        }
        drawInterior(shapes)
    }

    override fun drawOverlayShapes(shapes: ShapeRenderer) {
        if (!inIntro || introTime >= INTERIOR_AT) sparkles.draw(shapes)
        val fade = when {
            introTime < FADE_START -> 0f
            introTime < INTERIOR_AT -> (introTime - FADE_START) / (INTERIOR_AT - FADE_START)
            introTime < INTRO_END -> 1f - (introTime - INTERIOR_AT) / (INTRO_END - INTERIOR_AT)
            else -> 0f
        }
        if (fade > 0f) {
            tint.set(Palette.WARM_FLASH)
            tint.a = MathUtils.clamp(fade, 0f, 1f)
            shapes.color = tint
            shapes.rect(0f, 0f, worldWidth, worldHeight)
        }
    }

    private fun zoomLevel(): Float {
        val drift = 1f + minOf(introTime, ZOOM_START) * 0.08f
        if (introTime < ZOOM_START) return drift
        val t = MathUtils.clamp((introTime - ZOOM_START) / (INTERIOR_AT - ZOOM_START), 0f, 1f)
        return drift + t * t * t * 7f
    }

    private fun doorOpen(): Float = MathUtils.clamp((introTime - DOOR_START) / DOOR_TIME, 0f, 1f)

    private fun groundY(): Float = worldHeight * 0.16f

    private fun drawExterior(shapes: ShapeRenderer) {
        val w = worldWidth
        val h = worldHeight
        val cx = w / 2f
        val ground = groundY()
        val focusY = ground + 80f

        shapes.rect(0f, 0f, w, h, Palette.SKY_BOTTOM, Palette.SKY_BOTTOM, Palette.SKY_TOP, Palette.SKY_TOP)

        val z = zoomLevel()
        zoom.idt().translate(cx, focusY, 0f).scale(z, z, 1f).translate(-cx, -focusY, 0f)
        shapes.transformMatrix = zoom

        shapes.color = Palette.MOON
        shapes.circle(w * 0.16f, h * 0.8f, 40f, 32)
        shapes.color = Palette.SKY_TOP
        shapes.circle(w * 0.16f + 16f, h * 0.8f + 10f, 34f, 32)

        shapes.color = Palette.SNOW_SHADE
        shapes.ellipse(-w * 0.2f, ground - 50f, w * 0.8f, 160f, 40)
        shapes.ellipse(w * 0.45f, ground - 60f, w * 0.8f, 180f, 40)
        for ((i, x) in floatArrayOf(0.06f, 0.15f, 0.84f, 0.94f).withIndex()) {
            drawPine(shapes, w * x, ground, 0.9f + (i % 2) * 0.35f)
        }

        val left = cx - CHALET_HALF
        val wallTop = ground + 240f
        var row = 0
        var y = ground
        while (y < wallTop) {
            shapes.color = if (row % 2 == 0) Palette.LOG else Palette.LOG_DARK
            shapes.rect(left, y, CHALET_HALF * 2f, 26f)
            shapes.color = Palette.LOG_END
            shapes.circle(left - 4f, y + 13f, 13f, 12)
            shapes.circle(left + CHALET_HALF * 2f + 4f, y + 13f, 13f, 12)
            shapes.color = Palette.LOG_LINE
            shapes.rect(left, y, CHALET_HALF * 2f, 2f)
            y += 26f
            row++
        }

        shapes.color = Palette.BRICK
        shapes.rect(cx + 150f, wallTop + 60f, 54f, 130f)
        shapes.color = Palette.SNOW
        Draw.roundedRect(shapes, cx + 144f, wallTop + 184f, 66f, 16f, 8f)
        for (k in 0 until 5) {
            val p = ((introTime * 0.35f + k * 0.2f) % 1f)
            tint.set(Palette.SNOW)
            tint.a = 0.45f * (1f - p)
            shapes.color = tint
            shapes.circle(cx + 177f + MathUtils.sin(p * 6f + k) * 14f + p * 40f, wallTop + 210f + p * 150f, 12f + p * 22f, 14)
        }

        shapes.color = Palette.ROOF
        shapes.triangle(cx - CHALET_HALF - 70f, wallTop - 6f, cx + CHALET_HALF + 70f, wallTop - 6f, cx, wallTop + 200f)
        shapes.color = Palette.SNOW
        shapes.triangle(cx - CHALET_HALF - 40f, wallTop + 14f, cx + CHALET_HALF + 40f, wallTop + 14f, cx, wallTop + 212f)
        shapes.color = Palette.SNOW_SHADE
        shapes.rect(cx - CHALET_HALF - 70f, wallTop - 10f, (CHALET_HALF + 70f) * 2f, 12f)
        shapes.color = Palette.SNOW
        var sx = cx - CHALET_HALF - 70f
        var drip = 0
        while (sx <= cx + CHALET_HALF + 70f) {
            shapes.circle(sx, wallTop + 4f, 13f, 12)
            if (drip % 3 == 1) shapes.circle(sx, wallTop - 10f, 7f, 10)
            sx += 24f
            drip++
        }

        var bulb = 0
        var bx = cx - CHALET_HALF - 50f
        while (bx <= cx + CHALET_HALF + 50f) {
            val color = BULBS[bulb % BULBS.size]
            val lit = (bulb + (introTime * 4f).toInt()) % 2 == 0
            if (lit) {
                tint.set(color)
                tint.a = 0.35f
                shapes.color = tint
                shapes.circle(bx, wallTop - 22f, 12f, 12)
            }
            shapes.color = color
            shapes.ellipse(bx - 5f, wallTop - 30f, 10f, 14f, 10)
            bx += 36f
            bulb++
        }

        for (side in intArrayOf(-1, 1)) {
            val wx = cx + side * 190f
            val wy = ground + 80f
            shapes.color = Palette.WARM_GLOW
            shapes.circle(wx, wy + 45f, 90f, 24)
            shapes.color = Palette.LOG_LINE
            shapes.rect(wx - 56f, wy - 6f, 112f, 102f)
            shapes.color = Palette.WARM_LIGHT
            shapes.rect(wx - 48f, wy, 96f, 90f)
            shapes.color = Palette.LOG_LINE
            shapes.rect(wx - 3f, wy, 6f, 90f)
            shapes.rect(wx - 48f, wy + 42f, 96f, 6f)
            shapes.color = Palette.SNOW
            Draw.roundedRect(shapes, wx - 62f, wy - 14f, 124f, 14f, 7f)
            shapes.color = Palette.PINE
            Draw.roundedRect(shapes, wx - 54f, wy - 30f, 108f, 18f, 8f)
            shapes.color = Palette.SANTA_RED
            for (k in 0 until 4) shapes.circle(wx - 39f + k * 26f, wy - 21f, 5f, 8)
        }

        val doorW = 116f
        val doorH = 176f
        val doorX = cx - doorW / 2f
        shapes.color = Palette.LOG_LINE
        shapes.rect(doorX - 10f, ground, doorW + 20f, doorH + 10f)
        val open = doorOpen()
        shapes.color = Palette.WARM_FLASH
        shapes.rect(doorX, ground, doorW, doorH)
        shapes.color = Palette.WARM_LIGHT
        shapes.rect(doorX, ground, doorW, doorH * 0.35f)
        if (open > 0f) {
            tint.set(Palette.WARM_GLOW)
            tint.a = 0.5f * open
            shapes.color = tint
            shapes.triangle(doorX, ground, doorX + doorW, ground, cx + 140f * open, ground - 80f)
            shapes.triangle(doorX, ground, cx + 140f * open, ground - 80f, cx - 140f * open, ground - 80f)
        }
        val panelW = doorW * (1f - open * 0.85f)
        shapes.color = Palette.SANTA_RED_DARK
        shapes.rect(doorX, ground, panelW, doorH)
        shapes.color = Palette.SANTA_RED
        shapes.rect(doorX + 8f * (panelW / doorW), ground + 8f, panelW - 16f * (panelW / doorW), doorH - 16f)
        if (open < 0.4f) {
            val wreathX = doorX + panelW / 2f
            shapes.color = Palette.PINE_DARK
            Draw.ring(shapes, wreathX, ground + 118f, 18f, 32f, 24)
            shapes.color = Palette.PINE
            Draw.ring(shapes, wreathX, ground + 118f, 21f, 29f, 24)
            shapes.color = Palette.SANTA_RED
            shapes.triangle(wreathX, ground + 90f, wreathX - 12f, ground + 76f, wreathX - 2f, ground + 76f)
            shapes.triangle(wreathX, ground + 90f, wreathX + 12f, ground + 76f, wreathX + 2f, ground + 76f)
            shapes.circle(wreathX, ground + 90f, 6f, 10)
            shapes.color = Palette.GOLD
            shapes.circle(doorX + panelW - 14f, ground + 84f, 5f, 10)
        }

        val signY = ground + doorH + 14f
        shapes.color = Palette.GOLD_DARK
        shapes.rectLine(cx - 70f, signY + 46f, cx - 60f, wallTop - 30f, 3f)
        shapes.rectLine(cx + 70f, signY + 46f, cx + 60f, wallTop - 30f, 3f)
        shapes.color = Palette.LOG_LINE
        Draw.roundedRect(shapes, cx - 96f, signY - 4f, 192f, 56f, 12f)
        shapes.color = Palette.TABLE_TOP
        Draw.roundedRect(shapes, cx - 90f, signY, 180f, 48f, 10f)
        shapes.color = Palette.SNOW
        Draw.roundedRect(shapes, cx - 94f, signY + 40f, 188f, 12f, 6f)

        shapes.color = Palette.SNOW_SHADE
        shapes.triangle(doorX - 10f, ground, doorX + doorW + 10f, ground, cx + 160f, 0f)
        shapes.triangle(doorX - 10f, ground, cx + 160f, 0f, cx - 160f, 0f)
        shapes.color = Palette.SNOW
        shapes.rect(0f, 0f, w, ground - 1f)
        shapes.color = Palette.SNOW_SHADE
        shapes.triangle(doorX, ground - 1f, doorX + doorW, ground - 1f, cx + 150f, 0f)
        shapes.triangle(doorX, ground - 1f, cx + 150f, 0f, cx - 150f, 0f)

        for (side in intArrayOf(-1, 1)) {
            val lx = cx + side * (CHALET_HALF + 90f)
            shapes.color = Palette.WARM_GLOW
            shapes.circle(lx, ground + 150f, 46f, 20)
            shapes.color = Palette.COAL
            shapes.rect(lx - 4f, ground, 8f, 140f)
            shapes.rect(lx - 14f, ground + 136f, 28f, 6f)
            shapes.color = Palette.WARM_LIGHT
            shapes.rect(lx - 10f, ground + 142f, 20f, 22f)
            shapes.color = Palette.COAL
            shapes.triangle(lx - 15f, ground + 164f, lx + 15f, ground + 164f, lx, ground + 180f)
        }

        shapes.transformMatrix = identity
        game.scenery.drawSnow(shapes, w)
    }

    private fun drawPine(shapes: ShapeRenderer, x: Float, base: Float, scale: Float) {
        val tw = 100f * scale
        val th = 80f * scale
        shapes.color = Palette.TRUNK
        shapes.rect(x - 8f * scale, base, 16f * scale, 22f * scale)
        for (layer in 0 until 3) {
            val ly = base + 18f * scale + layer * th * 0.5f
            val lw = tw * (1f - layer * 0.22f)
            shapes.color = if (layer % 2 == 0) Palette.PINE else Palette.PINE_DARK
            shapes.triangle(x - lw / 2f, ly, x + lw / 2f, ly, x, ly + th)
            shapes.color = Palette.SNOW
            shapes.triangle(x - lw * 0.14f, ly + th * 0.72f, x + lw * 0.14f, ly + th * 0.72f, x, ly + th)
        }
    }

    private fun drawInterior(shapes: ShapeRenderer) {
        val w = worldWidth
        val h = worldHeight
        drawWall(shapes, w, h)
        drawWindow(shapes)
        drawWreath(shapes, (290f + panel.x - 40f) / 2f + 40f, h - 56f)
        drawGarland(shapes, panel.x - 10f, h)
        drawBubble(shapes)
        drawElf(shapes)
        drawTable(shapes)
        drawPanel(shapes)
        drawCoinPill(shapes)
    }

    private fun drawWall(shapes: ShapeRenderer, w: Float, h: Float) {
        var y = 0f
        var row = 0
        while (y < h) {
            shapes.color = if (row % 2 == 0) Palette.LOG else Palette.LOG_DARK
            shapes.rect(0f, y, w, 34f)
            shapes.color = Palette.LOG_LINE
            shapes.rect(0f, y, w, 3f)
            val offset = (row * 173f) % 260f
            var kx = offset
            while (kx < w) {
                shapes.circle(kx, y + 17f, 3f, 8)
                kx += 260f
            }
            y += 34f
            row++
        }
        shapes.color = Palette.FLOOR_WOOD
        shapes.rect(0f, 0f, w, 30f)
        shapes.color = Palette.LOG_LINE
        shapes.rect(0f, 28f, w, 4f)
        var px = 0f
        while (px < w) {
            shapes.rect(px, 0f, 3f, 28f)
            px += 120f
        }
    }

    private fun drawWindow(shapes: ShapeRenderer) {
        val x = 40f
        val y = tableTop + 112f
        val ww = 140f
        val wh = 130f
        if (elfX - 90f < x + ww) return
        shapes.color = Palette.LOG_LINE
        shapes.rect(x - 10f, y - 10f, ww + 20f, wh + 20f)
        shapes.color = Palette.WINDOW_NIGHT
        shapes.rect(x, y, ww, wh)
        shapes.color = Palette.MOON
        shapes.circle(x + ww * 0.7f, y + wh * 0.72f, 16f, 16)
        shapes.color = Palette.SNOW
        for (k in 0 until 9) {
            val fx = x + 10f + (k * 47f) % (ww - 20f)
            val fy = y + wh - ((time * (30f + k * 4f) + k * 37f) % wh)
            shapes.circle(fx + MathUtils.sin(time + k) * 4f, fy, 2.5f, 6)
        }
        shapes.color = Palette.SNOW
        shapes.rect(x, y, ww, 18f)
        shapes.color = Palette.LOG_LINE
        shapes.rect(x + ww / 2f - 4f, y, 8f, wh)
        shapes.rect(x, y + wh / 2f - 4f, ww, 8f)
        shapes.color = Palette.SNOW
        Draw.roundedRect(shapes, x - 18f, y - 22f, ww + 36f, 16f, 8f)
        shapes.color = Palette.SANTA_RED
        shapes.triangle(x - 10f, y + wh + 10f, x + 30f, y + wh + 10f, x - 10f, y + wh - 40f)
        shapes.triangle(x + ww + 10f, y + wh + 10f, x + ww - 30f, y + wh + 10f, x + ww + 10f, y + wh - 40f)
    }

    private fun drawWreath(shapes: ShapeRenderer, x: Float, y: Float) {
        shapes.color = Palette.PINE_DARK
        Draw.ring(shapes, x, y, 18f, 36f, 28)
        shapes.color = Palette.PINE
        for (k in 0 until 12) {
            val a = k * MathUtils.PI2 / 12f + 0.2f
            shapes.circle(x + MathUtils.cos(a) * 27f, y + MathUtils.sin(a) * 27f, 9f, 10)
        }
        for (k in 0 until 6) {
            val a = k * MathUtils.PI2 / 6f
            shapes.color = if (k % 2 == 0) Palette.SANTA_RED else Palette.GOLD
            shapes.circle(x + MathUtils.cos(a) * 27f, y + MathUtils.sin(a) * 27f, 5f, 8)
        }
        shapes.color = Palette.SANTA_RED
        shapes.triangle(x, y - 28f, x - 18f, y - 50f, x - 4f, y - 50f)
        shapes.triangle(x, y - 28f, x + 18f, y - 50f, x + 4f, y - 50f)
        shapes.circle(x, y - 28f, 7f, 10)
    }

    private fun drawGarland(shapes: ShapeRenderer, right: Float, h: Float) {
        val swags = maxOf(2, (right / 260f).toInt())
        val span = right / swags
        val top = h + 4f
        var bulb = 0
        for (k in 0 until swags) {
            val x0 = k * span
            for (j in 0 until 12) {
                val a = j / 12f
                val b = (j + 1) / 12f
                shapes.color = Palette.PINE_DARK
                shapes.rectLine(x0 + a * span, top - 120f * a * (1f - a), x0 + b * span, top - 120f * b * (1f - b), 10f)
            }
            for (j in 1 until 4) {
                val a = j / 4f
                val bx = x0 + a * span
                val by = top - 120f * a * (1f - a) - 9f
                val color = BULBS[bulb % BULBS.size]
                if ((bulb + (time * 3f).toInt()) % 3 == 0) {
                    tint.set(color)
                    tint.a = 0.3f
                    shapes.color = tint
                    shapes.circle(bx, by - 3f, 13f, 12)
                }
                shapes.color = color
                shapes.ellipse(bx - 5f, by - 11f, 10f, 14f, 10)
                bulb++
            }
        }
    }

    private fun bubbleRect(out: Rectangle): Rectangle {
        val lines = maxOf(1, speechLines.size)
        val height = 40f + lines * LINE_HEIGHT
        val bottom = tableTop + 262f
        val x = minOf(maxOf(24f, elfX - bubbleWidth * 0.35f), panel.x - 24f - bubbleWidth)
        out.set(x, bottom, bubbleWidth, height)
        return out
    }

    private val bubble = Rectangle()

    private fun drawBubble(shapes: ShapeRenderer) {
        val r = bubbleRect(bubble)
        shapes.color = Palette.BUBBLE_EDGE
        Draw.roundedRect(shapes, r.x - 4f, r.y - 4f, r.width + 8f, r.height + 8f, 22f)
        shapes.triangle(elfX - 84f, r.y - 30f, elfX - 100f, r.y + 2f, elfX - 54f, r.y + 2f)
        shapes.color = Palette.BUBBLE
        Draw.roundedRect(shapes, r.x, r.y, r.width, r.height, 18f)
        shapes.triangle(elfX - 82f, r.y - 22f, elfX - 94f, r.y + 4f, elfX - 60f, r.y + 4f)
        shapes.color = Palette.SANTA_RED
        val nameWidth = game.textWidth(I18n.t("shop.elf.name"), 1.2f) + 26f
        Draw.roundedRect(shapes, r.x + 16f, r.y + r.height - 14f, nameWidth, 30f, 15f)
    }

    private fun drawElf(shapes: ShapeRenderer) {
        val x = elfX
        val base = tableTop
        val bob = MathUtils.sin(time * 2.2f) * 3f
        val headY = base + 156f + bob
        val wave = if (talking) MathUtils.sin(time * 9f) * 12f else 0f

        shapes.color = Palette.ELF_GREEN_DARK
        shapes.ellipse(x - 78f, base - 40f, 156f, 180f + bob, 28)
        shapes.color = Palette.ELF_GREEN
        shapes.ellipse(x - 70f, base - 36f, 140f, 168f + bob, 28)
        shapes.color = Palette.BLACK
        shapes.rect(x - 66f, base + 28f, 132f, 16f)
        shapes.color = Palette.GOLD
        Draw.roundedRect(shapes, x - 14f, base + 24f, 28f, 24f, 4f)
        shapes.color = Palette.BLACK
        shapes.rect(x - 7f, base + 31f, 14f, 10f)
        shapes.color = Palette.GOLD_LIGHT
        shapes.circle(x, base + 90f + bob, 5f, 8)
        shapes.circle(x, base + 66f + bob, 5f, 8)

        shapes.color = Palette.ELF_GREEN_DARK
        shapes.rectLine(x - 62f, base + 100f + bob, x - 96f, base + 10f, 26f)
        shapes.rectLine(x + 62f, base + 100f + bob, x + 104f, base + 60f + wave, 26f)
        shapes.color = Palette.SANTA_RED
        shapes.circle(x - 96f, base + 12f, 15f, 12)
        shapes.circle(x + 106f, base + 62f + wave, 15f, 12)
        shapes.color = Palette.SKIN
        shapes.circle(x - 98f, base + 4f, 13f, 14)
        shapes.circle(x + 112f, base + 76f + wave, 13f, 14)
        shapes.circle(x + 104f, base + 86f + wave, 5f, 8)

        shapes.color = Palette.SANTA_RED
        for (k in 0 until 7) {
            val a = MathUtils.PI + k * MathUtils.PI / 6f
            shapes.circle(x + MathUtils.cos(a) * 46f, base + 104f + bob + MathUtils.sin(a) * 12f + 12f, 14f, 10)
        }
        shapes.color = Palette.SNOW
        for (k in 0 until 7) {
            val a = MathUtils.PI + k * MathUtils.PI / 6f
            shapes.circle(x + MathUtils.cos(a) * 46f, base + 104f + bob + MathUtils.sin(a) * 12f + 4f, 5f, 8)
        }

        shapes.color = Palette.SKIN
        shapes.triangle(x - 40f, headY + 4f, x - 40f, headY - 20f, x - 82f, headY + 34f)
        shapes.triangle(x + 40f, headY + 4f, x + 40f, headY - 20f, x + 82f, headY + 34f)
        shapes.color = Palette.BLUSH
        shapes.triangle(x - 44f, headY, x - 44f, headY - 10f, x - 70f, headY + 22f)
        shapes.triangle(x + 44f, headY, x + 44f, headY - 10f, x + 70f, headY + 22f)
        shapes.color = Palette.SKIN
        shapes.circle(x, headY, 46f, 30)

        shapes.color = Palette.ELF_HAIR
        for (k in 0 until 5) shapes.circle(x - 34f + k * 17f, headY + 30f, 12f, 10)

        shapes.color = Palette.BLUSH
        shapes.circle(x - 26f, headY - 12f, 9f, 12)
        shapes.circle(x + 26f, headY - 12f, 9f, 12)

        val blinking = blinkTimer < 0f
        for (side in intArrayOf(-1, 1)) {
            val ex = x + side * 17f
            val ey = headY + 6f
            if (blinking) {
                shapes.color = Palette.BLACK
                shapes.rect(ex - 7f, ey - 1.5f, 14f, 3f)
            } else {
                shapes.color = Palette.BLACK
                shapes.ellipse(ex - 6f, ey - 8f, 12f, 16f, 12)
                shapes.color = Palette.SNOW
                shapes.circle(ex + 2f, ey + 3f, 2.6f, 8)
            }
        }
        shapes.color = Palette.NOSE
        shapes.circle(x, headY - 4f, 6f, 10)

        val mouth = if (talking) kotlin.math.abs(MathUtils.sin(time * 16f)) else 0f
        shapes.color = Palette.MOUTH
        shapes.ellipse(x - 12f, headY - 26f - mouth * 8f, 24f, 8f + mouth * 12f, 14)
        if (mouth < 0.2f) {
            shapes.color = Palette.SKIN
            shapes.ellipse(x - 13f, headY - 20f, 26f, 8f, 14)
        } else {
            shapes.color = Palette.TONGUE
            shapes.ellipse(x - 6f, headY - 26f - mouth * 8f, 12f, 5f, 10)
        }

        val sway = MathUtils.sin(time * 1.8f) * 10f
        shapes.color = Palette.ELF_GREEN_DARK
        shapes.triangle(x - 50f, headY + 30f, x + 50f, headY + 30f, x + 50f + sway, headY + 98f)
        shapes.color = Palette.ELF_GREEN
        shapes.triangle(x - 44f, headY + 34f, x + 40f, headY + 34f, x + 46f + sway, headY + 92f)
        shapes.triangle(x + 50f + sway, headY + 98f, x + 40f + sway, headY + 80f, x + 82f + sway, headY + 76f)
        shapes.color = Palette.SANTA_RED
        Draw.roundedRect(shapes, x - 54f, headY + 22f, 108f, 18f, 9f)
        shapes.color = Palette.SNOW
        for (k in 0 until 6) shapes.rect(x - 46f + k * 17f, headY + 24f, 7f, 14f)
        shapes.color = Palette.GOLD
        shapes.circle(x + 86f + sway, headY + 74f, 10f, 14)
        shapes.color = Palette.GOLD_DARK
        shapes.circle(x + 86f + sway, headY + 69f, 3f, 8)
    }

    private fun drawTable(shapes: ShapeRenderer) {
        val right = panel.x - 24f
        val top = tableTop
        shapes.color = Palette.SHADOW
        shapes.rect(-10f, 0f, right + 20f, 30f)
        shapes.color = Palette.TABLE_DARK
        shapes.rect(-10f, 20f, right + 10f, top - 26f)
        shapes.color = Palette.TABLE_FRONT
        var px = 20f
        while (px + 120f < right) {
            Draw.roundedRect(shapes, px, 44f, 120f, top - 90f, 10f)
            px += 150f
        }
        shapes.color = Palette.TABLE_TOP
        Draw.roundedRect(shapes, -20f, top - 18f, right + 30f, 30f, 10f)
        shapes.color = Palette.SANTA_RED
        shapes.rect(-20f, top - 46f, right + 30f, 30f)
        shapes.color = Palette.SANTA_RED_DARK
        var sx = -10f
        while (sx < right + 10f) {
            shapes.triangle(sx - 14f, top - 46f, sx + 14f, top - 46f, sx, top - 62f)
            sx += 28f
        }
        shapes.color = Palette.GOLD
        shapes.rect(-20f, top - 20f, right + 30f, 4f)
        sx = -10f
        while (sx < right + 10f) {
            shapes.circle(sx, top - 62f, 3f, 6)
            sx += 28f
        }

        val giftX = maxOf(70f, elfX - 190f)
        drawGift(shapes, giftX, top + 12f, 64f, 0)
        drawGift(shapes, giftX + 46f, top + 12f, 46f, 1)
        drawGift(shapes, giftX + 18f, top + 74f, 40f, 2)

        val candleX = elfX + 150f
        if (candleX + 40f < right - 120f) {
            shapes.color = Palette.GOLD_DARK
            shapes.ellipse(candleX - 24f, top + 8f, 48f, 12f, 14)
            shapes.color = Palette.CANDLE
            shapes.rect(candleX - 10f, top + 12f, 20f, 56f)
            val flicker = MathUtils.sin(time * 20f) * 1.5f + MathUtils.sin(time * 7f)
            shapes.color = Palette.WARM_GLOW
            shapes.circle(candleX, top + 80f, 26f + flicker, 16)
            shapes.color = Palette.FLAME
            shapes.ellipse(candleX - 6f, top + 68f, 12f, 22f + flicker, 12)
            shapes.color = Palette.WARM_FLASH
            shapes.ellipse(candleX - 3f, top + 70f, 6f, 10f, 10)
        }

        val treeX = right - 70f
        if (treeX > elfX + 130f) {
            shapes.color = Palette.SANTA_RED_DARK
            shapes.rect(treeX - 16f, top + 8f, 32f, 22f)
            for (layer in 0 until 3) {
                val ly = top + 26f + layer * 34f
                val lw = 110f - layer * 26f
                shapes.color = if (layer % 2 == 0) Palette.PINE else Palette.PINE_DARK
                shapes.triangle(treeX - lw / 2f, ly, treeX + lw / 2f, ly, treeX, ly + 62f)
            }
            for (k in 0 until 7) {
                val ox = MathUtils.sin(k * 2.1f) * (40f - k * 4.5f)
                val oy = top + 40f + k * 13f
                val on = (k + (time * 2.5f).toInt()) % 2 == 0
                shapes.color = if (on) BULBS[k % BULBS.size] else Palette.GOLD_DARK
                shapes.circle(treeX + ox, oy, 4.5f, 8)
            }
            shapes.color = Palette.GOLD
            Draw.star(shapes, treeX, top + 136f, 13f + MathUtils.sin(time * 4f) * 1.5f, time * 0.6f)
        }
    }

    private fun drawGift(shapes: ShapeRenderer, x: Float, y: Float, size: Float, style: Int) {
        val box = Palette.GIFT_BOXES[style % Palette.GIFT_BOXES.size]
        val ribbon = Palette.GIFT_RIBBONS[style % Palette.GIFT_RIBBONS.size]
        shapes.color = box
        shapes.rect(x - size / 2f, y, size, size * 0.86f)
        shapes.color = ribbon
        shapes.rect(x - size * 0.08f, y, size * 0.16f, size * 0.86f)
        shapes.rect(x - size / 2f, y + size * 0.36f, size, size * 0.14f)
        shapes.triangle(x, y + size * 0.86f, x - size * 0.3f, y + size * 1.04f, x - size * 0.26f, y + size * 0.8f)
        shapes.triangle(x, y + size * 0.86f, x + size * 0.3f, y + size * 1.04f, x + size * 0.26f, y + size * 0.8f)
    }

    private fun drawPanel(shapes: ShapeRenderer) {
        for ((i, rect) in tabRects.withIndex()) {
            val selected = ShopItem.Tab.entries[i] == tab
            shapes.color = Palette.SHOP_PANEL_EDGE
            Draw.roundedRect(shapes, rect.x - 4f, rect.y - 10f, rect.width + 8f, rect.height + 14f, 16f)
            shapes.color = if (selected) Palette.SANTA_RED else Palette.SHOP_TAB
            Draw.roundedRect(shapes, rect.x, rect.y - 6f, rect.width, rect.height + 6f, 13f)
            if (selected) {
                shapes.color = Palette.GOLD
                shapes.rect(rect.x + 14f, rect.y + rect.height - 7f, rect.width - 28f, 3f)
                shapes.color = Palette.SNOW
                for (k in 0 until 5) {
                    shapes.circle(rect.x + 14f + k * (rect.width - 28f) / 4f, rect.y + rect.height, 6f, 8)
                }
            }
        }

        shapes.color = Palette.SHOP_PANEL_EDGE
        Draw.roundedRect(shapes, panel.x - 8f, panel.y - 8f, panel.width + 16f, panel.height + 16f, 22f)
        val stripe = 22f
        var sx = panel.x - 8f
        var k = 0
        while (sx < panel.x + panel.width + 8f) {
            shapes.color = if (k % 2 == 0) Palette.SANTA_RED else Palette.SNOW
            val sw = minOf(stripe, panel.x + panel.width + 8f - sx)
            shapes.rect(sx, panel.y + panel.height + 2f, sw, 6f)
            shapes.rect(sx, panel.y - 8f, sw, 6f)
            sx += stripe
            k++
        }
        shapes.color = Palette.SHOP_PANEL
        Draw.roundedRect(shapes, panel.x, panel.y, panel.width, panel.height, 16f)

        shapes.flush()
        game.viewport.calculateScissors(shapes.transformMatrix, clip, scissor)
        if (ScissorStack.pushScissors(scissor)) {
            forEachVisibleCard { item, x, y -> drawCard(shapes, item, x, y) }
            shapes.flush()
            ScissorStack.popScissors()
        }

        if (maxScroll > 0f) {
            val trackX = clip.x + clip.width - 7f
            shapes.color = Palette.PANEL_GREEN_DARK
            Draw.roundedRect(shapes, trackX, clip.y, 7f, clip.height, 3.5f)
            val thumb = clip.height * clip.height / (clip.height + maxScroll)
            val thumbY = clip.y + (clip.height - thumb) * (1f - scroll / maxScroll)
            shapes.color = Palette.GOLD
            Draw.roundedRect(shapes, trackX, thumbY, 7f, thumb, 3.5f)
        }
    }

    private fun drawCard(shapes: ShapeRenderer, item: ShopItem, x: Float, y: Float) {
        val owned = game.owns(item)
        val equipped = owned && game.isEquipped(item)
        val glow = if (item == flashItem) flashTimer / FLASH_TIME else 0f
        val hover = item == hoveredItem
        shapes.color = when {
            glow > 0f -> Palette.GOLD_LIGHT
            equipped -> Palette.GOLD
            hover -> Palette.GOLD_LIGHT
            else -> Palette.SHOP_CARD_EDGE
        }
        val grow = glow * 4f + if (equipped) 1f else 0f
        Draw.roundedRect(shapes, x - 3f - grow, y - 3f - grow, cardWidth + 6f + grow * 2f, CARD_HEIGHT + 6f + grow * 2f, 18f)
        shapes.color = if (hover) Palette.SHOP_CARD_HOVER else Palette.SHOP_CARD
        Draw.roundedRect(shapes, x, y, cardWidth, CARD_HEIGHT, 16f)
        shapes.color = Palette.PANEL_GREEN_DARK
        shapes.circle(x + 50f, y + CARD_HEIGHT / 2f, 37f, 28)
        shapes.color = Palette.PANEL_GREEN
        shapes.circle(x + 50f, y + CARD_HEIGHT / 2f, 33f, 28)
        drawPreview(shapes, item, x + 50f, y + CARD_HEIGHT / 2f)
        if (equipped) {
            shapes.color = Palette.SANTA_RED
            shapes.triangle(x + cardWidth - 40f, y + CARD_HEIGHT, x + cardWidth, y + CARD_HEIGHT, x + cardWidth, y + CARD_HEIGHT - 40f)
            shapes.color = Palette.GOLD
            Draw.star(shapes, x + cardWidth - 12f, y + CARD_HEIGHT - 12f, 7f, 0f)
        }
        if (!owned) {
            shapes.color = Palette.SHOP_INK
            Draw.roundedRect(shapes, x + cardWidth - 124f, y + CARD_HEIGHT / 2f - 20f, 108f, 40f, 20f)
            Draw.goldenGift(shapes, x + cardWidth - 100f, y + CARD_HEIGHT / 2f, 22f)
        }
    }

    private fun drawPreview(shapes: ShapeRenderer, item: ShopItem, cx: Float, cy: Float) {
        when (item.category) {
            ShopItem.Category.SUIT -> {
                shapes.color = item.shade
                shapes.ellipse(cx - 22f, cy - 26f, 44f, 40f, 20)
                shapes.color = item.color
                shapes.ellipse(cx - 19f, cy - 24f, 38f, 36f, 20)
                shapes.color = Palette.BLACK
                shapes.rect(cx - 19f, cy - 12f, 38f, 6f)
                shapes.color = Palette.GOLD
                shapes.rect(cx - 4f, cy - 13f, 8f, 8f)
                shapes.color = Palette.SKIN
                shapes.circle(cx, cy + 14f, 11f, 16)
                shapes.color = Palette.SNOW
                shapes.circle(cx, cy + 8f, 9f, 14)
                shapes.color = item.color
                shapes.triangle(cx - 12f, cy + 21f, cx + 12f, cy + 21f, cx + 14f, cy + 32f)
                shapes.color = Palette.SNOW
                shapes.rect(cx - 13f, cy + 19f, 26f, 5f)
                shapes.circle(cx + 15f, cy + 32f, 3.5f, 8)
            }
            ShopItem.Category.JACKET -> {
                shapes.color = item.color
                shapes.ellipse(cx - 20f, cy - 26f, 40f, 40f, 20)
                shapes.rectLine(cx - 16f, cy + 4f, cx - 24f, cy - 16f, 9f)
                shapes.rectLine(cx + 16f, cy + 4f, cx + 24f, cy - 16f, 9f)
                shapes.color = Palette.SNOW
                shapes.rect(cx - 1.5f, cy - 24f, 3f, 34f)
                shapes.color = Palette.SKIN
                shapes.circle(cx, cy + 16f, 11f, 16)
                shapes.color = Palette.SANTA_RED
                shapes.rect(cx - 13f, cy + 4f, 26f, 6f)
                shapes.color = Palette.HAIR
                shapes.circle(cx, cy + 24f, 9f, 12)
            }
            ShopItem.Category.SLED -> {
                shapes.color = Palette.SLED_RUNNER
                shapes.rect(cx - 26f, cy - 16f, 50f, 4f)
                shapes.circle(cx + 24f, cy - 10f, 6f, 10)
                shapes.rect(cx - 14f, cy - 14f, 3f, 8f)
                shapes.rect(cx + 10f, cy - 14f, 3f, 8f)
                shapes.color = item.shade
                shapes.rect(cx - 24f, cy - 8f, 46f, 20f)
                shapes.triangle(cx + 22f, cy - 8f, cx + 22f, cy + 12f, cx + 30f, cy + 14f)
                for (k in 0 until 4) {
                    shapes.color = if (item == ShopItem.SLED_CANDY && k % 2 == 1) Palette.SNOW else item.color
                    shapes.rect(cx - 22f + k * 11f, cy - 6f, 9f, 16f)
                }
                shapes.color = item.shade
                shapes.rect(cx - 24f, cy + 10f, 14f, 12f)
            }
            ShopItem.Category.PERK -> when (item) {
                ShopItem.PERK_EXTRA_LIFE -> {
                    shapes.color = item.color
                    Draw.heart(shapes, cx, cy + 8f, 50f)
                    shapes.color = Palette.SNOW
                    shapes.rect(cx - 3f, cy - 6f, 6f, 20f)
                    shapes.rect(cx - 10f, cy + 1f, 20f, 6f)
                }
                ShopItem.PERK_QUIET_BOOTS -> {
                    shapes.color = item.color
                    shapes.rect(cx - 14f, cy - 8f, 18f, 30f)
                    shapes.rect(cx - 14f, cy - 20f, 32f, 14f)
                    shapes.color = Palette.SNOW
                    shapes.rect(cx - 16f, cy + 16f, 22f, 8f)
                    shapes.color = Palette.TEXT_DIM
                    shapes.rect(cx - 14f, cy - 24f, 32f, 5f)
                }
                else -> {
                    shapes.color = item.color
                    shapes.triangle(cx + 6f, cy + 28f, cx - 14f, cy - 2f, cx + 2f, cy - 2f)
                    shapes.triangle(cx - 6f, cy - 28f, cx + 14f, cy + 2f, cx - 2f, cy + 2f)
                    shapes.rect(cx - 6f, cy - 4f, 10f, 8f)
                }
            }
        }
    }

    private fun drawCoinPill(shapes: ShapeRenderer) {
        val text = game.data.coins.toString()
        val width = game.textWidth(text, 2f) + 74f
        val x = worldWidth - 28f - width
        val y = worldHeight - 76f
        shapes.color = Palette.GOLD_DARK
        Draw.roundedRect(shapes, x - 3f, y - 3f, width + 6f, 54f, 27f)
        shapes.color = Palette.TOAST_BG
        Draw.roundedRect(shapes, x, y, width, 48f, 24f)
        Draw.goldenGift(shapes, x + 30f, y + 24f + MathUtils.sin(time * 3f) * 2f, 28f)
    }

    override fun drawTexts() {
        if (introTime < INTERIOR_AT) return
        val coins = game.data.coins.toString()
        val pillWidth = game.textWidth(coins, 2f) + 74f
        game.drawText(coins, worldWidth - 28f - pillWidth + 52f, worldHeight - 34f, 2f, Palette.GOLD_LIGHT, centered = false)
        game.drawText(I18n.t("shop.title"), (worldWidth - 28f - pillWidth + panel.x) / 2f, worldHeight - 52f, 3.2f, Palette.GOLD)

        for ((i, rect) in tabRects.withIndex()) {
            val selected = ShopItem.Tab.entries[i] == tab
            game.drawText(I18n.t(ShopItem.Tab.entries[i].key), rect.x + rect.width / 2f, rect.y + rect.height / 2f - 2f, 1.7f, if (selected) Palette.GOLD_LIGHT else Palette.TEXT)
        }

        drawSpeech()

        val batch = game.batch
        batch.flush()
        game.viewport.calculateScissors(batch.transformMatrix, clip, scissor)
        if (ScissorStack.pushScissors(scissor)) {
            forEachVisibleCard { item, x, y -> drawCardText(item, x, y) }
            batch.flush()
            ScissorStack.popScissors()
        }
    }

    private fun drawSpeech() {
        val r = bubble
        game.drawText(I18n.t("shop.elf.name"), r.x + 29f, r.y + r.height + 8f, 1.2f, Palette.SNOW, centered = false)
        var budget = typed.toInt()
        for ((i, line) in speechLines.withIndex()) {
            if (budget <= 0) break
            val shown = if (budget >= line.length) line else line.substring(0, budget)
            budget -= line.length + 1
            game.drawText(shown, r.x + 22f, r.y + r.height - 26f - i * LINE_HEIGHT, SPEECH_SCALE, Palette.TEXT_DARK, centered = false, shadow = false)
        }
    }

    private fun drawCardText(item: ShopItem, x: Float, y: Float) {
        val owned = game.owns(item)
        val equipped = owned && game.isEquipped(item)
        val textX = x + 100f
        game.drawText(item.title, textX, y + CARD_HEIGHT - 16f, 1.6f, if (equipped) Palette.SANTA_RED_DARK else Palette.SHOP_INK, centered = false, shadow = false)
        val descWidth = cardWidth - 100f - 140f
        val lines = game.wrapText(item.description, 0.95f, descWidth)
        for ((i, line) in lines.take(2).withIndex()) {
            game.drawText(line, textX, y + CARD_HEIGHT - 50f - i * 20f, 0.95f, Palette.SHOP_INK_SOFT, centered = false, shadow = false)
        }
        val status = when {
            item.category == ShopItem.Category.PERK && owned -> I18n.t("shop.active")
            equipped -> I18n.t("shop.equipped")
            owned -> I18n.t("shop.owned")
            else -> null
        }
        if (status != null) {
            game.drawText(status, x + cardWidth - 70f, y + CARD_HEIGHT / 2f, 1.25f, if (equipped) Palette.SANTA_RED else Palette.SHOP_INK_SOFT, shadow = false)
        } else {
            val affordable = game.data.coins >= item.price
            game.drawText(item.price.toString(), x + cardWidth - 56f, y + CARD_HEIGHT / 2f, 1.6f, if (affordable) Palette.GOLD_LIGHT else Palette.HEART)
        }
    }

    override fun drawOverlayTexts() {
        if (introTime >= INTERIOR_AT) return
        val z = zoomLevel()
        if (z < 3.2f) {
            val cx = worldWidth / 2f
            val focusY = groundY() + 80f
            val signY = groundY() + 176f + 14f + 24f
            val y = focusY + (signY - focusY) * z
            game.drawText("SHOP", cx, y, 2.6f * z, Palette.SANTA_RED_DARK, shadow = false)
        }
        if (!introSeenBefore) {
            val alpha = MathUtils.clamp((FADE_START - introTime) * 2f, 0f, 0.8f)
            game.drawText(I18n.t("shop.skip"), worldWidth / 2f, 34f, 1.2f, Palette.TEXT, alpha = alpha)
        }
    }

    private val introSeenBefore = introSeen

    companion object {
        private var introSeen = false

        private const val DOOR_START = 1.0f
        private const val DOOR_TIME = 0.6f
        private const val ZOOM_START = 1.3f
        private const val FADE_START = 2.2f
        private const val INTERIOR_AT = 2.6f
        private const val INTRO_END = 3.15f
        private const val SKIP_TO = 2.45f
        private const val SHORT_INTRO_START = 2.3f
        private const val CHALET_HALF = 280f

        private const val PANEL_WIDTH = 580f
        private const val TAB_ZONE = 178f
        private const val TAB_HEIGHT = 50f
        private const val CARD_HEIGHT = 96f
        private const val GAP = 12f
        private const val KEY_SPEED = 700f
        private const val TAP_SLOP = 14f
        private const val FLASH_TIME = 0.6f

        private const val SPEECH_SCALE = 1.3f
        private const val LINE_HEIGHT = 30f
        private const val CHARS_PER_SECOND = 42f
        private const val IDLE_TIME = 11f
        private const val BLINK_EVERY = 3.2f
        private const val BLINK_TIME = 0.14f

        private val HELLOS = listOf("shop.elf.helloAgain1", "shop.elf.helloAgain2", "shop.elf.helloAgain3")
        private val IDLES = listOf("shop.elf.idle1", "shop.elf.idle2", "shop.elf.idle3", "shop.elf.idle4", "shop.elf.idle5")
        private val BULBS = listOf(
            Color.valueOf("ff4d5e"),
            Color.valueOf("ffd166"),
            Color.valueOf("4fc3f7"),
            Color.valueOf("7ed957"),
            Color.valueOf("c77dff")
        )
    }
}
