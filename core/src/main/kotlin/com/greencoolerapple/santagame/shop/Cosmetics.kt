package com.greencoolerapple.santagame.shop

import com.badlogic.gdx.graphics.Color

object Cosmetics {
    val suit: Color = Color(ShopItem.SUIT_CLASSIC.color)
    val suitShade: Color = Color(ShopItem.SUIT_CLASSIC.shade)
    val jacket: Color = Color(ShopItem.JACKET_BLUE.color)

    fun apply(suitItem: ShopItem, jacketItem: ShopItem) {
        suit.set(suitItem.color)
        suitShade.set(suitItem.shade)
        jacket.set(jacketItem.color)
    }
}
