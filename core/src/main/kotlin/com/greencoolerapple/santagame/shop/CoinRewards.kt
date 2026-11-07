package com.greencoolerapple.santagame.shop

object CoinRewards {
    private const val SECONDS_PER_COIN = 30f

    private fun forTime(seconds: Float): Int = (seconds / SECONDS_PER_COIN).toInt()

    fun catchGifts(score: Int, seconds: Float): Int = forTime(seconds) + score / 5

    fun stolenGifts(gifts: Int, seconds: Float): Int = forTime(seconds) + gifts / 2

    fun sledRace(place: Int, seconds: Float, gifts: Int): Int = forTime(seconds) + gifts / 3 + (5 - place.coerceIn(1, 4)) * 2

    fun escapeCity(score: Int, seconds: Float): Int = forTime(seconds) + score / 4
}
