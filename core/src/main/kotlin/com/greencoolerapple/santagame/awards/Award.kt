package com.greencoolerapple.santagame.awards

import com.greencoolerapple.santagame.i18n.I18n

enum class Award(private val key: String) {
    FIRST_CATCH("firstCatch"),
    GIFT_COLLECTOR("giftCollector"),
    SLEIGH_MASTER("sleighMaster"),
    SNOW_SURVIVOR("snowSurvivor"),
    SANTAS_HELPER("santasHelper"),
    SNEAKY_START("sneakyStart"),
    CLEAN_SWEEP("cleanSweep"),
    GHOST("ghost"),
    NIGHT_SHIFT("nightShift"),
    GRINCH_MODE("grinchMode"),
    FREE("free"),
    MARATHON("marathon"),
    INVISIBLE("invisible"),
    SWEET_TOOTH("sweetTooth"),
    SUGAR_RUSH("sugarRush"),
    EXPLORER("explorer"),
    SAFE_KEEPING("safeKeeping"),
    POLYGLOT("polyglot"),
    SHOPPER("shopper");

    val bit: Long
        get() = 1L shl ordinal

    val title: String
        get() = I18n.t("award.$key")

    val description: String
        get() = I18n.t("award.$key.desc")
}
