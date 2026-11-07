package com.greencoolerapple.santagame.shop

import com.badlogic.gdx.graphics.Color

object Cosmetics {
    val suit: Color = Color(ShopItem.SUIT_CLASSIC.color)
    val suitShade: Color = Color(ShopItem.SUIT_CLASSIC.shade)
    val jacket: Color = Color(ShopItem.JACKET_BLUE.color)
    val sled: Color = Color(ShopItem.SLED_WOOD.color)
    val sledShade: Color = Color(ShopItem.SLED_WOOD.shade)
    var sledStriped = false
        private set

    fun apply(suitItem: ShopItem, jacketItem: ShopItem, sledItem: ShopItem) {
        suit.set(suitItem.color)
        suitShade.set(suitItem.shade)
        jacket.set(jacketItem.color)
        sled.set(sledItem.color)
        sledShade.set(sledItem.shade)
        sledStriped = sledItem == ShopItem.SLED_CANDY
    }
}
