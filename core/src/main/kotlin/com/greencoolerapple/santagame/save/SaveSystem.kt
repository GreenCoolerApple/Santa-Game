package com.greencoolerapple.santagame.save

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.CRC32

class SaveSystem {
    enum class SlotState { EMPTY, OK, DAMAGED }

    class SlotInfo(val state: SlotState, val data: GameData?)

    val autosaveFile: FileHandle
        get() = file("autosave.sav")

    val settingsFile: FileHandle
        get() = file("settings.sav")

    fun slotFile(index: Int): FileHandle = file("slot${index + 1}.sav")

    private fun file(name: String): FileHandle =
        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.files.external("$DESKTOP_FOLDER/$FOLDER/$name")
        } else {
            Gdx.files.local("$FOLDER/$name")
        }

    fun saveData(file: FileHandle, data: GameData): Boolean = write(file, KIND_DATA, data.encode())

    fun loadData(file: FileHandle): GameData? = read(file, KIND_DATA)?.let { input ->
        runCatching { GameData.decode(input) }.getOrNull()
    }

    fun saveSettings(settings: Settings): Boolean = write(settingsFile, KIND_SETTINGS, settings.encode())

    fun loadSettings(): Settings? = read(settingsFile, KIND_SETTINGS)?.let { input ->
        runCatching { Settings.decode(input) }.getOrNull()
    }

    fun slotInfo(index: Int): SlotInfo {
        val file = slotFile(index)
        if (!file.exists()) return SlotInfo(SlotState.EMPTY, null)
        val data = loadData(file) ?: return SlotInfo(SlotState.DAMAGED, null)
        return SlotInfo(SlotState.OK, data)
    }

    private fun write(file: FileHandle, kind: Int, payload: ByteArray): Boolean = runCatching {
        val crc = CRC32().apply { update(payload) }.value
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(MAGIC)
            out.writeShort(VERSION)
            out.writeByte(kind)
            out.writeInt(payload.size)
            out.write(payload)
            out.writeLong(crc)
        }
        val temp = file.sibling(file.name() + ".tmp")
        temp.writeBytes(bytes.toByteArray(), false)
        temp.moveTo(file)
        true
    }.getOrDefault(false)

    private fun read(file: FileHandle, kind: Int): DataInputStream? {
        if (!file.exists()) return null
        return try {
            val bytes = file.readBytes()
            val input = DataInputStream(ByteArrayInputStream(bytes))
            val valid = input.readInt() == MAGIC &&
                input.readUnsignedShort() in 1..VERSION &&
                input.readUnsignedByte() == kind
            if (!valid) return null
            val size = input.readInt()
            if (size < 0 || size > bytes.size) return null
            val payload = ByteArray(size)
            input.readFully(payload)
            val expected = input.readLong()
            val actual = CRC32().apply { update(payload) }.value
            if (expected != actual) null else DataInputStream(ByteArrayInputStream(payload))
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val SLOT_COUNT = 3
        private const val FOLDER = "saves"
        private const val DESKTOP_FOLDER = ".santa-game"
        private const val MAGIC = 0x53475356
        private const val VERSION = 1
        private const val KIND_DATA = 1
        private const val KIND_SETTINGS = 2
    }
}
