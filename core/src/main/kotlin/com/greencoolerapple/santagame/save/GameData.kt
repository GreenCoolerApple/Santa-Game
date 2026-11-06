package com.greencoolerapple.santagame.save

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class GameData {
    var bestCatch = 0
    var bestStolen = 0
    var giftsCaught = 0
    var giftsStolen = 0
    var catchGames = 0
    var stolenGames = 0
    var runHouse = 0
    var runGifts = 0
    var runHouses = 0
    var savedAt = 0L
    var bestEscape = 0
    var candiesEaten = 0
    var escapeGames = 0
    var citiesEscaped = 0
    var awards = 0L
    var coins = 0
    var coinsEarned = 0
    var owned = 0L
    var suit = 0
    var jacket = 0
    var appleMeetings = 0

    val hasRun: Boolean
        get() = runHouse > 0

    fun copyFrom(other: GameData) {
        bestCatch = other.bestCatch
        bestStolen = other.bestStolen
        giftsCaught = other.giftsCaught
        giftsStolen = other.giftsStolen
        catchGames = other.catchGames
        stolenGames = other.stolenGames
        runHouse = other.runHouse
        runGifts = other.runGifts
        runHouses = other.runHouses
        savedAt = other.savedAt
        bestEscape = other.bestEscape
        candiesEaten = other.candiesEaten
        escapeGames = other.escapeGames
        citiesEscaped = other.citiesEscaped
        awards = other.awards
        coins = other.coins
        coinsEarned = other.coinsEarned
        owned = other.owned
        suit = other.suit
        jacket = other.jacket
        appleMeetings = other.appleMeetings
    }

    fun clearRun() {
        runHouse = 0
        runGifts = 0
        runHouses = 0
    }

    fun encode(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(bestCatch)
            out.writeInt(bestStolen)
            out.writeInt(giftsCaught)
            out.writeInt(giftsStolen)
            out.writeInt(catchGames)
            out.writeInt(stolenGames)
            out.writeInt(runHouse)
            out.writeInt(runGifts)
            out.writeInt(runHouses)
            out.writeLong(savedAt)
            out.writeInt(bestEscape)
            out.writeInt(candiesEaten)
            out.writeInt(escapeGames)
            out.writeInt(citiesEscaped)
            out.writeLong(awards)
            out.writeInt(coins)
            out.writeInt(coinsEarned)
            out.writeLong(owned)
            out.writeInt(suit)
            out.writeInt(jacket)
            out.writeInt(appleMeetings)
        }
        return bytes.toByteArray()
    }

    companion object {
        fun decode(input: DataInputStream): GameData = GameData().apply {
            bestCatch = input.readInt().coerceAtLeast(0)
            bestStolen = input.readInt().coerceAtLeast(0)
            giftsCaught = input.readInt().coerceAtLeast(0)
            giftsStolen = input.readInt().coerceAtLeast(0)
            catchGames = input.readInt().coerceAtLeast(0)
            stolenGames = input.readInt().coerceAtLeast(0)
            runHouse = input.readInt().coerceAtLeast(0)
            runGifts = input.readInt().coerceAtLeast(0)
            runHouses = input.readInt().coerceAtLeast(0)
            savedAt = input.readLong()
            if (input.available() >= 16) {
                bestEscape = input.readInt().coerceAtLeast(0)
                candiesEaten = input.readInt().coerceAtLeast(0)
                escapeGames = input.readInt().coerceAtLeast(0)
                citiesEscaped = input.readInt().coerceAtLeast(0)
            }
            if (input.available() >= 8) {
                awards = input.readLong()
            }
            if (input.available() >= 24) {
                coins = input.readInt().coerceAtLeast(0)
                coinsEarned = input.readInt().coerceAtLeast(0)
                owned = input.readLong()
                suit = input.readInt().coerceAtLeast(0)
                jacket = input.readInt().coerceAtLeast(0)
            }
            if (input.available() >= 4) {
                appleMeetings = input.readInt().coerceAtLeast(0)
            }
        }
    }
}
