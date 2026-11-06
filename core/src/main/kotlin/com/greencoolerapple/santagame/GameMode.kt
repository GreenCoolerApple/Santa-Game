package com.greencoolerapple.santagame

import com.greencoolerapple.santagame.i18n.I18n

enum class GameMode(private val titleKey: String) {
    CATCH_GIFTS("mode.catch"),
    STOLEN_GIFTS("mode.stolen"),
    ESCAPE_CITY("mode.escape");

    val title: String
        get() = I18n.t(titleKey)
}
