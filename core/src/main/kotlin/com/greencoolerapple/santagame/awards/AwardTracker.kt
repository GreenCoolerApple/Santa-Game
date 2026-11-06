package com.greencoolerapple.santagame.awards

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.ui.Draw

class AwardTracker(private val game: SantaGame) {
    private val pending = ArrayDeque<Award>()
    private var current: Award? = null
    private var timer = 0f

    val unlockedCount: Int
        get() = Award.entries.count { isUnlocked(it) }

    fun isUnlocked(award: Award): Boolean = (game.data.awards and award.bit) != 0L

    fun unlock(award: Award) {
        if (isUnlocked(award)) return
        game.data.awards = game.data.awards or award.bit
        game.persist()
        pending.addLast(award)
        game.haptics.play(Haptics.Pattern.AWARD)
    }

    fun onCatchProgress(score: Int, seconds: Float) {
        if (score >= 1) unlock(Award.FIRST_CATCH)
        if (score >= 25) unlock(Award.GIFT_COLLECTOR)
        if (score >= 50) unlock(Award.SLEIGH_MASTER)
        if (seconds >= 60f) unlock(Award.SNOW_SURVIVOR)
        if (game.data.giftsCaught + score >= 500) unlock(Award.SANTAS_HELPER)
    }

    fun onHouseRobbed(houses: Int, cleanSweep: Boolean, unseen: Boolean) {
        unlock(Award.SNEAKY_START)
        if (cleanSweep) unlock(Award.CLEAN_SWEEP)
        if (unseen) unlock(Award.GHOST)
        if (houses >= 5) unlock(Award.NIGHT_SHIFT)
        if (game.data.giftsStolen >= 100) unlock(Award.GRINCH_MODE)
    }

    fun onCandy(candies: Int) {
        if (candies >= 20) unlock(Award.SWEET_TOOTH)
        if (game.data.candiesEaten + candies >= 200) unlock(Award.SUGAR_RUSH)
    }

    fun onCityEscaped(cities: Int, unchased: Boolean) {
        unlock(Award.FREE)
        if (cities >= 3) unlock(Award.MARATHON)
        if (unchased) unlock(Award.INVISIBLE)
    }

    fun checkExplorer() {
        val data = game.data
        if (data.catchGames > 0 && data.stolenGames > 0 && data.escapeGames > 0) unlock(Award.EXPLORER)
    }

    fun render(delta: Float, width: Float, height: Float) {
        if (current == null) {
            current = pending.removeFirstOrNull() ?: return
            timer = 0f
        }
        val award = current ?: return
        timer += delta
        if (timer >= TOTAL_TIME) {
            current = null
            return
        }

        val slide = when {
            timer < SLIDE_TIME -> easeOut(timer / SLIDE_TIME)
            timer > TOTAL_TIME - SLIDE_TIME -> easeOut((TOTAL_TIME - timer) / SLIDE_TIME)
            else -> 1f
        }
        val boxWidth = 560f
        val boxHeight = 96f
        val x = width / 2f - boxWidth / 2f
        val y = height - (boxHeight + 18f) * slide

        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Palette.GOLD
        Draw.roundedRect(shapes, x - 4f, y - 4f, boxWidth + 8f, boxHeight + 8f, 26f)
        shapes.color = Palette.TOAST_BG
        Draw.roundedRect(shapes, x, y, boxWidth, boxHeight, 22f)
        drawTrophy(shapes, x + 56f, y + boxHeight / 2f, 1f + kotlin.math.sin(timer * 6f) * 0.05f)
        shapes.end()

        game.batch.begin()
        game.drawText(I18n.t("awards.unlocked"), x + 110f, y + boxHeight - 18f, 1.4f, Palette.GOLD, centered = false)
        game.drawText(award.title, x + 110f, y + boxHeight - 52f, 2.1f, centered = false)
        game.batch.end()
    }

    private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)

    companion object {
        private const val SLIDE_TIME = 0.35f
        private const val TOTAL_TIME = 3.2f

        fun drawTrophy(shapes: ShapeRenderer, cx: Float, cy: Float, scale: Float) {
            shapes.color = Palette.GOLD
            shapes.rect(cx - 20f * scale, cy - 26f * scale, 40f * scale, 8f * scale)
            shapes.rect(cx - 6f * scale, cy - 20f * scale, 12f * scale, 14f * scale)
            shapes.circle(cx, cy + 6f * scale, 18f * scale, 24)
            shapes.rect(cx - 18f * scale, cy + 6f * scale, 36f * scale, 18f * scale)
            shapes.circle(cx - 22f * scale, cy + 12f * scale, 8f * scale, 14)
            shapes.circle(cx + 22f * scale, cy + 12f * scale, 8f * scale, 14)
            shapes.color = Palette.TOAST_BG
            shapes.circle(cx - 22f * scale, cy + 12f * scale, 4f * scale, 10)
            shapes.circle(cx + 22f * scale, cy + 12f * scale, 4f * scale, 10)
            shapes.color = Palette.GOLD_LIGHT
            Draw.star(shapes, cx, cy + 10f * scale, 9f * scale, 0f)
        }

        fun drawLock(shapes: ShapeRenderer, cx: Float, cy: Float) {
            shapes.color = Palette.LOCK
            Draw.ring(shapes, cx, cy + 8f, 11f, 17f, 20)
            shapes.rect(cx - 20f, cy - 24f, 40f, 26f)
            shapes.color = Palette.LOCK_BG
            shapes.circle(cx, cy - 10f, 4f, 10)
        }
    }
}
