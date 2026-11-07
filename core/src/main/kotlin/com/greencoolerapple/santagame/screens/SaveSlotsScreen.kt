package com.greencoolerapple.santagame.screens

import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.save.SaveSystem
import com.greencoolerapple.santagame.ui.Button
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaveSlotsScreen(game: SantaGame, private val mode: Mode) : MenuBase(game) {
    enum class Mode(val titleKey: String) { SAVE("slots.save"), LOAD("slots.load") }

    private val slots = List(SaveSystem.SLOT_COUNT) {
        Button(
            "",
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

    private fun slotName(index: Int): String = I18n.t("slot", index + 1)

    private fun refresh() {
        for (i in slots.indices) {
            val info = game.saves.slotInfo(i)
            states[i] = info.state
            val data = info.data
            slots[i].sublabel = when (info.state) {
                SaveSystem.SlotState.EMPTY -> I18n.t("slot.empty")
                SaveSystem.SlotState.DAMAGED -> I18n.t("slot.damaged")
                SaveSystem.SlotState.OK -> if (data == null) "" else buildString {
                    append(dateFormat.format(Date(data.savedAt)))
                    append("   ").append(I18n.t("slot.catch", data.bestCatch))
                    append("   ").append(I18n.t("slot.stolen", data.bestStolen))
                    append("   ").append(I18n.t("slot.escape", data.bestEscape))
                    if (data.bestSledMs > 0) append("   ").append(I18n.t("slot.sled", SledRaceScreen.formatTime(data.bestSledMs)))
                    if (data.hasRun) append("   ").append(I18n.t("slot.house", data.runHouse))
                }
            }
        }
    }

    override fun layout() {
        for (i in slots.indices) {
            slots[i].label = slotName(i)
            slots[i].place(worldWidth / 2f + 60f, worldHeight * (0.64f - i * 0.15f), 780f, 92f)
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
        val name = slotName(index)
        when (mode) {
            Mode.SAVE -> showToast(I18n.t(if (game.saveToSlot(index)) "toast.saved" else "toast.saveFailed", name))
            Mode.LOAD -> showToast(
                when (states[index]) {
                    SaveSystem.SlotState.EMPTY -> I18n.t("toast.empty", name)
                    SaveSystem.SlotState.DAMAGED -> I18n.t("toast.damaged", name)
                    SaveSystem.SlotState.OK -> I18n.t(if (game.loadFromSlot(index)) "toast.loaded" else "toast.loadFailed", name)
                }
            )
        }
        refresh()
    }

    override fun onBack() {
        game.setScreen(OptionsScreen(game))
    }

    override fun drawTexts() {
        game.drawText(I18n.t(mode.titleKey), worldWidth / 2f + 60f, worldHeight * 0.85f, 4f, Palette.GOLD)
    }
}
