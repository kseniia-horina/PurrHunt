package app.purrhunt.game.scenes

import app.purrhunt.domain.Location
import app.purrhunt.domain.Medium
import app.purrhunt.game.engine.GameRandom
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class SpotKind(val occludes: Boolean) {
    BURROW(false), CRATE(true), SACK(true), BUSH(true), FEEDER(false), FLOWER(false),
    STONE(true), GRASS(true), LILY(true), REED(true)
}

/**
 * A place creatures hide in or rest on. Occluding spots are drawn in the foreground layer, so a creature
 * at their centre is covered and a creature at their rim is half visible ("peeking").
 */
class HidingSpot(
    val kind: SpotKind,
    val x: Float,
    val y: Float,
    val radius: Float,
    val medium: Medium,
    /** Direction a creature faces when it comes out. */
    val exitAngle: Float,
) {
    /** Burrows sit in the wall: a creature inside is clipped above this line. */
    var clipLine: Float = Float.NaN

    val isShelter: Boolean get() = kind.occludes || kind == SpotKind.BURROW
}

/** Elliptic water area; a pond scene uses one covering the whole screen. */
class WaterArea(val cx: Float, val cy: Float, val rx: Float, val ry: Float) {
    fun contains(x: Float, y: Float, margin: Float = 0f): Boolean {
        val ex = rx - margin
        val ey = ry - margin
        if (ex <= 0f || ey <= 0f) return false
        val dx = (x - cx) / ex
        val dy = (y - cy) / ey
        return dx * dx + dy * dy <= 1f
    }
}

