package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.awards.AwardTracker
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.input.Action
import com.greencoolerapple.santagame.input.Touches
import com.greencoolerapple.santagame.shop.Cosmetics
import com.greencoolerapple.santagame.shop.ShopItem
import com.greencoolerapple.santagame.ui.Button
import com.greencoolerapple.santagame.ui.Chalet
import com.greencoolerapple.santagame.ui.Draw
import com.greencoolerapple.santagame.world.Santa
import com.greencoolerapple.santagame.world.Sparkles

class SkinsScreen(game: SantaGame) : MenuBase(game) {
    private enum class Character { SANTA, BOY }

    private val back = backButton()
    private val santaButton = Button("", labelScale = 2f)
    private val boyButton = Button("", labelScale = 2f)
    private val clip = Rectangle()
    private val scissor = Rectangle()
    private val panel = Rectangle()
    private val transform = Matrix4()
    private val identity = Matrix4()
    private val tint = Color()
    private val santa = Santa(0f, 0f)
    private val sparkles = Sparkles()

    private var character = Character.SANTA
    private var pop = 0f
    private var message = ""
    private var messageTimer = 0f
    private var flashItem: ShopItem? = null
    private var flashTimer = 0f

    private var scroll = 0f
    private var velocity = 0f
    private var dragPointer = -1
    private var lastY = 0f
    private var pressX = 0f
    private var pressY = 0f
    private var dragDistance = 0f

    override val buttons = listOf(back, santaButton, boyButton)

    override val showSanta: Boolean = false

    private val items: List<ShopItem>
        get() = if (character == Character.SANTA) ShopItem.suits else ShopItem.jackets

    private val cardCount: Int
        get() = items.size + 1

    private val maxScroll: Float
        get() = maxOf(0f, cardCount * (CARD_HEIGHT + GAP) - GAP - clip.height)

    private val leftWidth: Float
        get() = panel.x - 24f

    private val cardWidth: Float
        get() = clip.width - 16f

    private val stageX: Float
        get() = leftWidth / 2f

    override fun layout() {
        val w = worldWidth
        val h = worldHeight
        val delta = Gdx.graphics.deltaTime
        panel.set(w - PANEL_WIDTH - 28f, 28f, PANEL_WIDTH, h - 28f - HEADER)
        clip.set(panel.x + 18f, panel.y + 16f, panel.width - 36f, panel.height - 30f)
        back.place(150f, h - 52f, 220f, 58f)
        santaButton.label = I18n.t("skins.santa")
        boyButton.label = I18n.t("skins.boy")
        santaButton.place(stageX - 115f, 62f, 210f, 64f)
        boyButton.place(stageX + 115f, 62f, 210f, 64f)
        style(santaButton, character == Character.SANTA)
        style(boyButton, character == Character.BOY)

        santa.idle(delta)
        sparkles.update(delta)
        pop = maxOf(0f, pop - delta * 2.5f)
        messageTimer = maxOf(0f, messageTimer - delta)
        flashTimer = maxOf(0f, flashTimer - delta)

        if (game.controls.isJustPressed(Action.MOVE_LEFT)) choose(Character.SANTA)
        if (game.controls.isJustPressed(Action.MOVE_RIGHT)) choose(Character.BOY)
        updateInput(delta)
    }

    private fun style(button: Button, selected: Boolean) {
        button.fill = if (selected) Palette.BUTTON else Palette.BUTTON_DARK
        button.edge = if (selected) Palette.BUTTON_EDGE else Palette.BUTTON_DARK_EDGE
    }

    private fun choose(next: Character) {
        if (next == character) return
        character = next
        scroll = 0f
        velocity = 0f
        dragPointer = -1
        pop = 1f
        sparkles.burst(stageX, CHARACTER_BASE + 160f, Palette.SNOW, 18)
    }

