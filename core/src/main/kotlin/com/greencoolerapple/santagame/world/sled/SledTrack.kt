package com.greencoolerapple.santagame.world.sled

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.greencoolerapple.santagame.Palette
import com.greencoolerapple.santagame.ui.Draw
import kotlin.random.Random

class SledTrack(seed: Long) {
    enum class Kind { ROCK, TREE, SNOWMAN, BOOST, GIFT }

    class Item(val kind: Kind, val x: Float, val y: Float) {
        var taken = false

        val solid: Boolean
            get() = kind == Kind.ROCK || kind == Kind.TREE || kind == Kind.SNOWMAN

        val radius: Float
            get() = when (kind) {
                Kind.ROCK -> 24f
                Kind.TREE -> 26f
                Kind.SNOWMAN -> 24f
                Kind.BOOST -> 34f
                Kind.GIFT -> 22f
            }
    }

    class Scenery(val x: Float, val y: Float, val size: Float, val snowy: Boolean)

    private val random = Random(seed)
    private val phase1 = random.nextFloat() * MathUtils.PI2
    private val phase2 = random.nextFloat() * MathUtils.PI2
    private val phase3 = random.nextFloat() * MathUtils.PI2
    private val tint = Color()

    val items = mutableListOf<Item>()
    val forest = mutableListOf<Scenery>()

    init {
        var y = FIRST_ROW
        var row = 0
        while (y < LENGTH - 500f) {
            val lanes = LANES.indices.shuffled(random).toMutableList()
            val hazards = when {
                y < 4000f -> 1
                random.nextFloat() < 0.45f -> 2
                else -> 1
            }
            repeat(hazards) {
                val lane = lanes.removeAt(0)
                val kind = when (random.nextInt(3)) {
                    0 -> Kind.ROCK
                    1 -> Kind.TREE
                    else -> Kind.SNOWMAN
                }
                items += Item(kind, laneX(lane, y), y + random.nextFloat() * 40f - 20f)
            }
            if (row % 3 == 1) items += Item(Kind.BOOST, laneX(lanes.removeAt(0), y + 150f), y + 150f)
            if (random.nextFloat() < 0.65f) {
                val lane = lanes.removeAt(0)
                for (k in 0 until 3) items += Item(Kind.GIFT, laneX(lane, y + 70f + k * 55f), y + 70f + k * 55f)
            }
            y += 300f + random.nextFloat() * 160f
            row++
        }
        items.sortBy { it.y }

        var fy = -400f
        while (fy < LENGTH + 1200f) {
            for (side in intArrayOf(-1, 1)) {
                val edge = centerAt(fy) + side * (halfWidthAt(fy) + 40f)
                val offset = 30f + random.nextFloat() * 380f
                forest += Scenery(edge + side * offset, fy + random.nextFloat() * 60f, 0.8f + random.nextFloat() * 0.6f, random.nextBoolean())
            }
            fy += 70f
        }
    }

    fun laneX(lane: Int, y: Float): Float = centerAt(y) + LANES[lane] * halfWidthAt(y)

    fun centerAt(y: Float): Float {
        val ramp = MathUtils.clamp((y - 300f) / 1500f, 0f, 1f)
        return ramp * (
            MathUtils.sin(y / 1300f + phase1) * 360f +
                MathUtils.sin(y / 610f + phase2) * 120f +
                MathUtils.sin(y / 2900f + phase3) * 260f
            )
    }

    fun halfWidthAt(y: Float): Float = 250f + MathUtils.sin(y / 1700f + phase2) * 50f

    fun limitAt(y: Float): Float = halfWidthAt(y) + OFF_TRACK_ROOM

    fun onTrack(x: Float, y: Float): Boolean = kotlin.math.abs(x - centerAt(y)) <= halfWidthAt(y)

