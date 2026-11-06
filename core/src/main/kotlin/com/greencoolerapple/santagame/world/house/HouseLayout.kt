package com.greencoolerapple.santagame.world.house

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2

class Furniture(val bounds: Rectangle, val style: Int)

class HouseLayout(
    val room: Rectangle,
    val interior: Rectangle,
    val fireplace: Rectangle,
    val spawn: Vector2,
    val tree: Vector2,
    val treeBounds: Rectangle,
    val furniture: List<Furniture>,
    val guardPaths: List<List<Vector2>>
) {
    val blockers: List<Rectangle> = furniture.map { it.bounds } + treeBounds

    companion object {
        const val ROOM_WIDTH = 1280f
        const val ROOM_HEIGHT = 720f
        const val TREE_RADIUS = 46f

        private class Template(val furniture: List<FloatArray>, val paths: List<FloatArray>)

        private val templates = listOf(
            Template(
                furniture = listOf(
                    floatArrayOf(330f, 120f, 240f, 80f, 0f),
                    floatArrayOf(560f, 330f, 170f, 110f, 1f),
                    floatArrayOf(330f, 470f, 70f, 170f, 2f),
                    floatArrayOf(880f, 140f, 100f, 100f, 3f),
                    floatArrayOf(760f, 560f, 200f, 80f, 4f)
                ),
                paths = listOf(
                    floatArrayOf(480f, 275f, 1000f, 275f, 1000f, 470f, 480f, 470f),
                    floatArrayOf(1180f, 100f, 1180f, 420f),
                    floatArrayOf(620f, 80f, 1100f, 80f)
                )
            ),
            Template(
                furniture = listOf(
                    floatArrayOf(300f, 230f, 150f, 220f, 1f),
                    floatArrayOf(620f, 520f, 260f, 80f, 0f),
                    floatArrayOf(300f, 40f, 160f, 70f, 4f),
                    floatArrayOf(850f, 220f, 180f, 120f, 5f),
                    floatArrayOf(980f, 40f, 140f, 70f, 2f)
                ),
                paths = listOf(
                    floatArrayOf(530f, 100f, 530f, 450f, 780f, 450f, 780f, 100f),
                    floatArrayOf(1180f, 100f, 1180f, 400f),
                    floatArrayOf(220f, 600f, 560f, 600f)
                )
            ),
            Template(
                furniture = listOf(
                    floatArrayOf(250f, 100f, 80f, 200f, 2f),
                    floatArrayOf(450f, 400f, 220f, 90f, 0f),
                    floatArrayOf(450f, 40f, 200f, 60f, 4f),
                    floatArrayOf(750f, 150f, 90f, 250f, 1f),
                    floatArrayOf(900f, 520f, 150f, 120f, 3f)
                ),
                paths = listOf(
                    floatArrayOf(380f, 330f, 700f, 330f, 700f, 560f, 380f, 560f),
                    floatArrayOf(930f, 100f, 1180f, 100f, 1180f, 420f, 930f, 420f),
                    floatArrayOf(200f, 600f, 400f, 600f)
                )
            )
        )

        val count: Int
            get() = templates.size

        fun build(index: Int, offsetX: Float, offsetY: Float): HouseLayout {
            val template = templates[index % templates.size]
            val furniture = template.furniture.map {
                Furniture(Rectangle(it[0] + offsetX, it[1] + offsetY, it[2], it[3]), it[4].toInt())
            }
            val paths = template.paths.map { values ->
                List(values.size / 2) { i -> Vector2(values[i * 2] + offsetX, values[i * 2 + 1] + offsetY) }
            }
            val tree = Vector2(1120f + offsetX, 520f + offsetY)
            return HouseLayout(
                room = Rectangle(20f + offsetX, 20f + offsetY, 1240f, 640f),
                interior = Rectangle(40f + offsetX, 40f + offsetY, 1200f, 600f),
                fireplace = Rectangle(40f + offsetX, 290f + offsetY, 70f, 130f),
                spawn = Vector2(140f + offsetX, 355f + offsetY),
                tree = tree,
                treeBounds = Rectangle(tree.x - TREE_RADIUS + 6f, tree.y - TREE_RADIUS + 6f, (TREE_RADIUS - 6f) * 2f, (TREE_RADIUS - 6f) * 2f),
                furniture = furniture,
                guardPaths = paths
            )
        }
    }
}
