package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.input.Action
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.input.Touches
import com.greencoolerapple.santagame.shop.ShopItem
import com.greencoolerapple.santagame.ui.Button
import com.greencoolerapple.santagame.ui.Draw

class ShopScreen(game: SantaGame) : MenuBase(game) {
    private val back = backButton()
    private val clip = Rectangle()
    private val scissor = Rectangle()
    private val items = ShopItem.entries

    private var scroll = 0f
    private var velocity = 0f
    private var dragPointer = -1
    private var lastY = 0f
    private var pressX = 0f
    private var pressY = 0f
    private var dragDistance = 0f
    private var flashItem: ShopItem? = null
    private var flashTimer = 0f

    override val buttons = listOf(back)

    override val showSanta: Boolean = false

    override val toastY: Float
        get() = worldHeight * 0.165f

    private val rows: Int
        get() = (items.size + COLUMNS - 1) / COLUMNS

    private val maxScroll: Float
        get() = maxOf(0f, rows * (CARD_HEIGHT + GAP) - GAP - clip.height)

    override fun layout() {
        clip.set(0f, worldHeight * 0.2f, worldWidth, worldHeight * 0.56f)
        back.place(worldWidth / 2f, worldHeight * 0.08f, 260f, 62f)
        val delta = Gdx.graphics.deltaTime
        flashTimer = maxOf(0f, flashTimer - delta)
        updateInput(delta)
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

    private fun tapAt(x: Float, y: Float) {
        for ((i, item) in items.withIndex()) {
            val cx = cardX(i % COLUMNS)
            val cy = cardY(i / COLUMNS)
            if (x >= cx && x <= cx + CARD_WIDTH && y >= cy && y <= cy + CARD_HEIGHT) {
                select(item)
                return
            }
        }
    }

    private fun select(item: ShopItem) {
        when {
            game.owns(item) && item.category == ShopItem.Category.PERK -> {
                game.haptics.play(Haptics.Pattern.TAP)
                showToast(I18n.t("shop.alreadyActive"))
            }
            game.owns(item) && game.isEquipped(item) -> game.haptics.play(Haptics.Pattern.TAP)
            game.owns(item) -> {
                game.equip(item)
                game.haptics.play(Haptics.Pattern.TAP)
                showToast(I18n.t("shop.equippedToast", item.title))
                flash(item)
            }
            game.buy(item) -> {
                game.haptics.play(Haptics.Pattern.SUCCESS)
                showToast(I18n.t("shop.bought", item.title))
                flash(item)
            }
            else -> {
                game.haptics.play(Haptics.Pattern.ERROR)
                showToast(I18n.t("shop.notEnough"))
            }
        }
    }

    private fun flash(item: ShopItem) {
        flashItem = item
        flashTimer = FLASH_TIME
    }

    private fun cardX(column: Int): Float {
        val total = COLUMNS * CARD_WIDTH + (COLUMNS - 1) * GAP
        return worldWidth / 2f - total / 2f + column * (CARD_WIDTH + GAP)
    }

    private fun cardY(row: Int): Float = clip.y + clip.height + scroll - row * (CARD_HEIGHT + GAP) - CARD_HEIGHT

    private inline fun forEachVisibleCard(action: (ShopItem, Float, Float) -> Unit) {
        for ((i, item) in items.withIndex()) {
            val x = cardX(i % COLUMNS)
            val y = cardY(i / COLUMNS)
            if (y > clip.y + clip.height || y + CARD_HEIGHT < clip.y) continue
            action(item, x, y)
        }
    }

    override fun drawContentShapes(shapes: ShapeRenderer) {
        shapes.color = Palette.OVERLAY
        shapes.rect(0f, 0f, worldWidth, worldHeight)

        val coinsText = game.data.coins.toString()
        val coinsWidth = game.textWidth(coinsText, 2.4f)
        Draw.goldenGift(shapes, worldWidth / 2f - coinsWidth / 2f - 34f, worldHeight * 0.8f - 4f, 34f)

        shapes.flush()
        game.viewport.calculateScissors(shapes.transformMatrix, clip, scissor)
        if (ScissorStack.pushScissors(scissor)) {
            forEachVisibleCard { item, x, y ->
                val owned = game.owns(item)
                val equipped = owned && game.isEquipped(item)
                val glow = if (item == flashItem) flashTimer / FLASH_TIME else 0f
                shapes.color = when {
                    glow > 0f -> Palette.GOLD_LIGHT
                    equipped -> Palette.GOLD
                    owned -> Palette.BUTTON_ALT
                    else -> Palette.CARD_EDGE
                }
                Draw.roundedRect(shapes, x - 3f - glow * 4f, y - 3f - glow * 4f, CARD_WIDTH + 6f + glow * 8f, CARD_HEIGHT + 6f + glow * 8f, 22f)
                shapes.color = if (equipped) Palette.CARD_UNLOCKED else Palette.CARD
                Draw.roundedRect(shapes, x, y, CARD_WIDTH, CARD_HEIGHT, 20f)
                drawPreview(shapes, item, x + 52f, y + CARD_HEIGHT / 2f)
                if (!owned) Draw.goldenGift(shapes, x + CARD_WIDTH - 100f, y + CARD_HEIGHT - 30f, 22f)
            }
            shapes.flush()
            ScissorStack.popScissors()
        }

        if (maxScroll > 0f) {
            val trackX = cardX(COLUMNS - 1) + CARD_WIDTH + 18f
            shapes.color = Palette.JOYSTICK_BASE
            shapes.rect(trackX, clip.y, 6f, clip.height)
            val thumb = clip.height * clip.height / (clip.height + maxScroll)
            val thumbY = clip.y + (clip.height - thumb) * (1f - scroll / maxScroll)
            shapes.color = Palette.GOLD
            shapes.rect(trackX, thumbY, 6f, thumb)
        }
    }

    private fun drawPreview(shapes: ShapeRenderer, item: ShopItem, cx: Float, cy: Float) {
        when (item.category) {
            ShopItem.Category.SUIT -> {
                shapes.color = Palette.SNOW
                shapes.circle(cx, cy, 28f, 24)
                shapes.color = item.color
                shapes.circle(cx, cy, 23f, 24)
                shapes.color = Palette.SNOW
                shapes.circle(cx, cy, 13f, 18)
                shapes.color = item.shade
                shapes.circle(cx, cy, 9f, 16)
                shapes.color = Palette.SNOW
                shapes.circle(cx - 9f, cy - 8f, 5f, 10)
            }
            ShopItem.Category.JACKET -> {
                shapes.color = Palette.SKIN
                shapes.circle(cx - 18f, cy + 14f, 6f, 10)
                shapes.circle(cx + 18f, cy - 14f, 6f, 10)
                shapes.color = item.color
                shapes.circle(cx, cy, 22f, 24)
                shapes.color = Palette.SNOW
                shapes.circle(cx, cy, 12f, 18)
                shapes.color = Palette.SANTA_RED
                shapes.circle(cx, cy, 9f, 16)
                shapes.color = Palette.SNOW
                shapes.circle(cx - 8f, cy - 8f, 4.5f, 10)
            }
            ShopItem.Category.PERK -> when (item) {
                ShopItem.PERK_EXTRA_LIFE -> {
                    shapes.color = item.color
                    Draw.heart(shapes, cx, cy + 8f, 54f)
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

    override fun drawTexts() {
        game.drawText(I18n.t("shop.title"), worldWidth / 2f, worldHeight * 0.9f, 4.2f, Palette.GOLD)
        val coinsText = game.data.coins.toString()
        game.drawText(coinsText, worldWidth / 2f + 10f, worldHeight * 0.8f, 2.4f, Palette.GOLD_LIGHT)

        val batch = game.batch
        batch.flush()
        game.viewport.calculateScissors(batch.transformMatrix, clip, scissor)
        if (ScissorStack.pushScissors(scissor)) {
            forEachVisibleCard { item, x, y ->
                val owned = game.owns(item)
                val equipped = owned && game.isEquipped(item)
                game.drawText(item.title, x + 98f, y + CARD_HEIGHT - 16f, 1.75f, if (equipped) Palette.GOLD else Palette.TEXT, centered = false)
                game.drawText(item.description, x + 98f, y + CARD_HEIGHT - 52f, 1f, Palette.TEXT_DIM, centered = false)
                val status = when {
                    item.category == ShopItem.Category.PERK && owned -> I18n.t("shop.active")
                    equipped -> I18n.t("shop.equipped")
                    owned -> I18n.t("shop.owned")
                    else -> null
                }
                if (status != null) {
                    game.drawText(status, x + CARD_WIDTH - 70f, y + CARD_HEIGHT - 28f, 1.25f, if (equipped) Palette.GOLD else Palette.TEXT_DIM)
                } else {
                    val affordable = game.data.coins >= item.price
                    game.drawText(item.price.toString(), x + CARD_WIDTH - 50f, y + CARD_HEIGHT - 28f, 1.7f, if (affordable) Palette.GOLD_LIGHT else Palette.HEART)
                }
            }
            batch.flush()
            ScissorStack.popScissors()
        }
    }

    override fun onTap(button: Button) {
        if (button == back) onBack()
    }

    override fun onBack() {
        game.setScreen(MainMenuScreen(game))
    }

    companion object {
        private const val COLUMNS = 2
        private const val CARD_WIDTH = 560f
        private const val CARD_HEIGHT = 92f
        private const val GAP = 18f
        private const val KEY_SPEED = 700f
        private const val TAP_SLOP = 14f
        private const val FLASH_TIME = 0.6f
    }
}
