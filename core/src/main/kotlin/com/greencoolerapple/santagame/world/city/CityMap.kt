package com.greencoolerapple.santagame.world.city

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.greencoolerapple.santagame.Palette
import kotlin.random.Random

class CityMap(level: Int, seed: Long) {
    class Block(val area: Rectangle, val building: Rectangle?, val roof: Int, val lights: Boolean, val chimney: Vector2)

    private val random = Random(seed)

    val columns = minOf(MAX_COLUMNS, 4 + level)
    val rows = minOf(MAX_ROWS, 3 + level)
    val width = STREET + columns * (BLOCK_WIDTH + STREET)
    val height = STREET + rows * (BLOCK_HEIGHT + STREET)

    val blocks = mutableListOf<Block>()
    val trees = mutableListOf<Vector2>()
    val obstacles = mutableListOf<Rectangle>()
    val lamps = mutableListOf<Vector2>()
    val start = Vector2(STREET / 2f, STREET / 2f)
    val exit = Vector2(width - STREET / 2f, height - STREET / 2f)

    private val gridWidth = (width / TILE).toInt()
    private val gridHeight = (height / TILE).toInt()
    private val walkable = BooleanArray(gridWidth * gridHeight)
    private val parent = IntArray(gridWidth * gridHeight)
    private val visited = IntArray(gridWidth * gridHeight)
    private val queue = IntArray(gridWidth * gridHeight)
    private var stamp = 0

    private val probe = Circle()
    private val tileRect = Rectangle()
    private val tint = Color()

    init {
        for (c in 0 until columns) {
            for (r in 0 until rows) {
                val x = STREET + c * (BLOCK_WIDTH + STREET)
                val y = STREET + r * (BLOCK_HEIGHT + STREET)
                val area = Rectangle(x, y, BLOCK_WIDTH, BLOCK_HEIGHT)
                val park = random.nextFloat() < PARK_CHANCE
                if (park) {
                    blocks += Block(area, null, 0, false, Vector2())
                    repeat(random.nextInt(3, 6)) {
                        val tx = x + 40f + random.nextFloat() * (BLOCK_WIDTH - 80f)
                        val ty = y + 40f + random.nextFloat() * (BLOCK_HEIGHT - 80f)
                        trees += Vector2(tx, ty)
                        obstacles += Rectangle(tx - TREE_SIZE / 2f, ty - TREE_SIZE / 2f, TREE_SIZE, TREE_SIZE)
                    }
                } else {
                    val building = Rectangle(x + SIDEWALK, y + SIDEWALK, BLOCK_WIDTH - SIDEWALK * 2f, BLOCK_HEIGHT - SIDEWALK * 2f)
                    val chimney = Vector2(
                        building.x + 30f + random.nextFloat() * (building.width - 60f),
                        building.y + 30f + random.nextFloat() * (building.height - 60f)
                    )
                    blocks += Block(area, building, random.nextInt(ROOFS.size), random.nextFloat() < 0.6f, chimney)
                    obstacles += building
                }
            }
        }
        for (c in 0..columns) {
            for (r in 0..rows) {
                lamps += Vector2(STREET / 2f + c * (BLOCK_WIDTH + STREET), STREET / 2f + r * (BLOCK_HEIGHT + STREET))
            }
        }
        for (ty in 0 until gridHeight) {
            for (tx in 0 until gridWidth) {
                tileRect.set(tx * TILE + 2f, ty * TILE + 2f, TILE - 4f, TILE - 4f)
                walkable[ty * gridWidth + tx] = obstacles.none { it.overlaps(tileRect) }
            }
        }
    }

    fun randomOpenPoint(awayFrom: Vector2, minDistance: Float, out: Vector2): Vector2 {
        repeat(400) {
            val tx = random.nextInt(gridWidth)
            val ty = random.nextInt(gridHeight)
            if (walkable[ty * gridWidth + tx]) {
                out.set(tx * TILE + TILE / 2f, ty * TILE + TILE / 2f)
                if (out.dst(awayFrom) >= minDistance) return out
            }
        }
        return out.set(exit)
    }

    fun randomOpenPointNear(center: Vector2, radius: Float, out: Vector2): Vector2 {
        repeat(60) {
            val x = center.x + (random.nextFloat() * 2f - 1f) * radius
            val y = center.y + (random.nextFloat() * 2f - 1f) * radius
            val index = tileIndex(x, y)
            if (index >= 0 && walkable[index]) return out.set(x, y)
        }
        return randomOpenPoint(center, 0f, out)
    }

    fun move(position: Vector2, dx: Float, dy: Float, radius: Float) {
        if (canStand(position.x + dx, position.y, radius)) position.x += dx
        if (canStand(position.x, position.y + dy, radius)) position.y += dy
    }

