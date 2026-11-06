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
        }
    }
}
