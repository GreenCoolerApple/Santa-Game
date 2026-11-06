package com.greencoolerapple.santagame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.ScreenUtils
import com.greencoolerapple.santagame.SantaGame

abstract class BaseScreen(protected val game: SantaGame) : ScreenAdapter() {
    protected val worldWidth: Float
        get() = game.viewport.worldWidth

    protected val worldHeight: Float
        get() = game.viewport.worldHeight

    override fun resize(width: Int, height: Int) {
        game.viewport.update(width, height, true)
    }

    protected fun beginFrame(offsetX: Float = 0f, offsetY: Float = 0f) {
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        game.viewport.apply()
        game.camera.position.set(worldWidth / 2f + offsetX, worldHeight / 2f + offsetY, 0f)
        game.camera.update()
        game.shapes.projectionMatrix = game.camera.combined
        game.batch.projectionMatrix = game.camera.combined
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }
}
