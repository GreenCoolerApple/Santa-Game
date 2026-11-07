package com.greencoolerapple.santagame

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.Application
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Pixmap
import com.greencoolerapple.santagame.capture.Screenshots
import com.greencoolerapple.santagame.input.Action
import com.greencoolerapple.santagame.input.Controls
import com.greencoolerapple.santagame.audio.MusicPlayer
import com.greencoolerapple.santagame.awards.Award
import com.greencoolerapple.santagame.awards.AwardTracker
import com.greencoolerapple.santagame.haptics.Haptics
import com.greencoolerapple.santagame.i18n.I18n
import com.greencoolerapple.santagame.input.Touches
import com.greencoolerapple.santagame.save.GameData
import com.greencoolerapple.santagame.save.SaveSystem
import com.greencoolerapple.santagame.save.Settings
import com.greencoolerapple.santagame.screens.CatchGiftsScreen
import com.greencoolerapple.santagame.screens.EscapeCityScreen
import com.greencoolerapple.santagame.screens.IntroScreen
import com.greencoolerapple.santagame.screens.MenuBase
import com.greencoolerapple.santagame.screens.PlayScreen
import com.greencoolerapple.santagame.screens.ShopScreen
import com.greencoolerapple.santagame.screens.SledRaceScreen
import com.greencoolerapple.santagame.screens.StolenGiftsScreen
import com.greencoolerapple.santagame.rewards.DailyRewards
import com.greencoolerapple.santagame.shop.Cosmetics
import com.greencoolerapple.santagame.shop.ShopItem
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
    val saves = SaveSystem()
    val awards = AwardTracker(this)
    val haptics = Haptics { settings }
    val music = MusicPlayer { settings }
    val controls = Controls { settings }

    val isDesktop: Boolean
        get() = Gdx.app.type == Application.ApplicationType.Desktop

    val isMobile: Boolean
        get() = Gdx.app.type == Application.ApplicationType.Android || Gdx.app.type == Application.ApplicationType.iOS

    private val screenshots = Screenshots(this)
    private var cursor: Cursor? = null

    var data = GameData()
        private set
    var settings = Settings()
        private set

    var appleRolled = false

    private val layout = GlyphLayout()
    private val textColor = Color()
    private val limiter = FrameLimiter()

    override fun create() {
        ShopScreen.resetIntro()
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        font = BitmapFont(Gdx.files.internal(FONT_FILE)).apply {
            region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            setUseIntegerPositions(false)
        }
        scenery = Scenery()
        data = saves.loadData(saves.autosaveFile) ?: importLegacyScores()
        settings = saves.loadSettings() ?: Settings()
        limiter.fps = settings.fps
        I18n.language = settings.language
        applyCosmetics()
        if (isDesktop) setupCursor()
        awards.checkExplorer()
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        setScreen(IntroScreen(this))
    }

    override fun render() {
        super.render()
        haptics.update(Gdx.graphics.deltaTime)
        music.update(Gdx.graphics.deltaTime)
        drawAwardPopup()
        if (isDesktop && controls.isJustPressed(Action.SCREENSHOT)) screenshots.capture()
        screenshots.render(Gdx.graphics.deltaTime, viewport.worldWidth, viewport.worldHeight)
        limiter.sync()
    }

    private fun setupCursor() {
        runCatching {
            val pixmap = Pixmap(Gdx.files.internal(CURSOR_FILE))
            val created = Gdx.graphics.newCursor(pixmap, 1, 1)
            pixmap.dispose()
            Gdx.graphics.setCursor(created)
            cursor = created
        }
    }

    fun saveControls() {
        saves.saveSettings(settings)
    }

    private fun drawAwardPopup() {
        val width = viewport.worldWidth
        val height = viewport.worldHeight
        viewport.apply()
        camera.position.set(width / 2f, height / 2f, 0f)
        camera.update()
        shapes.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        awards.render(Gdx.graphics.deltaTime, width, height)
    }

    private fun importLegacyScores(): GameData {
        val imported = GameData()
        runCatching {
            val prefs = Gdx.app.getPreferences(LEGACY_PREFS)
            imported.bestCatch = prefs.getInteger("best", 0)
            imported.bestStolen = prefs.getInteger("best_stolen", 0)
        }
        saves.saveData(saves.autosaveFile, imported)
        return imported
    }

    fun persist(): Boolean = saves.saveData(saves.autosaveFile, data)

    fun changeFps(step: Int) {
        settings.cycleFps(step)
        limiter.fps = settings.fps
        saves.saveSettings(settings)
    }

    fun changeLanguage(step: Int) {
        settings.cycleLanguage(step)
        I18n.language = settings.language
        saves.saveSettings(settings)
        awards.unlock(Award.POLYGLOT)
    }

    fun saveToSlot(index: Int): Boolean {
        awards.unlock(Award.SAFE_KEEPING)
        data.savedAt = System.currentTimeMillis()
        val ok = saves.saveData(saves.slotFile(index), data)
        persist()
        return ok
    }

    fun loadFromSlot(index: Int): Boolean {
        val loaded = saves.loadData(saves.slotFile(index)) ?: return false
        val earned = data.awards
        data.copyFrom(loaded)
        data.awards = data.awards or earned
        applyCosmetics()
        persist()
        return true
    }

    override fun setScreen(screen: Screen?) {
        music.duck(1f)
        when (screen) {
            is IntroScreen -> music.play(MusicPlayer.Track.INTRO, 0f)
            is ShopScreen -> music.play(MusicPlayer.Track.SHOP)
            is MenuBase -> music.play(MusicPlayer.Track.MENU)
            is CatchGiftsScreen -> music.play(MusicPlayer.Track.CATCH_GIFTS)
            is StolenGiftsScreen -> music.play(MusicPlayer.Track.STOLEN_GIFTS)
            is EscapeCityScreen -> music.play(MusicPlayer.Track.ESCAPE_CITY)
            is SledRaceScreen -> music.play(MusicPlayer.Track.SLED_RACE)
            is PlayScreen -> music.stop()
        }
        super.setScreen(screen)
    }

    fun changeMusicVolume(step: Int) {
        settings.cycleMusic(step)
        saves.saveSettings(settings)
    }

    fun toggleHaptics() {
        settings.haptics = !settings.haptics
        saves.saveSettings(settings)
        haptics.play(Haptics.Pattern.PREVIEW)
    }

    fun changeHapticIntensity(step: Int) {
        settings.cycleIntensity(step)
        saves.saveSettings(settings)
        haptics.play(Haptics.Pattern.PREVIEW)
    }

    fun claimDailyReward(): Int {
        val amount = DailyRewards.claim(data)
        if (amount > 0) earnCoins(amount)
        return amount
    }

    fun earnCoins(amount: Int) {
        if (amount <= 0) return
        data.coins += amount
        data.coinsEarned += amount
        persist()
    }

    fun owns(item: ShopItem): Boolean = item.free || (data.owned and item.bit) != 0L

    fun hasPerk(item: ShopItem): Boolean = item.category == ShopItem.Category.PERK && owns(item)

    fun isEquipped(item: ShopItem): Boolean = when (item.category) {
        ShopItem.Category.SUIT -> equippedSuit() == item
        ShopItem.Category.JACKET -> equippedJacket() == item
        ShopItem.Category.SLED -> equippedSled() == item
        ShopItem.Category.PERK -> owns(item)
    }

    fun buy(item: ShopItem): Boolean {
        if (owns(item) || data.coins < item.price) return false
        data.coins -= item.price
        data.owned = data.owned or item.bit
        if (item.category != ShopItem.Category.PERK) equip(item) else persist()
        awards.unlock(Award.SHOPPER)
        return true
    }

    fun equip(item: ShopItem) {
        if (!owns(item)) return
        when (item.category) {
            ShopItem.Category.SUIT -> data.suit = item.indexInCategory
            ShopItem.Category.JACKET -> data.jacket = item.indexInCategory
            ShopItem.Category.SLED -> data.sledPaint = item.indexInCategory
            ShopItem.Category.PERK -> return
        }
        applyCosmetics()
        persist()
    }

    private fun equippedSuit(): ShopItem = ShopItem.suits.getOrNull(data.suit)?.takeIf { owns(it) } ?: ShopItem.SUIT_CLASSIC

    private fun equippedJacket(): ShopItem = ShopItem.jackets.getOrNull(data.jacket)?.takeIf { owns(it) } ?: ShopItem.JACKET_BLUE

    private fun equippedSled(): ShopItem = ShopItem.sleds.getOrNull(data.sledPaint)?.takeIf { owns(it) } ?: ShopItem.SLED_WOOD

    private fun applyCosmetics() {
        Cosmetics.apply(equippedSuit(), equippedJacket(), equippedSled())
    }

    fun bestScore(mode: GameMode): Int = when (mode) {
        GameMode.CATCH_GIFTS -> data.bestCatch
        GameMode.STOLEN_GIFTS -> data.bestStolen
        GameMode.ESCAPE_CITY -> data.bestEscape
        GameMode.SLED_RACE -> data.bestSledMs
    }

    fun finishSledRace(place: Int, millis: Int, gifts: Int): Boolean {
        data.sledRaces++
        if (place == 1) data.sledWins++
        data.sledGifts += gifts
        val best = data.bestSledMs == 0 || millis < data.bestSledMs
        if (best) data.bestSledMs = millis
        persist()
        awards.checkExplorer()
        return best
    }

    fun finishEscapeGame(score: Int, candies: Int, cities: Int): Boolean {
        data.escapeGames++
        data.candiesEaten += candies
        data.citiesEscaped += cities
        val best = score > data.bestEscape
        if (best) data.bestEscape = score
        persist()
        awards.checkExplorer()
        return best
    }

    fun finishCatchGame(score: Int): Boolean {
        data.catchGames++
        data.giftsCaught += score
        val best = score > data.bestCatch
        if (best) data.bestCatch = score
        persist()
        awards.checkExplorer()
        return best
    }

    fun checkpointStolenRun(house: Int, gifts: Int, houses: Int, gained: Int) {
        data.runHouse = house
        data.runGifts = gifts
        data.runHouses = houses
        data.giftsStolen += gained
        persist()
    }

    fun finishStolenRun(gifts: Int): Boolean {
        data.stolenGames++
        data.clearRun()
        val best = gifts > data.bestStolen
        if (best) data.bestStolen = gifts
        persist()
        awards.checkExplorer()
        return best
    }

    fun wrapText(text: String, scale: Float, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in text.split(' ')) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (current.isNotEmpty() && textWidth(candidate, scale) > maxWidth) {
                lines += current.toString()
                current = StringBuilder(word)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    fun textWidth(text: String, scale: Float): Float {
        if (scale < MIN_SCALE) return 0f
        font.data.setScale(scale * FONT_RATIO)
        layout.setText(font, text)
        return layout.width
    }

    fun drawText(
        text: String,
        x: Float,
        y: Float,
        scale: Float,
        color: Color = Palette.TEXT,
        centered: Boolean = true,
        alpha: Float = 1f,
        shadow: Boolean = true
    ) {
        if (alpha <= 0f || scale < MIN_SCALE) return
        font.data.setScale(scale * FONT_RATIO)
        layout.setText(font, text)
        val drawX = if (centered) x - layout.width / 2f else x
        val drawY = if (centered) y + layout.height / 2f else y
        if (shadow) {
            textColor.set(Palette.TEXT_SHADOW)
            textColor.a *= alpha
            font.color = textColor
            font.draw(batch, text, drawX + scale, drawY - scale)
        }
        textColor.set(color)
        textColor.a *= alpha
        font.color = textColor
        font.draw(batch, text, drawX, drawY)
    }

    override fun dispose() {
        screen?.dispose()
        music.dispose()
        screenshots.dispose()
        cursor?.dispose()
        batch.dispose()
        shapes.dispose()
        font.dispose()
    }

    companion object {
        const val WORLD_WIDTH = 1280f
        const val WORLD_HEIGHT = 720f
        const val GROUND_HEIGHT = 90f
        private const val LEGACY_PREFS = "santa-game"
        private const val FONT_FILE = "fonts/santa.fnt"
        private const val CURSOR_FILE = "cursors/cursor.png"
        private const val FONT_RATIO = 0.42f
        private const val MIN_SCALE = 0.05f
    }
}