    private fun updateInput(delta: Float) {
        val touches = game.touches
        if (dragPointer >= 0 && !touches.down[dragPointer]) {
            if (dragDistance < TAP_SLOP) tapAt(pressX, pressY)
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
        if (game.controls.isPressed(Action.MOVE_DOWN)) scroll += KEY_SPEED * delta
        if (game.controls.isPressed(Action.MOVE_UP)) scroll -= KEY_SPEED * delta
        scroll = MathUtils.clamp(scroll, 0f, maxScroll)
    }

    private fun cardY(index: Int): Float = clip.y + clip.height + scroll - index * (CARD_HEIGHT + GAP) - CARD_HEIGHT

    private fun tapAt(x: Float, y: Float) {
        if (!clip.contains(x, y)) return
        for (i in 0 until cardCount) {
            val cy = cardY(i)
            if (x < clip.x || x > clip.x + cardWidth || y < cy || y > cy + CARD_HEIGHT) continue
            val item = items.getOrNull(i)
            if (item == null) {
                game.haptics.play(Haptics.Pattern.TAP)
                game.setScreen(ShopScreen(game))
            } else {
                select(item)
            }
            return
        }
    }

    private fun select(item: ShopItem) {
        when {
            !game.owns(item) -> {
                game.haptics.play(Haptics.Pattern.ERROR)
                say(I18n.t("skins.locked"))
            }
            game.isEquipped(item) -> game.haptics.play(Haptics.Pattern.TAP)
            else -> {
                game.equip(item)
                game.haptics.play(Haptics.Pattern.SUCCESS)
                say(I18n.t("shop.equippedToast", item.title))
                pop = 1f
                flashItem = item
                flashTimer = FLASH_TIME
                sparkles.burst(stageX, CHARACTER_BASE + 160f, Palette.GOLD, 26)
                sparkles.burst(stageX, CHARACTER_BASE + 160f, item.color, 16)
            }
        }
    }

    private fun say(text: String) {
        message = text
        messageTimer = MESSAGE_TIME
    }

    override fun onTap(button: Button) {
        when (button) {
            back -> onBack()
            santaButton -> choose(Character.SANTA)
            boyButton -> choose(Character.BOY)
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
        Chalet.drawWreath(shapes, (270f + panel.x) / 2f, h - 58f)
        drawMirror(shapes)
        drawDecor(shapes)
        drawRug(shapes)
        drawCharacter(shapes)
        sparkles.draw(shapes)
        drawHeader(shapes)
        Chalet.drawBoard(shapes, panel)

        shapes.flush()
        game.viewport.calculateScissors(shapes.transformMatrix, clip, scissor)
        if (ScissorStack.pushScissors(scissor)) {
            for (i in 0 until cardCount) {
                val y = cardY(i)
                if (y > clip.y + clip.height || y + CARD_HEIGHT < clip.y) continue
                val item = items.getOrNull(i)
                if (item == null) drawMoreCard(shapes, clip.x, y) else drawCard(shapes, item, clip.x, y)
            }
            shapes.flush()
            ScissorStack.popScissors()
        }
        Chalet.drawScrollbar(shapes, clip, scroll, maxScroll)
    }

    private fun drawMirror(shapes: ShapeRenderer) {
        val cx = stageX
        val cy = CHARACTER_BASE + 200f
        val mw = 300f
        val mh = 400f
        shapes.color = Palette.GOLD_DARK
        shapes.ellipse(cx - mw / 2f - 14f, cy - mh / 2f - 14f, mw + 28f, mh + 28f, 48)
        shapes.color = Palette.GOLD
        shapes.ellipse(cx - mw / 2f - 8f, cy - mh / 2f - 8f, mw + 16f, mh + 16f, 48)
        shapes.color = Palette.MIRROR
        shapes.ellipse(cx - mw / 2f, cy - mh / 2f, mw, mh, 48)
        shapes.color = Palette.MIRROR_SHINE
        shapes.triangle(cx - 90f, cy + 60f, cx - 40f, cy + 160f, cx - 10f, cy + 160f)
        shapes.triangle(cx - 90f, cy + 60f, cx - 60f, cy + 60f, cx - 10f, cy + 160f)
        shapes.triangle(cx - 100f, cy + 10f, cx - 80f, cy + 40f, cx - 66f, cy + 40f)
        shapes.triangle(cx - 100f, cy + 10f, cx - 86f, cy + 10f, cx - 66f, cy + 40f)
        val top = cy + mh / 2f + 8f
        shapes.color = Palette.PINE
        leaf(shapes, cx, top, cx - 40f, top + 14f)
        leaf(shapes, cx, top, cx + 40f, top + 14f)
        shapes.color = Palette.SANTA_RED
        shapes.circle(cx - 6f, top + 6f, 7f, 12)
        shapes.circle(cx + 6f, top + 6f, 7f, 12)
        shapes.circle(cx, top + 15f, 6.5f, 12)
    }

    private fun leaf(shapes: ShapeRenderer, x1: Float, y1: Float, x2: Float, y2: Float) {
        val mx = (x1 + x2) / 2f
        val my = (y1 + y2) / 2f
        val nx = -(y2 - y1) * 0.35f
        val ny = (x2 - x1) * 0.35f
        shapes.triangle(x1, y1, mx + nx, my + ny, x2, y2)
        shapes.triangle(x1, y1, mx - nx, my - ny, x2, y2)
    }

    private fun drawDecor(shapes: ShapeRenderer) {
        val treeX = stageX - 230f
        if (treeX > 50f) {
            shapes.color = Palette.SANTA_RED_DARK
            shapes.rect(treeX - 18f, FLOOR_TOP - 30f, 36f, 26f)
            for (layer in 0 until 4) {
                val ly = FLOOR_TOP - 8f + layer * 46f
                val lw = 150f - layer * 32f
                shapes.color = if (layer % 2 == 0) Palette.PINE else Palette.PINE_DARK
                shapes.triangle(treeX - lw / 2f, ly, treeX + lw / 2f, ly, treeX, ly + 84f)
            }
            for (k in 0 until 9) {
                val ox = MathUtils.sin(k * 2.1f) * (56f - k * 5.5f)
                val oy = FLOOR_TOP + 10f + k * 19f
                val on = (k + (time * 2.5f).toInt()) % 2 == 0
                shapes.color = if (on) Chalet.BULBS[k % Chalet.BULBS.size] else Palette.GOLD_DARK
                shapes.circle(treeX + ox, oy, 5f, 8)
            }
            shapes.color = Palette.GOLD
            Draw.star(shapes, treeX, FLOOR_TOP + 230f, 16f + MathUtils.sin(time * 4f) * 2f, time * 0.6f)
        }
        val giftX = stageX + 220f
        if (giftX < leftWidth - 40f) {
            drawGift(shapes, giftX, FLOOR_TOP - 20f, 70f, 0)
            drawGift(shapes, giftX + 52f, FLOOR_TOP - 20f, 50f, 1)
            drawGift(shapes, giftX + 18f, FLOOR_TOP + 42f, 44f, 2)
        }
    }

    private fun drawGift(shapes: ShapeRenderer, x: Float, y: Float, size: Float, style: Int) {
        shapes.color = Palette.GIFT_BOXES[style % Palette.GIFT_BOXES.size]
        shapes.rect(x - size / 2f, y, size, size * 0.86f)
        shapes.color = Palette.GIFT_RIBBONS[style % Palette.GIFT_RIBBONS.size]
        shapes.rect(x - size * 0.08f, y, size * 0.16f, size * 0.86f)
        shapes.rect(x - size / 2f, y + size * 0.36f, size, size * 0.14f)
        shapes.triangle(x, y + size * 0.86f, x - size * 0.3f, y + size * 1.04f, x - size * 0.26f, y + size * 0.8f)
        shapes.triangle(x, y + size * 0.86f, x + size * 0.3f, y + size * 1.04f, x + size * 0.26f, y + size * 0.8f)
    }

    private fun drawRug(shapes: ShapeRenderer) {
        val cx = stageX
        shapes.color = Palette.RUG_BORDER
        shapes.ellipse(cx - 170f, CHARACTER_BASE - 34f, 340f, 64f, 40)
        shapes.color = Palette.RUG
        shapes.ellipse(cx - 160f, CHARACTER_BASE - 29f, 320f, 54f, 40)
        shapes.color = Palette.SHADOW
        shapes.ellipse(cx - 90f, CHARACTER_BASE - 14f, 180f, 26f, 30)
    }

    private fun drawCharacter(shapes: ShapeRenderer) {
        val bounce = MathUtils.sin(pop * MathUtils.PI)
        val sx = 1f - bounce * 0.22f
        val sy = 1f + bounce * 0.16f
        val scale = if (character == Character.SANTA) SANTA_SCALE else BOY_SCALE
        transform.idt().translate(stageX, CHARACTER_BASE, 0f).scale(scale * sx, scale * sy, 1f)
        shapes.transformMatrix = transform
        if (character == Character.SANTA) santa.draw(shapes) else drawBoy(shapes)
        shapes.transformMatrix = identity
    }

    private fun drawBoy(shapes: ShapeRenderer) {
        val b = kotlin.math.abs(MathUtils.sin(time * 3f)) * 4f
        val wave = MathUtils.sin(time * 2f) * 4f
        shapes.color = Palette.BLACK
        Draw.roundedRect(shapes, -30f, 0f, 24f, 16f, 6f)
        Draw.roundedRect(shapes, 6f, 0f, 24f, 16f, 6f)
        shapes.color = Palette.JEANS
        shapes.rect(-26f, 12f, 20f, 52f)
        shapes.rect(6f, 12f, 20f, 52f)
        shapes.color = Palette.JEANS_DARK
        shapes.rect(-8f, 40f, 16f, 24f)

        shapes.color = Cosmetics.jacket
        shapes.rectLine(-34f, 122f + b, -48f, 74f + b + wave, 18f)
        shapes.rectLine(34f, 122f + b, 48f, 74f + b - wave, 18f)
        Draw.roundedRect(shapes, -38f, 56f + b, 76f, 76f, 16f)
        tint.set(Cosmetics.jacket).mul(0.8f, 0.8f, 0.8f, 1f)
        shapes.color = tint
        shapes.rect(-38f, 56f + b, 76f, 10f)
        shapes.rect(-30f, 80f + b, 18f, 4f)
        shapes.rect(12f, 80f + b, 18f, 4f)
        shapes.color = Palette.SNOW
        shapes.rect(-2f, 60f + b, 4f, 68f)
        shapes.color = Palette.SANTA_RED
        shapes.circle(-49f, 68f + b + wave, 11f, 14)
        shapes.circle(49f, 68f + b - wave, 11f, 14)

        shapes.color = Palette.SANTA_RED
        Draw.roundedRect(shapes, -32f, 124f + b, 64f, 16f, 8f)
        shapes.rect(12f, 92f + b, 13f, 36f)
        shapes.color = Palette.SNOW
        shapes.rect(12f, 92f + b, 13f, 4f)
        shapes.rect(12f, 100f + b, 13f, 4f)

        val hy = 162f + b
        shapes.color = Palette.SKIN
        shapes.circle(-27f, hy, 7f, 12)
        shapes.circle(27f, hy, 7f, 12)
        shapes.circle(0f, hy, 28f, 26)
        shapes.color = Palette.HAIR
        for (k in 0 until 5) shapes.circle(-20f + k * 10f, hy + 17f, 7f, 10)
        shapes.color = Palette.BLUSH
        shapes.circle(-15f, hy - 8f, 5.5f, 10)
        shapes.circle(15f, hy - 8f, 5.5f, 10)
        val blink = (time % 3.4f) < 0.12f
        shapes.color = Palette.BLACK
        if (blink) {
            shapes.rect(-14f, hy + 2f, 9f, 2f)
            shapes.rect(5f, hy + 2f, 9f, 2f)
        } else {
            shapes.circle(-9f, hy + 3f, 3.6f, 10)
            shapes.circle(9f, hy + 3f, 3.6f, 10)
            shapes.color = Palette.SNOW
            shapes.circle(-8f, hy + 4.5f, 1.2f, 6)
            shapes.circle(10f, hy + 4.5f, 1.2f, 6)
        }
        shapes.color = Palette.MOUTH
        shapes.ellipse(-8f, hy - 16f, 16f, 8f, 12)
        shapes.color = Palette.SKIN
        shapes.ellipse(-9f, hy - 12f, 18f, 5f, 12)

        shapes.color = Palette.SANTA_RED
        shapes.ellipse(-30f, hy + 10f, 60f, 40f, 24)
        shapes.color = Palette.SNOW
        Draw.roundedRect(shapes, -32f, hy + 6f, 64f, 13f, 6f)
        shapes.circle(0f, hy + 52f, 9f, 14)
    }

    private fun drawHeader(shapes: ShapeRenderer) {
        if (messageTimer <= 0f) return
        val alpha = minOf(1f, messageTimer * 2f)
        val width = game.textWidth(message, 1.4f) + 40f
        tint.set(Palette.TOAST_BG)
        tint.a = 0.9f * alpha
        shapes.color = tint
        Draw.roundedRect(shapes, stageX - width / 2f, MESSAGE_Y - 22f, width, 44f, 22f)
    }

    private fun drawCard(shapes: ShapeRenderer, item: ShopItem, x: Float, y: Float) {
        val owned = game.owns(item)
        val equipped = owned && game.isEquipped(item)
        val glow = if (item == flashItem) flashTimer / FLASH_TIME else 0f
        shapes.color = when {
            glow > 0f -> Palette.GOLD_LIGHT
            equipped -> Palette.GOLD
            else -> Palette.SHOP_CARD_EDGE
        }
        val grow = glow * 4f
        Draw.roundedRect(shapes, x - 3f - grow, y - 3f - grow, cardWidth + 6f + grow * 2f, CARD_HEIGHT + 6f + grow * 2f, 18f)
        shapes.color = if (owned) Palette.SHOP_CARD else Palette.SHOP_CARD_LOCKED
        Draw.roundedRect(shapes, x, y, cardWidth, CARD_HEIGHT, 16f)
        val cx = x + 50f
        val cy = y + CARD_HEIGHT / 2f
        shapes.color = if (owned) Palette.PANEL_GREEN_DARK else Palette.LOCK_BG
        shapes.circle(cx, cy, 37f, 28)
        shapes.color = if (owned) Palette.PANEL_GREEN else Palette.SHOP_INK_SOFT
        shapes.circle(cx, cy, 33f, 28)
        if (item.category == ShopItem.Category.SUIT) drawSuitIcon(shapes, item, cx, cy) else drawJacketIcon(shapes, item, cx, cy)
        if (equipped) {
            shapes.color = Palette.SANTA_RED
            shapes.triangle(x + cardWidth - 40f, y + CARD_HEIGHT, x + cardWidth, y + CARD_HEIGHT, x + cardWidth, y + CARD_HEIGHT - 40f)
            shapes.color = Palette.GOLD
            Draw.star(shapes, x + cardWidth - 12f, y + CARD_HEIGHT - 12f, 7f, 0f)
        }
        if (!owned) {
            AwardTracker.drawLock(shapes, x + cardWidth - 46f, cy + 4f)
        }
    }

    private fun drawMoreCard(shapes: ShapeRenderer, x: Float, y: Float) {
        shapes.color = Palette.GOLD_DARK
        Draw.roundedRect(shapes, x - 3f, y - 3f, cardWidth + 6f, CARD_HEIGHT + 6f, 18f)
        shapes.color = Palette.BUTTON_SHOP
        Draw.roundedRect(shapes, x, y, cardWidth, CARD_HEIGHT, 16f)
        val bob = MathUtils.sin(time * 3f) * 3f
        Draw.goldenGift(shapes, x + 50f, y + CARD_HEIGHT / 2f + bob, 44f)
        shapes.color = Palette.GOLD_LIGHT
        val ax = x + cardWidth - 40f
        val ay = y + CARD_HEIGHT / 2f
        shapes.triangle(ax - 10f, ay + 14f, ax - 10f, ay - 14f, ax + 10f, ay)
    }

    private fun drawSuitIcon(shapes: ShapeRenderer, item: ShopItem, cx: Float, cy: Float) {
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

    private fun drawJacketIcon(shapes: ShapeRenderer, item: ShopItem, cx: Float, cy: Float) {
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
        shapes.ellipse(cx - 11f, cy + 20f, 22f, 14f, 14)
        shapes.color = Palette.SNOW
        shapes.rect(cx - 12f, cy + 20f, 24f, 4f)
    }

    override fun drawTexts() {
        val h = worldHeight
        val title = I18n.t("skins.title")
        game.drawText(title, panel.x + panel.width / 2f, h - 46f, 3f, Palette.GOLD)
        val info = I18n.t(if (character == Character.SANTA) "skins.santaInfo" else "skins.boyInfo")
        game.drawText(info, panel.x + panel.width / 2f, h - 100f, 1f, Palette.GOLD_LIGHT)

        if (messageTimer > 0f) {
            game.drawText(message, stageX, MESSAGE_Y, 1.4f, Palette.GOLD_LIGHT, alpha = minOf(1f, messageTimer * 2f))
        }

        val batch = game.batch
        batch.flush()
        game.viewport.calculateScissors(batch.transformMatrix, clip, scissor)
        if (ScissorStack.pushScissors(scissor)) {
            for (i in 0 until cardCount) {
                val y = cardY(i)
                if (y > clip.y + clip.height || y + CARD_HEIGHT < clip.y) continue
                val item = items.getOrNull(i)
                if (item == null) {
                    game.drawText(I18n.t("skins.more"), clip.x + 100f, y + CARD_HEIGHT - 22f, 1.55f, Palette.SNOW, centered = false)
                    game.drawText(I18n.t("skins.moreDesc"), clip.x + 100f, y + CARD_HEIGHT - 58f, 1f, Palette.GOLD_LIGHT, centered = false)
                } else {
                    drawCardText(item, clip.x, y)
                }
            }
            batch.flush()
            ScissorStack.popScissors()
        }
    }

    private fun drawCardText(item: ShopItem, x: Float, y: Float) {
        val owned = game.owns(item)
        val equipped = owned && game.isEquipped(item)
        game.drawText(item.title, x + 100f, y + CARD_HEIGHT - 18f, 1.6f, if (equipped) Palette.SANTA_RED_DARK else Palette.SHOP_INK, centered = false, shadow = false)
        val lines = game.wrapText(item.description, 0.95f, cardWidth - 100f - 130f)
        for ((i, line) in lines.take(2).withIndex()) {
            game.drawText(line, x + 100f, y + CARD_HEIGHT - 52f - i * 20f, 0.95f, Palette.SHOP_INK_SOFT, centered = false, shadow = false)
        }
        if (equipped) {
            game.drawText(I18n.t("shop.equipped"), x + cardWidth - 66f, y + CARD_HEIGHT / 2f, 1.2f, Palette.SANTA_RED, shadow = false)
        } else if (!owned) {
            game.drawText(item.price.toString(), x + cardWidth - 46f, y + 16f, 1f, Palette.SHOP_INK_SOFT, shadow = false)
        }
    }

    companion object {
        private const val PANEL_WIDTH = 560f
        private const val HEADER = 140f
        private const val CARD_HEIGHT = 96f
        private const val GAP = 12f
        private const val KEY_SPEED = 700f
        private const val TAP_SLOP = 14f
        private const val FLASH_TIME = 0.6f
        private const val MESSAGE_TIME = 2.4f
        private const val FLOOR_TOP = 150f
        private const val CHARACTER_BASE = 132f
        private const val SANTA_SCALE = 1.85f
        private const val BOY_SCALE = 1.6f
        private const val MESSAGE_Y = CHARACTER_BASE + 380f
    }
}
