package com.greencoolerapple.santagame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.rewards.DailyRewards
import com.greencoolerapple.santagame.world.Sparkles

class DailyPanel(private val game: SantaGame) {
    enum class Result { NONE, CLAIMED, WAIT }

    val bounds = Rectangle()
    private val claimButton = Button("", labelScale = 1.7f, sublabelColor = Palette.GOLD_LIGHT)
    private val cell = Rectangle()
    private val sparkles = Sparkles()
    private val tint = Color()

    private var time = 0f
    private var slide = 0f
    private var openIndex = -1
    private var openTimer = 0f
    private var reward = 0
    private var rewardTimer = 0f
    private var rewardX = 0f
    private var rewardY = 0f

    fun update(delta: Float, width: Float, height: Float, appear: Float) {
        time += delta
        openTimer = maxOf(0f, openTimer - delta)
        rewardTimer = maxOf(0f, rewardTimer - delta)
        sparkles.update(delta)
        slide = (1f - appear) * (PANEL_WIDTH + 60f)
        val top = height - TOP_MARGIN
        bounds.set(width - PANEL_WIDTH - RIGHT_MARGIN + slide, BOTTOM, PANEL_WIDTH, top - BOTTOM)

        val data = game.data
        val ready = DailyRewards.canClaim(data)
        claimButton.place(bounds.x + bounds.width / 2f, bounds.y + 38f, bounds.width - 28f, 56f)
        if (ready) {
            claimButton.label = I18n.t("daily.claim")
            claimButton.sublabel = null
            claimButton.labelScale = 1.9f + MathUtils.sin(time * 5f) * 0.08f
            claimButton.fill = Palette.BUTTON
            claimButton.edge = Palette.BUTTON_EDGE
        } else {
            claimButton.label = I18n.t("daily.next")
            claimButton.sublabel = DailyRewards.formatCountdown(DailyRewards.millisUntilTomorrow())
            claimButton.labelScale = 1.35f
            claimButton.fill = Palette.BUTTON_DARK
            claimButton.edge = Palette.BUTTON_DARK_EDGE
        }
    }

    private fun cellRect(index: Int): Rectangle {
        val inner = bounds.width - PADDING * 2f
        val size = (inner - GAP * (COLUMNS - 1)) / COLUMNS
        val column = index % COLUMNS
        val row = index / COLUMNS
        val top = bounds.y + bounds.height - HEADER
        cell.set(bounds.x + PADDING + column * (size + GAP), top - (row + 1) * size - row * GAP, size, size)
        return cell
    }

    fun handleTap(x: Float, y: Float): Result {
        if (!bounds.contains(x, y)) return Result.NONE
        val data = game.data
        val index = DailyRewards.nextIndex(data)
        val onToday = cellRect(index).contains(x, y)
        if (!claimButton.contains(x, y) && !onToday) return Result.NONE
        if (!DailyRewards.canClaim(data)) {
            game.haptics.play(Haptics.Pattern.TAP)
            return Result.WAIT
        }
        val amount = game.claimDailyReward()
        if (amount <= 0) return Result.NONE
        val r = cellRect(index)
        rewardX = r.x + r.width / 2f
        rewardY = r.y + r.height / 2f
        reward = amount
        rewardTimer = REWARD_TIME
        openIndex = index
        openTimer = OPEN_TIME
        sparkles.burst(rewardX, rewardY, Palette.GOLD, 26)
        sparkles.burst(rewardX, rewardY, Palette.SANTA_RED, 12)
        sparkles.burst(rewardX, rewardY, Palette.SNOW, 10)
        game.haptics.play(Haptics.Pattern.SUCCESS)
        return Result.CLAIMED
    }

    fun drawShapes(shapes: ShapeRenderer) {
        if (bounds.x >= game.viewport.worldWidth) return
        Chalet.drawBoard(shapes, bounds)
        val data = game.data
        val claimed = DailyRewards.claimedInCycle(data)
        val next = DailyRewards.nextIndex(data)
        val ready = DailyRewards.canClaim(data)
        for (i in 0 until DailyRewards.DAYS) {
            val r = cellRect(i)
            val done = i < claimed
            val today = ready && i == next
            val pulse = (MathUtils.sin(time * 5f) + 1f) / 2f
            val pop = if (i == openIndex) openTimer / OPEN_TIME else 0f
            if (today) {
                tint.set(Palette.GOLD_LIGHT)
                tint.a = 0.35f + 0.35f * pulse
                shapes.color = tint
                Draw.roundedRect(shapes, r.x - 6f, r.y - 6f, r.width + 12f, r.height + 12f, 14f)
            }
            shapes.color = when {
                today -> Palette.GOLD
                done -> Palette.PANEL_GREEN
                else -> Palette.LOG_LINE
            }
            Draw.roundedRect(shapes, r.x - 2f, r.y - 2f, r.width + 4f, r.height + 4f, 12f)
            shapes.color = when {
                today -> Palette.SHOP_CARD
                done -> Palette.PANEL_GREEN_DARK
                else -> Palette.TOAST_BG
            }
            Draw.roundedRect(shapes, r.x, r.y, r.width, r.height, 10f)

            val cx = r.x + r.width / 2f
            val cy = r.y + r.height / 2f + 6f
            val big = DailyRewards.isBig(i)
            when {
                done -> {
                    drawOpenBox(shapes, cx, cy - 4f, i, 1f + pop * 0.4f)
                    shapes.color = Palette.GOLD
                    shapes.rectLine(cx - 12f, cy - 4f, cx - 3f, cy - 13f, 5f)
                    shapes.rectLine(cx - 3f, cy - 13f, cx + 14f, cy + 6f, 5f)
                }
                today -> {
                    val bob = kotlin.math.abs(MathUtils.sin(time * 4f)) * 5f
                    val wobble = MathUtils.sin(time * 14f) * 2f
                    if (big) Draw.goldenGift(shapes, cx + wobble, cy + bob, 34f) else drawBox(shapes, cx + wobble, cy - 14f + bob, 30f, i, 1f)
                }
                else -> {
                    if (big) {
                        tint.set(Palette.GOLD)
                        tint.a = 0.5f
                        shapes.color = tint
                        Draw.star(shapes, cx, cy, 15f, 0f)
                    } else {
                        drawBox(shapes, cx, cy - 12f, 26f, i, 0.4f)
                    }
                }
            }
        }
        sparkles.draw(shapes)
    }

