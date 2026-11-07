package app.purrhunt.game.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The glowing dot: an automatic "laser" in cat-visible blue-green. It dashes, stops dead, jitters
 * in place and sometimes slips behind a screen edge to reappear somewhere else.
 */
class LaserDot {
    enum class Move { DASH, PAUSE, OFFSCREEN }

    var x = 0f
    var y = 0f
    var move = Move.PAUSE
        private set
    /** 0..1 flash after a catch. */
    var flash = 0f
        private set
    var visible = true
        private set
    var trailX = 0f
    var trailY = 0f
    private var tx = 0f
    private var ty = 0f
    private var timer = 1f
    private var speed = 0f
    private var sleeping = false

    val radiusDp = 9f

    fun place(px: Float, py: Float) {
        x = px; y = py; tx = px; ty = py; trailX = px; trailY = py
    }

    fun rescale(sx: Float, sy: Float) {
        x *= sx; y *= sy; tx *= sx; ty *= sy; trailX *= sx; trailY *= sy
    }

    fun sleep() {
        sleeping = true
        tx = x
        ty = -200f
        move = Move.DASH
        speed = 600f
    }

    fun update(w: World, dt: Float) {
        flash = (flash - dt * 2.5f).coerceAtLeast(0f)
        // The trail lags behind the dot, which makes fast dashes read as streaks.
        trailX += (x - trailX) * (dt * 18f).coerceAtMost(1f)
        trailY += (y - trailY) * (dt * 18f).coerceAtMost(1f)
        val dp = w.dp
        val mult = w.difficulty.speedMultiplier
        timer -= dt
        when (move) {
            Move.DASH -> {
                val dx = tx - x
                val dy = ty - y
                val d = sqrt(dx * dx + dy * dy)
                val step = speed * dp * mult * dt
                if (d <= step) {
                    x = tx; y = ty
                    if (sleeping) { visible = false; return }
                    if (x < 0 || y < 0 || x > w.width || y > w.height) {
                        move = Move.OFFSCREEN
                        visible = false
                        timer = w.random.range(0.8f, 2f)
                    } else {
                        move = Move.PAUSE
                        timer = w.random.range(0.25f, 1.6f) * w.difficulty.freezeMultiplier
                    }
                } else {
                    x += dx / d * step
                    y += dy / d * step
                }
            }
            Move.PAUSE -> {
                // Tiny tremor like a hand-held pointer.
                x += sin(w.time.toFloat() * 31f) * 0.6f * dp
                y += cos(w.time.toFloat() * 27f) * 0.6f * dp
                if (timer <= 0f && !sleeping) nextTarget(w)
            }
            Move.OFFSCREEN -> if (timer <= 0f && !sleeping) {
                // Re-enter from a random edge.
                when (w.random.int(4)) {
                    0 -> { x = -20f * dp; y = w.random.range(0f, w.height) }
                    1 -> { x = w.width + 20f * dp; y = w.random.range(0f, w.height) }
                    2 -> { x = w.random.range(0f, w.width); y = -20f * dp }
                    else -> { x = w.random.range(0f, w.width); y = w.height + 20f * dp }
                }
                trailX = x; trailY = y
                visible = true
                nextTarget(w)
            }
        }
    }

    private fun nextTarget(w: World) {
        val dp = w.dp
        if (w.random.chance(0.12f)) {
            // Run behind an edge.
            val a = w.random.range(0f, (2 * PI).toFloat())
            tx = (w.width / 2 + cos(a) * w.width) .coerceIn(-40f * dp, w.width + 40f * dp)
            ty = (w.height / 2 + sin(a) * w.height).coerceIn(-40f * dp, w.height + 40f * dp)
        } else if (w.random.chance(0.4f)) {
            // Short hop nearby.
            tx = (x + w.random.range(-120f, 120f) * dp).coerceIn(30f * dp, w.width - 30f * dp)
            ty = (y + w.random.range(-120f, 120f) * dp).coerceIn(30f * dp, w.height - 30f * dp)
        } else {
            tx = w.random.range(30f * dp, w.width - 30f * dp)
            ty = w.random.range(30f * dp, w.height - 30f * dp)
        }
        speed = w.random.range(500f, 1100f)
        move = Move.DASH
    }

    /** Returns true when the paw covers the dot. */
    fun touch(w: World, px: Float, py: Float, slop: Float): Boolean {
        if (!visible || sleeping) return false
        val dx = px - x
        val dy = py - y
        val hit = sqrt(dx * dx + dy * dy) <= radiusDp * w.dp + slop
        if (hit) {
            flash = 1f
            w.sound(SoundCue.DOT_CHIME, x, 0.6f)
            // Escape far away.
            val a = kotlin.math.atan2(y - py, x - px)
            tx = (x + cos(a) * w.width * 0.5f).coerceIn(30f * w.dp, w.width - 30f * w.dp)
            ty = (y + sin(a) * w.height * 0.4f).coerceIn(30f * w.dp, w.height - 30f * w.dp)
            speed = 1300f
            move = Move.DASH
        }
        return hit
    }
}

