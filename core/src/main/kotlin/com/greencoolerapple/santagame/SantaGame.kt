package com.greencoolerapple.santagame

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.greencoolerapple.santagame.input.Touches
import com.greencoolerapple.santagame.screens.MenuScreen
import com.greencoolerapple.santagame.world.Scenery

class SantaGame : Game() {
    lateinit var batch: SpriteBatch
        private set
    lateinit var shapes: ShapeRenderer
        private set
    lateinit var font: BitmapFont
        private set
    lateinit var scenery: Scenery
        private set

    val camera = OrthographicCamera()
    val viewport = ExtendViewport(WORLD_WIDTH, WORLD_HEIGHT, camera)

    val touches = Touches()

    private val bestScores = IntArray(GameMode.entries.size)
    private val layout = GlyphLayout()
    private lateinit var prefs: Preferences

    override fun create() {
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        font = BitmapFont().apply {
            region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            setUseIntegerPositions(false)
        }
        scenery = Scenery()
        prefs = Gdx.app.getPreferences(PREFS_NAME)
        for (mode in GameMode.entries) {
            bestScores[mode.ordinal] = prefs.getInteger(mode.prefsKey, 0)
        }
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        setScreen(MenuScreen(this))
    }

    fun bestScore(mode: GameMode): Int = bestScores[mode.ordinal]

    fun submitScore(mode: GameMode, score: Int): Boolean {
        if (score <= bestScores[mode.ordinal]) return false
        bestScores[mode.ordinal] = score
        prefs.putInteger(mode.prefsKey, score)
        prefs.flush()
        return true
    }

    fun drawText(text: String, x: Float, y: Float, scale: Float, color: Color = Palette.TEXT, centered: Boolean = true) {
        font.data.setScale(scale)
        layout.setText(font, text)
        val drawX = if (centered) x - layout.width / 2f else x
        val drawY = if (centered) y + layout.height / 2f else y
        font.color = Palette.TEXT_SHADOW
        font.draw(batch, text, drawX + scale, drawY - scale)
        font.color = color
        font.draw(batch, text, drawX, drawY)
    }

    override fun dispose() {
        screen?.dispose()
        batch.dispose()
        shapes.dispose()
        font.dispose()
    }

    companion object {
        const val WORLD_WIDTH = 1280f
        const val WORLD_HEIGHT = 720f
        const val GROUND_HEIGHT = 90f
        private const val PREFS_NAME = "santa-game"
    }
}
