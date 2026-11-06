package com.greencoolerapple.santagame.capture

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.ScreenUtils
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.SantaGame
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.ui.Draw
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Screenshots(private val game: SantaGame) {
    private var flash = 0f
    private var message = ""
    private var messageTimer = 0f
    private var shutter: Sound? = null
    private val tint = Color()
    private val nameFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT)

    fun capture() {
        val width = Gdx.graphics.backBufferWidth
        val height = Gdx.graphics.backBufferHeight
        val pixels = ScreenUtils.getFrameBufferPixels(0, 0, width, height, true)
        for (i in 3 until pixels.size step 4) pixels[i] = 255.toByte()
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        BufferUtils.copy(pixels, 0, pixmap.pixels, pixels.size)
        val file = nextFile()

        flash = 1f
        playShutter()

        Thread {
            val saved = runCatching { PixmapIO.writePNG(file, pixmap) }.isSuccess
            pixmap.dispose()
            Gdx.app.postRunnable {
                message = if (saved) I18n.t("screenshot.saved", file.name()) else I18n.t("screenshot.failed")
                messageTimer = MESSAGE_TIME
            }
        }.apply { isDaemon = true }.start()
    }

    private fun nextFile(): FileHandle {
        val pictures = Gdx.files.external("Pictures")
        val folder = if (pictures.isDirectory) pictures.child("Santa Game") else Gdx.files.external(".santa-game/screenshots")
        folder.mkdirs()
        val base = "santa-" + nameFormat.format(Date())
        var file = folder.child("$base.png")
        var index = 2
        while (file.exists()) {
            file = folder.child("$base-$index.png")
            index++
        }
        return file
    }

    private fun playShutter() {
        val sound = shutter ?: runCatching { Gdx.audio.newSound(Gdx.files.internal(SHUTTER)) }.getOrNull()?.also { shutter = it }
        sound?.play(SHUTTER_VOLUME)
    }

    fun render(delta: Float, width: Float, height: Float) {
        if (flash <= 0f && messageTimer <= 0f) return
        flash = maxOf(0f, flash - delta * 4f)
        messageTimer = maxOf(0f, messageTimer - delta)

        val shapes = game.shapes
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        if (flash > 0f) {
            tint.set(1f, 1f, 1f, flash * 0.75f)
            shapes.color = tint
            shapes.rect(0f, 0f, width, height)
        }
        val alpha = minOf(1f, messageTimer * 2f)
        if (messageTimer > 0f) {
            val textWidth = game.textWidth(message, 1.3f)
            val boxWidth = textWidth + 90f
            tint.set(Palette.TOAST_BG)
            tint.a = 0.92f * alpha
            shapes.color = tint
            Draw.roundedRect(shapes, width / 2f - boxWidth / 2f, 24f, boxWidth, 52f, 18f)
            drawCamera(shapes, width / 2f - boxWidth / 2f + 36f, 50f, alpha)
        }
        shapes.end()

        if (messageTimer > 0f) {
            game.batch.begin()
            game.drawText(message, width / 2f + 22f, 50f, 1.3f, Palette.GOLD_LIGHT, alpha = alpha)
            game.batch.end()
        }
    }

    private fun drawCamera(shapes: ShapeRenderer, cx: Float, cy: Float, alpha: Float) {
        tint.set(Palette.GOLD)
        tint.a = alpha
        shapes.color = tint
        shapes.rect(cx - 16f, cy - 11f, 32f, 22f)
        shapes.rect(cx - 7f, cy + 11f, 14f, 5f)
        tint.set(Palette.TOAST_BG)
        tint.a = alpha
        shapes.color = tint
        shapes.circle(cx, cy, 7.5f, 16)
        tint.set(Palette.GOLD_LIGHT)
        tint.a = alpha
        shapes.color = tint
        shapes.circle(cx, cy, 4f, 12)
    }

    fun dispose() {
        shutter?.dispose()
        shutter = null
    }

    companion object {
        private const val SHUTTER = "sounds/shutter.ogg"
        private const val SHUTTER_VOLUME = 0.7f
        private const val MESSAGE_TIME = 2.6f
    }
}