/**
 * Feather on a string. The first touch-and-hold is the owner's hand: the rod tip follows it with a lag
 * and the string swings with inertia (verlet rope). Any other touch is the cat trying to catch it.
 */
class FeatherToy {
    val segments = 9
    val xs = FloatArray(segments)
    val ys = FloatArray(segments)
    private val pxs = FloatArray(segments)
    private val pys = FloatArray(segments)
    private var segLen = 10f

    var ownerId = -1
        private set
    private var handX = 0f
    private var handY = 0f
    var tipX = 0f
        private set
    var tipY = 0f
        private set
    private var idleTime = 0f
    private var cooldown = 0f
    private var sleeping = false
    /** 0..1 shake after a catch. */
    var flutter = 0f
        private set

    val featherX: Float get() = xs[segments - 1]
    val featherY: Float get() = ys[segments - 1]
    val featherAngle: Float
        get() = kotlin.math.atan2(ys[segments - 1] - ys[segments - 2], xs[segments - 1] - xs[segments - 2])

    fun place(px: Float, py: Float, dp: Float) {
        segLen = 18f * dp
        handX = px; handY = py; tipX = px; tipY = py
        for (i in 0 until segments) {
            xs[i] = px; ys[i] = py + i * segLen
            pxs[i] = xs[i]; pys[i] = ys[i]
        }
    }

    fun rescale(sx: Float, sy: Float) {
        handX *= sx; handY *= sy; tipX *= sx; tipY *= sy
        for (i in 0 until segments) { xs[i] *= sx; ys[i] *= sy; pxs[i] *= sx; pys[i] *= sy }
    }

    fun sleep() {
        sleeping = true
        ownerId = -1
    }

    fun isOwner(id: Int) = id == ownerId

    /** The first touch away from the feather becomes the owner's hand. Returns true if claimed. */
    fun claimOwner(id: Int, x: Float, y: Float, w: World): Boolean {
        if (ownerId != -1 || sleeping) return false
        val dx = x - featherX
        val dy = y - featherY
        if (sqrt(dx * dx + dy * dy) < FEATHER_DP * 2.5f * w.dp) return false
        ownerId = id
        handX = x
        handY = y
        return true
    }

    fun moveOwner(x: Float, y: Float) {
        handX = x
        handY = y
    }

    fun releaseOwner(id: Int) {
        if (id == ownerId) {
            ownerId = -1
            idleTime = 0f
        }
    }

    fun update(w: World, dt: Float) {
        cooldown -= dt
        flutter = (flutter - dt * 2f).coerceAtLeast(0f)
        if (sleeping) {
            handY -= 500f * w.dp * dt
        } else if (ownerId == -1) {
            // Without a hand the feather drifts in a lazy figure eight so it never lies dead.
            idleTime += dt
            handX += cos(idleTime * 0.9f) * 40f * w.dp * dt
            handY += sin(idleTime * 1.8f) * 30f * w.dp * dt
            handX = handX.coerceIn(40f * w.dp, w.width - 40f * w.dp)
            handY = handY.coerceIn(40f * w.dp, w.height - 120f * w.dp)
        }
        // The rod tip lags behind the hand.
        val follow = (dt * 7f).coerceAtMost(1f)
        tipX += (handX - tipX) * follow
        tipY += (handY - tipY) * follow

        xs[0] = tipX
        ys[0] = tipY
        for (i in 1 until segments) {
            val vx = (xs[i] - pxs[i]) * 0.97f
            val vy = (ys[i] - pys[i]) * 0.97f
            pxs[i] = xs[i]
            pys[i] = ys[i]
            xs[i] += vx
            ys[i] += vy + 0.25f * w.dp // a little drag towards the bottom of the screen
        }
        for (iteration in 0 until 3) {
            for (i in 1 until segments) {
                val dx = xs[i] - xs[i - 1]
                val dy = ys[i] - ys[i - 1]
                val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
                val k = segLen / d
                xs[i] = xs[i - 1] + dx * k
                ys[i] = ys[i - 1] + dy * k
            }
        }
    }

    fun touch(w: World, px: Float, py: Float, slop: Float): Boolean {
        if (cooldown > 0f || sleeping) return false
        // Aim at the middle of the vane, not the knot on the string.
        val a = featherAngle
        val cx = featherX + cos(a) * LENGTH_DP * 0.5f * w.dp
        val cy = featherY + sin(a) * LENGTH_DP * 0.5f * w.dp
        val dx = px - cx
        val dy = py - cy
        val hit = sqrt(dx * dx + dy * dy) <= FEATHER_DP * w.dp + slop
        if (hit) {
            cooldown = 0.5f
            flutter = 1f
            w.sound(SoundCue.FEATHER_SWISH, featherX, 0.7f)
            // The feather jerks away from the paw.
            val last = segments - 1
            pxs[last] = xs[last] + dx * 0.25f
            pys[last] = ys[last] + dy * 0.25f
        }
        return hit
    }

    companion object {
        const val FEATHER_DP = 34f
        const val LENGTH_DP = 84f
    }
}
