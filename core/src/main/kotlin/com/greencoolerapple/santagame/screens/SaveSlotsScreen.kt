package com.greencoolerapple.santagame.screens

import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.save.SaveSystem
import com.greencoolerapple.santagame.ui.Button
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaveSlotsScreen(game: SantaGame, private val mode: Mode) : MenuBase(game) {
    enum class Mode(val title: String) { SAVE("SAVE GAME"), LOAD("LOAD GAME") }

    private val slots = List(SaveSystem.SLOT_COUNT) { i ->
        Button(
            "Slot ${i + 1}",
            fill = if (mode == Mode.SAVE) Palette.BUTTON else Palette.BUTTON_ALT,
            edge = if (mode == Mode.SAVE) Palette.BUTTON_EDGE else Palette.BUTTON_ALT_EDGE,
            labelScale = 2.2f
        )
    }
    private val back = backButton()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val states = Array(SaveSystem.SLOT_COUNT) { SaveSystem.SlotState.EMPTY }

    override val buttons = slots + back

    override fun show() {
        refresh()
    }

    private fun refresh() {
        for (i in slots.indices) {
            val info = game.saves.slotInfo(i)
            states[i] = info.state
            val data = info.data
            slots[i].sublabel = when (info.state) {
                SaveSystem.SlotState.EMPTY -> "Empty"
                SaveSystem.SlotState.DAMAGED -> "Damaged save"
                SaveSystem.SlotState.OK -> buildString {
                    if (data != null) {
                        append(dateFormat.format(Date(data.savedAt)))
                        append("   Catch ").append(data.bestCatch)
                        append("   Stolen ").append(data.bestStolen)
                        if (data.hasRun) append("   House ").append(data.runHouse)
                    }
                }
            }
        }
    }

    override fun layout() {
        for (i in slots.indices) {
            slots[i].place(worldWidth / 2f + 60f, worldHeight * (0.64f - i * 0.15f), 720f, 92f)
        }
        back.place(worldWidth / 2f + 60f, worldHeight * 0.14f, 260f, 70f)
    }

    override fun onTap(button: Button) {
        if (button == back) {
            onBack()
            return
        }
        val index = slots.indexOf(button)
        if (index < 0) return
        val name = "Slot ${index + 1}"
        when (mode) {
            Mode.SAVE -> showToast(if (game.saveToSlot(index)) "Game saved to $name" else "Could not save to $name")
            Mode.LOAD -> showToast(
                when (states[index]) {
                    SaveSystem.SlotState.EMPTY -> "$name is empty"
                    SaveSystem.SlotState.DAMAGED -> "$name is damaged"
                    SaveSystem.SlotState.OK -> if (game.loadFromSlot(index)) "$name loaded" else "Could not load $name"
                }
            )
        }
        refresh()
    }

    override fun onBack() {
        game.setScreen(OptionsScreen(game))
    }

    override fun drawTexts() {
        game.drawText(mode.title, worldWidth / 2f + 60f, worldHeight * 0.85f, 4f, Palette.GOLD)
    }
}