    fun canStand(x: Float, y: Float, radius: Float): Boolean {
        if (x - radius < 0f || x + radius > width || y - radius < 0f || y + radius > height) return false
        probe.set(x, y, radius)
        for (rect in obstacles) {
            if (Intersector.overlaps(probe, rect)) return false
        }
        return true
    }

    fun lineOfSight(from: Vector2, to: Vector2): Boolean {
        val minX = minOf(from.x, to.x)
        val maxX = maxOf(from.x, to.x)
        val minY = minOf(from.y, to.y)
        val maxY = maxOf(from.y, to.y)
        for (rect in obstacles) {
            if (rect.x > maxX || rect.x + rect.width < minX || rect.y > maxY || rect.y + rect.height < minY) continue
            if (Intersector.intersectSegmentRectangle(from, to, rect)) return false
        }
        return true
    }

    fun findPath(from: Vector2, to: Vector2, out: MutableList<Vector2>): Boolean {
        out.clear()
        val startIndex = nearestWalkable(tileIndex(from.x, from.y))
        val goalIndex = nearestWalkable(tileIndex(to.x, to.y))
        if (startIndex < 0 || goalIndex < 0) return false
        if (startIndex == goalIndex) {
            out += Vector2(to)
            return true
        }

        stamp++
        var head = 0
        var tail = 0
        queue[tail++] = startIndex
        visited[startIndex] = stamp
        parent[startIndex] = -1
        var found = false
        while (head < tail) {
            val current = queue[head++]
            if (current == goalIndex) {
                found = true
                break
            }
            val cx = current % gridWidth
            val cy = current / gridWidth
            for (d in 0 until 4) {
                val nx = cx + DX[d]
                val ny = cy + DY[d]
                if (nx < 0 || ny < 0 || nx >= gridWidth || ny >= gridHeight) continue
                val next = ny * gridWidth + nx
                if (!walkable[next] || visited[next] == stamp) continue
                visited[next] = stamp
                parent[next] = current
                queue[tail++] = next
            }
        }
        if (!found) return false

        var node = goalIndex
        while (node != -1 && node != startIndex) {
            out.add(0, Vector2((node % gridWidth) * TILE + TILE / 2f, (node / gridWidth) * TILE + TILE / 2f))
            node = parent[node]
        }
        simplify(out)
        return out.isNotEmpty()
    }

    private fun simplify(path: MutableList<Vector2>) {
        if (path.size < 3) return
        var i = 1
        while (i < path.size - 1) {
            val a = path[i - 1]
            val b = path[i]
            val c = path[i + 1]
            val straight = (a.x == b.x && b.x == c.x) || (a.y == b.y && b.y == c.y)
            if (straight) path.removeAt(i) else i++
        }
    }

    private fun tileIndex(x: Float, y: Float): Int {
        val tx = (x / TILE).toInt()
        val ty = (y / TILE).toInt()
        if (tx < 0 || ty < 0 || tx >= gridWidth || ty >= gridHeight) return -1
        return ty * gridWidth + tx
    }

    private fun nearestWalkable(index: Int): Int {
        if (index < 0) return -1
        if (walkable[index]) return index
        val cx = index % gridWidth
        val cy = index / gridWidth
        for (radius in 1..3) {
            for (oy in -radius..radius) {
                for (ox in -radius..radius) {
                    val nx = cx + ox
                    val ny = cy + oy
                    if (nx < 0 || ny < 0 || nx >= gridWidth || ny >= gridHeight) continue
                    val n = ny * gridWidth + nx
                    if (walkable[n]) return n
                }
            }
        }
        return -1
    }

    fun drawGround(shapes: ShapeRenderer, view: Rectangle, time: Float) {
        shapes.color = Palette.CITY_STREET
        shapes.rect(view.x - 50f, view.y - 50f, view.width + 100f, view.height + 100f)

        shapes.color = Palette.CITY_MARKING
        for (r in 0..rows) {
            val y = STREET / 2f + r * (BLOCK_HEIGHT + STREET)
            if (y < view.y - 10f || y > view.y + view.height + 10f) continue
            var x = maxOf(0f, view.x - (view.x % 80f))
            while (x < minOf(width, view.x + view.width)) {
                shapes.rect(x, y - 3f, 40f, 6f)
                x += 80f
            }
        }
        for (c in 0..columns) {
            val x = STREET / 2f + c * (BLOCK_WIDTH + STREET)
            if (x < view.x - 10f || x > view.x + view.width + 10f) continue
            var y = maxOf(0f, view.y - (view.y % 80f))
            while (y < minOf(height, view.y + view.height)) {
                shapes.rect(x - 3f, y, 6f, 40f)
                y += 80f
            }
        }

        for (block in blocks) {
            val a = block.area
            if (!a.overlaps(view)) continue
            shapes.color = Palette.CITY_SIDEWALK
            shapes.rect(a.x - 6f, a.y - 6f, a.width + 12f, a.height + 12f)
            val building = block.building
            if (building == null) {
                shapes.color = Palette.SNOW
                shapes.rect(a.x + 6f, a.y + 6f, a.width - 12f, a.height - 12f)
                shapes.color = Palette.SNOW_SHADE
                shapes.rect(a.x + a.width / 2f - 14f, a.y + 6f, 28f, a.height - 12f)
            } else {
                drawBuilding(shapes, block, building, time)
            }
        }

        for (lamp in lamps) {
            if (!view.contains(lamp)) continue
            shapes.color = Palette.LAMP_GLOW
            shapes.circle(lamp.x, lamp.y, 70f, 28)
        }
    }