    fun drawGround(shapes: ShapeRenderer, view: Rectangle, time: Float) {
        shapes.color = Palette.SNOW
        shapes.rect(view.x - 50f, view.y - 50f, view.width + 100f, view.height + 100f)

        val start = (view.y / STEP).toInt() * STEP - STEP
        val end = view.y + view.height + STEP
        var y = start
        while (y < end) {
            val y2 = y + STEP
            val c1 = centerAt(y)
            val c2 = centerAt(y2)
            val w1 = halfWidthAt(y)
            val w2 = halfWidthAt(y2)
            shapes.color = Palette.SNOW_SHADE
            quad(shapes, c1 - w1 - 18f, c1 + w1 + 18f, y, c2 - w2 - 18f, c2 + w2 + 18f, y2)
            shapes.color = Palette.SLED_TRACK
            quad(shapes, c1 - w1, c1 + w1, y, c2 - w2, c2 + w2, y2)
            shapes.color = Palette.SLED_GROOVE
            for (f in GROOVES) {
                shapes.rectLine(c1 + f * w1, y, c2 + f * w2, y2, 3f)
            }
            y = y2
        }

        drawLine(shapes, START_LINE, view, false)
        drawLine(shapes, LENGTH, view, true)

        var p = (view.y / POLE_STEP).toInt() * POLE_STEP
        while (p < end) {
            if (p > -200f) {
                val c = centerAt(p)
                val w = halfWidthAt(p)
                val red = ((p / POLE_STEP).toInt() % 2 == 0)
                drawPole(shapes, c - w - 14f, p, red)
                drawPole(shapes, c + w + 14f, p, !red)
            }
            p += POLE_STEP
        }
    }

    private fun quad(shapes: ShapeRenderer, l1: Float, r1: Float, y1: Float, l2: Float, r2: Float, y2: Float) {
        shapes.triangle(l1, y1, r1, y1, r2, y2)
        shapes.triangle(l1, y1, r2, y2, l2, y2)
    }

    private fun drawLine(shapes: ShapeRenderer, y: Float, view: Rectangle, finish: Boolean) {
        if (y < view.y - 80f || y > view.y + view.height + 80f) return
        val c = centerAt(y)
        val w = halfWidthAt(y)
        val cells = 16
        val size = w * 2f / cells
        for (row in 0 until 2) {
            for (i in 0 until cells) {
                shapes.color = if ((i + row) % 2 == 0) Palette.BLACK else Palette.SNOW
                if (!finish && (i + row) % 2 == 0) shapes.color = Palette.SANTA_RED
                shapes.rect(c - w + i * size, y + row * size * 0.5f, size, size * 0.5f)
            }
        }
        if (finish) {
            shapes.color = Palette.TRUNK
            shapes.rect(c - w - 30f, y - 6f, 14f, 60f)
            shapes.rect(c + w + 16f, y - 6f, 14f, 60f)
        }
    }

    private fun drawPole(shapes: ShapeRenderer, x: Float, y: Float, red: Boolean) {
        shapes.color = Palette.SHADOW
        shapes.circle(x + 4f, y - 4f, 8f, 10)
        shapes.color = if (red) Palette.SANTA_RED else Palette.SNOW
        shapes.circle(x, y, 7f, 12)
        shapes.color = if (red) Palette.SNOW else Palette.SANTA_RED
        shapes.circle(x, y, 3.5f, 8)
    }

    fun drawItems(shapes: ShapeRenderer, view: Rectangle, time: Float) {
        for (item in items) {
            if (item.taken) continue
            if (item.y < view.y - 60f) continue
            if (item.y > view.y + view.height + 60f) break
            when (item.kind) {
                Kind.ROCK -> drawRock(shapes, item.x, item.y)
                Kind.TREE -> drawTree(shapes, item.x, item.y, 1f, true)
                Kind.SNOWMAN -> drawSnowman(shapes, item.x, item.y)
                Kind.BOOST -> drawBoost(shapes, item.x, item.y, time)
                Kind.GIFT -> {
                    val bob = MathUtils.sin(time * 4f + item.y * 0.01f) * 3f
                    shapes.color = Palette.SHADOW
                    shapes.circle(item.x + 3f, item.y - 8f, 11f, 12)
                    Draw.goldenGift(shapes, item.x, item.y + bob, 24f)
                }
            }
        }
    }

    fun drawForest(shapes: ShapeRenderer, view: Rectangle) {
        for (tree in forest) {
            if (tree.y < view.y - 80f || tree.y > view.y + view.height + 80f) continue
            if (tree.x < view.x - 80f || tree.x > view.x + view.width + 80f) continue
            drawTree(shapes, tree.x, tree.y, tree.size, tree.snowy)
        }
    }

