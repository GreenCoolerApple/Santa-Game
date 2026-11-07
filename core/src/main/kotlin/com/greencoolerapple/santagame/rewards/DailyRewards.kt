package com.greencoolerapple.santagame.rewards

import com.greencoolerapple.santagame.save.GameData
import java.util.TimeZone

object DailyRewards {
    const val DAYS = 15
    private const val DAY_MS = 86_400_000L

    val amounts = intArrayOf(20, 25, 30, 35, 60, 40, 45, 50, 55, 100, 60, 65, 70, 75, 250)

    fun isBig(index: Int): Boolean = (index + 1) % 5 == 0

    private fun localMillis(): Long {
        val now = System.currentTimeMillis()
        return now + TimeZone.getDefault().getOffset(now)
    }

    fun today(): Long = localMillis() / DAY_MS

    fun millisUntilTomorrow(): Long = DAY_MS - localMillis() % DAY_MS

    fun canClaim(data: GameData): Boolean = data.dailyLastDay != today()

    fun claimedInCycle(data: GameData): Int =
        if (canClaim(data) && data.dailyClaimed >= DAYS) 0 else data.dailyClaimed.coerceIn(0, DAYS)

    fun nextIndex(data: GameData): Int = claimedInCycle(data).coerceAtMost(DAYS - 1)

    fun claim(data: GameData): Int {
        if (!canClaim(data)) return 0
        val index = nextIndex(data)
        data.dailyClaimed = index + 1
        data.dailyLastDay = today()
        return amounts[index]
    }

    fun formatCountdown(millis: Long): String {
        val total = millis / 1000
        val hours = total / 3600
        val minutes = (total / 60) % 60
        val seconds = total % 60
        return hours.toString().padStart(2, '0') + ":" + minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
    }
}