/** Pure geometry of a scene for a given screen size: hiding spots, water and the playable floor. */
class SceneLayout(
    val location: Location,
    val width: Float,
    val height: Float,
    val dp: Float,
    val spots: List<HidingSpot>,
    val water: WaterArea?,
    /** Top of the walkable floor (the basement wall is above it). */
    val floorTop: Float,
) {
    val margin: Float get() = 24f * dp

    fun isWater(x: Float, y: Float, margin: Float = 0f): Boolean = water?.contains(x, y, margin) == true

    fun allows(medium: Medium, x: Float, y: Float): Boolean = when (medium) {
        Medium.AIR -> x in 0f..width && y in 0f..height
        Medium.GROUND -> y >= floorTop && x in 0f..width && y <= height && !isWater(x, y, -16f * dp)
        Medium.WATER, Medium.WATER_SURFACE -> isWater(x, y, 12f * dp)
    }

    fun spotsFor(medium: Medium): List<HidingSpot> = when (medium) {
        Medium.GROUND -> groundSpots
        Medium.WATER -> waterShelters
        Medium.WATER_SURFACE -> emptyList()
        Medium.AIR -> flowers
    }

    private val groundSpots = spots.filter { it.medium == Medium.GROUND && it.isShelter }
    private val waterShelters = spots.filter { it.medium == Medium.WATER && it.isShelter }
    val flowers = spots.filter { it.kind == SpotKind.FLOWER }
    val feeders = spots.filter { it.kind == SpotKind.FEEDER }
    val burrows = spots.filter { it.kind == SpotKind.BURROW }

    /** Writes a random point allowed for [medium] into [out] (x, y). Falls back to the screen centre. */
    fun randomPoint(medium: Medium, random: GameRandom, out: FloatArray, edgeAffinity: Float = 0f) {
        val m = margin
        repeat(40) {
            var px: Float
            var py: Float
            if (random.chance(edgeAffinity) && medium == Medium.GROUND) {
                // Hug a wall or an object: rodents prefer to run along edges.
                if (random.chance(0.5f) && groundSpots.isNotEmpty()) {
                    val s = random.pick(groundSpots)
                    val a = random.range(0f, (2 * PI).toFloat())
                    val r = s.radius + random.range(10f, 30f) * dp
                    px = s.x + cos(a) * r
                    py = s.y + sin(a) * r
                } else {
                    when (random.int(3)) {
                        0 -> { px = random.range(m, width - m); py = floorTop + random.range(6f, 26f) * dp }
                        1 -> { px = random.range(m, m + 30f * dp); py = random.range(floorTop + m, height - m) }
                        else -> { px = random.range(width - m - 30f * dp, width - m); py = random.range(floorTop + m, height - m) }
                    }
                }
            } else if (water != null && (medium == Medium.WATER || medium == Medium.WATER_SURFACE)) {
                val a = random.range(0f, (2 * PI).toFloat())
                val r = sqrt(random.float()) * 0.85f
                px = water.cx + cos(a) * water.rx * r
                py = water.cy + sin(a) * water.ry * r
            } else {
                px = random.range(m, width - m)
                py = random.range(maxOf(m, if (medium == Medium.GROUND) floorTop + m else m), height - m)
            }
            if (allows(medium, px, py) && px in m..(width - m) && py in m..(height - m)) {
                out[0] = px
                out[1] = py
                return
            }
        }
        out[0] = water?.cx?.takeIf { medium != Medium.GROUND && medium != Medium.AIR } ?: (width / 2)
        out[1] = water?.cy?.takeIf { medium != Medium.GROUND && medium != Medium.AIR } ?: ((floorTop + height) / 2)
    }

    /** The shelter for [medium] that is closest to (x, y) but not towards the threat at (tx, ty). */
    fun shelterAwayFrom(medium: Medium, x: Float, y: Float, tx: Float, ty: Float): HidingSpot? {
        var best: HidingSpot? = null
        var bestScore = Float.MAX_VALUE
        for (s in spotsFor(medium)) {
            if (s.kind == SpotKind.FLOWER) continue
            val d = dist(x, y, s.x, s.y)
            val threat = dist(tx, ty, s.x, s.y)
            // Spots behind the threat are penalised heavily.
            val score = d - threat * 0.8f + if (threat < d * 0.6f) 10_000f else 0f
            if (score < bestScore) {
                bestScore = score
                best = s
            }
        }
        return best
    }

    fun nearestShelter(medium: Medium, x: Float, y: Float): HidingSpot? {
        var best: HidingSpot? = null
        var bestDistance = Float.MAX_VALUE
        for (s in spotsFor(medium)) {
            if (s.kind == SpotKind.FLOWER) continue
            val d = dist(x, y, s.x, s.y)
            if (d < bestDistance) {
                bestDistance = d
                best = s
            }
        }
        return best
    }

    companion object {
        fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float {
            val dx = ax - bx
            val dy = ay - by
            return sqrt(dx * dx + dy * dy)
        }

        fun build(location: Location, width: Float, height: Float, dp: Float): SceneLayout {
            val w = width
            val h = height
            val u = dp
            val down = (PI / 2).toFloat()
            val spots = ArrayList<HidingSpot>()
            var water: WaterArea? = null
            var floorTop = 0f
            fun spot(kind: SpotKind, fx: Float, fy: Float, r: Float, medium: Medium, exit: Float = down) =
                HidingSpot(kind, fx * w, fy * h, r * u, medium, exit).also { spots += it }

            when (location) {
                Location.BASEMENT -> {
                    floorTop = (h * 0.16f).coerceIn(90f * u, 170f * u)
                    for (fx in floatArrayOf(0.16f, 0.52f, 0.86f)) {
                        HidingSpot(SpotKind.BURROW, fx * w, floorTop, 20f * u, Medium.GROUND, down)
                            .also { it.clipLine = floorTop; spots += it }
                    }
                    spot(SpotKind.CRATE, 0.14f, 0.6f, 62f, Medium.GROUND, 0f)
                    spot(SpotKind.CRATE, 0.84f, 0.42f, 56f, Medium.GROUND, PI.toFloat())
                    spot(SpotKind.SACK, 0.62f, 0.84f, 52f, Medium.GROUND, -down)
                }
                Location.GARDEN -> {
                    spot(SpotKind.BUSH, 0.1f, 0.12f, 80f, Medium.GROUND)
                    spot(SpotKind.BUSH, 0.92f, 0.7f, 86f, Medium.GROUND, PI.toFloat())
                    spot(SpotKind.BUSH, 0.08f, 0.9f, 70f, Medium.GROUND, 0f)
                    spot(SpotKind.FEEDER, 0.55f, 0.4f, 46f, Medium.GROUND)
                    spot(SpotKind.FEEDER, 0.3f, 0.68f, 30f, Medium.GROUND)
                }
                Location.MEADOW -> {
                    spot(SpotKind.STONE, 0.2f, 0.3f, 42f, Medium.GROUND)
                    spot(SpotKind.STONE, 0.78f, 0.78f, 50f, Medium.GROUND, PI.toFloat())
                    spot(SpotKind.GRASS, 0.85f, 0.2f, 56f, Medium.GROUND, PI.toFloat())
                    spot(SpotKind.GRASS, 0.12f, 0.75f, 60f, Medium.GROUND, 0f)
                    for ((fx, fy) in listOf(0.35f to 0.15f, 0.62f to 0.32f, 0.45f to 0.55f, 0.25f to 0.88f, 0.7f to 0.6f, 0.86f to 0.45f)) {
                        spot(SpotKind.FLOWER, fx, fy, 22f, Medium.AIR)
                    }
                }
                Location.POND -> {
                    water = WaterArea(w / 2, h / 2, w * 0.62f, h * 0.62f)
                    spot(SpotKind.LILY, 0.25f, 0.25f, 46f, Medium.WATER)
                    spot(SpotKind.LILY, 0.72f, 0.5f, 52f, Medium.WATER)
                    spot(SpotKind.LILY, 0.35f, 0.78f, 44f, Medium.WATER)
                    spot(SpotKind.REED, 0.95f, 0.1f, 50f, Medium.WATER, PI.toFloat())
                    spot(SpotKind.REED, 0.04f, 0.6f, 50f, Medium.WATER, 0f)
                }
                Location.MIX -> {
                    water = WaterArea(w * 0.78f, h * 0.8f, w * 0.55f, h * 0.36f)
                    spot(SpotKind.STONE, 0.18f, 0.2f, 46f, Medium.GROUND)
                    HidingSpot(SpotKind.BURROW, 0.18f * w, 0.2f * h + 40f * u, 18f * u, Medium.GROUND, down).also { spots += it }
                    spot(SpotKind.BUSH, 0.85f, 0.12f, 70f, Medium.GROUND, PI.toFloat())
                    spot(SpotKind.GRASS, 0.08f, 0.62f, 54f, Medium.GROUND, 0f)
                    spot(SpotKind.FLOWER, 0.45f, 0.18f, 22f, Medium.AIR)
                    spot(SpotKind.FLOWER, 0.3f, 0.45f, 22f, Medium.AIR)
                    spot(SpotKind.FLOWER, 0.62f, 0.36f, 22f, Medium.AIR)
                    spot(SpotKind.LILY, 0.72f, 0.82f, 44f, Medium.WATER)
                    spot(SpotKind.LILY, 0.9f, 0.62f, 38f, Medium.WATER)
                }
            }
            return SceneLayout(location, w, h, dp, spots, water, floorTop)
        }
    }
}
