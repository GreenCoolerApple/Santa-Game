package com.greencoolerapple.santagame.shop

import com.badlogic.gdx.graphics.Color
import com.greencoolerapple.santagame.i18n.I18n

enum class ShopItem(
    private val key: String,
    val category: Category,
    val price: Int,
    val color: Color = Color.WHITE,
    val shade: Color = Color.WHITE
) {
    SUIT_CLASSIC("suitClassic", Category.SUIT, 0, Color.valueOf("d62a2a"), Color.valueOf("a11c1c")),
    SUIT_FROST("suitFrost", Category.SUIT, 120, Color.valueOf("3a8fd6"), Color.valueOf("1f5f99")),
    SUIT_EVERGREEN("suitEvergreen", Category.SUIT, 120, Color.valueOf("2e9e5b"), Color.valueOf("1d6b3c")),
    SUIT_ROYAL("suitRoyal", Category.SUIT, 180, Color.valueOf("8e44ad"), Color.valueOf("5e2a75")),
    SUIT_GOLDEN("suitGolden", Category.SUIT, 400, Color.valueOf("d4a017"), Color.valueOf("8a6a0e")),
    JACKET_BLUE("jacketBlue", Category.JACKET, 0, Color.valueOf("3a7bd5")),
    JACKET_ORANGE("jacketOrange", Category.JACKET, 80, Color.valueOf("e67e22")),
    JACKET_GREEN("jacketGreen", Category.JACKET, 80, Color.valueOf("27ae60")),
    JACKET_PINK("jacketPink", Category.JACKET, 100, Color.valueOf("e84393")),
    PERK_EXTRA_LIFE("perkExtraLife", Category.PERK, 150, Color.valueOf("ff4d5e")),
    PERK_QUIET_BOOTS("perkQuietBoots", Category.PERK, 200, Color.valueOf("4a3a5e")),
    PERK_FAST_SNEAKERS("perkFastSneakers", Category.PERK, 200, Color.valueOf("f5c542"));

    enum class Category { SUIT, JACKET, PERK }

    val bit: Long
        get() = 1L shl ordinal

    val free: Boolean
        get() = price == 0

    val title: String
        get() = I18n.t("item.$key")

    val description: String
        get() = I18n.t("item.$key.desc")

    val indexInCategory: Int
        get() = entries.filter { it.category == category }.indexOf(this)

    companion object {
        val suits: List<ShopItem> = entries.filter { it.category == Category.SUIT }
        val jackets: List<ShopItem> = entries.filter { it.category == Category.JACKET }
    }
}