    private fun drawBuilding(shapes: ShapeRenderer, block: Block, b: Rectangle, time: Float) {
        shapes.color = Palette.SHADOW
        shapes.rect(b.x + 8f, b.y - 10f, b.width, b.height)
        val roof = ROOFS[block.roof]
        shapes.color = roof
        shapes.rect(b.x, b.y, b.width, b.height)
        tint.set(roof).lerp(Color.BLACK, 0.18f)
        shapes.color = tint
        shapes.rect(b.x, b.y + b.height / 2f - 3f, b.width, 6f)
        shapes.color = Palette.SNOW
        shapes.rect(b.x + 6f, b.y + b.height - 22f, b.width - 12f, 16f)
        shapes.rect(b.x + 6f, b.y + 6f, b.width - 12f, 12f)
        shapes.color = Palette.BRICK
        shapes.rect(block.chimney.x - 14f, block.chimney.y - 14f, 28f, 28f)
        shapes.color = Palette.HEARTH
        shapes.rect(block.chimney.x - 8f, block.chimney.y - 8f, 16f, 16f)

        if (block.lights) {
            val step = 26f
            var i = 0
            var x = b.x + 10f
            while (x < b.x + b.width - 6f) {
                drawBulb(shapes, x, b.y + b.height - 3f, i, time)
                drawBulb(shapes, x, b.y + 3f, i + 2, time)
                x += step
                i++
            }
        }
    }

    private fun drawBulb(shapes: ShapeRenderer, x: Float, y: Float, index: Int, time: Float) {
        val on = ((time * 2f).toInt() + index) % 3 != 0
        val base = Palette.GIFT_BOXES[index % Palette.GIFT_BOXES.size]
        tint.set(base)
        if (!on) tint.lerp(Color.BLACK, 0.5f)
        shapes.color = tint
        shapes.circle(x, y, 4f, 8)
    }

    fun drawTrees(shapes: ShapeRenderer, view: Rectangle) {
        for (tree in trees) {
            if (!view.contains(tree)) continue
            shapes.color = Palette.SHADOW
            shapes.circle(tree.x + 5f, tree.y - 6f, 24f, 20)
            shapes.color = Palette.TREE
            shapes.circle(tree.x, tree.y, 24f, 20)
            shapes.color = Palette.TREE_LIGHT
            shapes.circle(tree.x, tree.y, 15f, 16)
            shapes.color = Palette.SNOW
            shapes.circle(tree.x - 6f, tree.y + 6f, 5f, 10)
            shapes.circle(tree.x + 8f, tree.y - 4f, 4f, 10)
        }
    }

    fun drawMinimap(shapes: ShapeRenderer, x: Float, y: Float, scale: Float) {
        shapes.color = Palette.MINIMAP_BG
        shapes.rect(x - 4f, y - 4f, width * scale + 8f, height * scale + 8f)
        for (block in blocks) {
            val a = block.area
            shapes.color = if (block.building == null) Palette.MINIMAP_PARK else Palette.MINIMAP_BLOCK
            shapes.rect(x + a.x * scale, y + a.y * scale, a.width * scale, a.height * scale)
        }
    }

    companion object {
        const val TILE = 40f
        const val STREET = 160f
        const val BLOCK_WIDTH = 280f
        const val BLOCK_HEIGHT = 240f
        private const val SIDEWALK = 14f
        private const val TREE_SIZE = 34f
        private const val PARK_CHANCE = 0.22f
        private const val MAX_COLUMNS = 9
        private const val MAX_ROWS = 6
        private val DX = intArrayOf(1, -1, 0, 0)
        private val DY = intArrayOf(0, 0, 1, -1)

        private val ROOFS = listOf(
            Color.valueOf("7a3b3b"),
            Color.valueOf("3b5a7a"),
            Color.valueOf("5b4a7a"),
            Color.valueOf("4a6b4a"),
            Color.valueOf("7a6a3b")
        )
    }
}