    private fun drawRock(shapes: ShapeRenderer, x: Float, y: Float) {
        shapes.color = Palette.SHADOW
        shapes.ellipse(x - 24f, y - 22f, 52f, 30f, 16)
        shapes.color = Palette.ROCK_DARK
        shapes.circle(x - 6f, y - 2f, 20f, 16)
        shapes.circle(x + 10f, y + 2f, 16f, 14)
        shapes.color = Palette.ROCK
        shapes.circle(x - 8f, y + 2f, 16f, 16)
        shapes.circle(x + 8f, y + 5f, 12f, 14)
        shapes.color = Palette.SNOW
        shapes.ellipse(x - 18f, y + 8f, 26f, 10f, 12)
    }

    private fun drawTree(shapes: ShapeRenderer, x: Float, y: Float, scale: Float, snowy: Boolean) {
        val r = 26f * scale
        shapes.color = Palette.SHADOW
        shapes.circle(x + 6f * scale, y - 8f * scale, r, 18)
        shapes.color = Palette.PINE_DARK
        shapes.circle(x, y, r, 18)
        shapes.color = Palette.PINE
        shapes.circle(x - 3f * scale, y + 3f * scale, r * 0.72f, 16)
        shapes.color = Palette.TREE_LIGHT
        shapes.circle(x - 5f * scale, y + 6f * scale, r * 0.38f, 12)
        if (snowy) {
            shapes.color = Palette.SNOW
            shapes.circle(x - 9f * scale, y + 10f * scale, r * 0.28f, 10)
            shapes.circle(x + 8f * scale, y - 4f * scale, r * 0.2f, 10)
        }
    }

    private fun drawSnowman(shapes: ShapeRenderer, x: Float, y: Float) {
        shapes.color = Palette.SHADOW
        shapes.circle(x + 5f, y - 7f, 24f, 18)
        shapes.color = Palette.SNOW_SHADE
        shapes.circle(x, y, 23f, 20)
        shapes.color = Palette.SNOW
        shapes.circle(x - 2f, y + 2f, 20f, 20)
        shapes.color = Palette.SNOW_SHADE
        shapes.circle(x, y + 4f, 14f, 18)
        shapes.color = Palette.SNOW
        shapes.circle(x - 1f, y + 5f, 12f, 16)
        shapes.color = Palette.BLACK
        shapes.circle(x, y + 5f, 8f, 14)
        shapes.color = Palette.COAL_SHINE
        shapes.circle(x, y + 5f, 5f, 12)
        shapes.color = Palette.CARROT
        shapes.triangle(x - 3f, y - 5f, x + 3f, y - 5f, x, y - 18f)
        shapes.color = Palette.SANTA_RED
        shapes.rect(x - 16f, y - 2f, 32f, 5f)
    }

    private fun drawBoost(shapes: ShapeRenderer, x: Float, y: Float, time: Float) {
        shapes.color = Palette.BOOST_GLOW
        Draw.roundedRect(shapes, x - 38f, y - 46f, 76f, 92f, 16f)
        shapes.color = Palette.BOOST
        Draw.roundedRect(shapes, x - 30f, y - 38f, 60f, 76f, 12f)
        for (k in 0 until 3) {
            val pulse = ((time * 3f - k * 0.33f) % 1f + 1f) % 1f
            tint.set(Palette.SNOW)
            tint.a = 0.35f + 0.65f * (1f - pulse)
            shapes.color = tint
            val cy = y - 22f + k * 20f
            shapes.triangle(x - 20f, cy, x, cy + 14f, x, cy + 6f)
            shapes.triangle(x - 20f, cy, x, cy + 6f, x - 20f, cy - 8f)
            shapes.triangle(x + 20f, cy, x, cy + 14f, x, cy + 6f)
            shapes.triangle(x + 20f, cy, x, cy + 6f, x + 20f, cy - 8f)
        }
    }

    companion object {
        const val LENGTH = 30000f
        const val START_LINE = 240f
        const val OFF_TRACK_ROOM = 170f
        private const val FIRST_ROW = 1200f
        private const val STEP = 40f
        private const val POLE_STEP = 220f
        val LANES = floatArrayOf(-0.68f, -0.34f, 0f, 0.34f, 0.68f)
        private val GROOVES = floatArrayOf(-0.5f, -0.38f, 0.38f, 0.5f)
    }
}