    private fun drawBox(shapes: ShapeRenderer, x: Float, y: Float, size: Float, index: Int, alpha: Float) {
        tint.set(Palette.GIFT_BOXES[index % Palette.GIFT_BOXES.size])
        tint.a = alpha
        shapes.color = tint
        shapes.rect(x - size / 2f, y, size, size * 0.86f)
        tint.set(Palette.GIFT_RIBBONS[index % Palette.GIFT_RIBBONS.size])
        tint.a = alpha
        shapes.color = tint
        shapes.rect(x - size * 0.09f, y, size * 0.18f, size * 0.86f)
        shapes.rect(x - size / 2f, y + size * 0.36f, size, size * 0.15f)
        shapes.triangle(x, y + size * 0.86f, x - size * 0.32f, y + size * 1.06f, x - size * 0.26f, y + size * 0.8f)
        shapes.triangle(x, y + size * 0.86f, x + size * 0.32f, y + size * 1.06f, x + size * 0.26f, y + size * 0.8f)
    }

    private fun drawOpenBox(shapes: ShapeRenderer, x: Float, y: Float, index: Int, scale: Float) {
        val size = 26f * scale
        tint.set(Palette.GIFT_BOXES[index % Palette.GIFT_BOXES.size])
        tint.a = 0.55f
        shapes.color = tint
        shapes.rect(x - size / 2f, y - size * 0.5f, size, size * 0.6f)
        shapes.triangle(x - size / 2f, y + size * 0.1f, x - size * 0.8f, y + size * 0.5f, x - size * 0.4f, y + size * 0.15f)
        shapes.triangle(x + size / 2f, y + size * 0.1f, x + size * 0.8f, y + size * 0.5f, x + size * 0.4f, y + size * 0.15f)
    }

    fun drawButtonShape(shapes: ShapeRenderer) {
        if (bounds.x >= game.viewport.worldWidth) return
        claimButton.drawShape(shapes)
    }

    fun drawTexts() {
        if (bounds.x >= game.viewport.worldWidth) return
        val data = game.data
        val cx = bounds.x + bounds.width / 2f
        val top = bounds.y + bounds.height
        game.drawText(I18n.t("daily.title"), cx, top - 24f, 1.45f, Palette.GOLD)
        val shown = if (DailyRewards.canClaim(data)) DailyRewards.nextIndex(data) + 1 else DailyRewards.claimedInCycle(data)
        game.drawText(I18n.t("daily.day", shown, DailyRewards.DAYS), cx, top - 50f, 1f, Palette.GOLD_LIGHT)

        val claimed = DailyRewards.claimedInCycle(data)
        val ready = DailyRewards.canClaim(data)
        val next = DailyRewards.nextIndex(data)
        for (i in 0 until DailyRewards.DAYS) {
            val r = cellRect(i)
            val today = ready && i == next
            val color = when {
                today -> Palette.SHOP_INK
                i < claimed -> Palette.GOLD_LIGHT
                else -> Palette.TEXT_DIM
            }
            game.drawText((i + 1).toString(), r.x + 6f, r.y + r.height - 5f, 0.8f, color, centered = false, shadow = !today)
            val amountColor = when {
                today -> Palette.SANTA_RED_DARK
                DailyRewards.isBig(i) -> Palette.GOLD
                else -> Palette.TEXT_DIM
            }
            game.drawText("+" + DailyRewards.amounts[i], r.x + r.width / 2f, r.y + 10f, 0.8f, amountColor, shadow = !today)
        }
        claimButton.drawLabel(game)

        if (rewardTimer > 0f) {
            val t = 1f - rewardTimer / REWARD_TIME
            val scale = 2.2f + MathUtils.sin(minOf(1f, t * 3f) * MathUtils.PI) * 0.6f
            game.drawText(I18n.t("daily.reward", reward), rewardX, rewardY + 30f + t * 70f, scale, Palette.GOLD_LIGHT, alpha = minOf(1f, rewardTimer * 1.5f))
        }
    }

    companion object {
        const val PANEL_WIDTH = 236f
        private const val RIGHT_MARGIN = 24f
        private const val TOP_MARGIN = 120f
        private const val BOTTOM = 100f
        private const val PADDING = 14f
        private const val GAP = 8f
        private const val COLUMNS = 3
        private const val HEADER = 66f
        private const val OPEN_TIME = 0.6f
        private const val REWARD_TIME = 1.6f
    }
}
