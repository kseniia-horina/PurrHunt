package app.purrhunt.game.engine

/**
 * Keeps the hunt winnable. Accuracy (catches / attempts) over the last [WINDOW] seconds slowly moves a
 * level factor: below 20 % prey gets slower, darts less and freezes longer (down to −40 %),
 * above 60 % it gets harder (up to +30 %).
 */
class AdaptiveDifficulty(
    private val baseSpeed: Float,
    private val enabled: Boolean,
) {
    private val times = DoubleArray(CAPACITY)
    private val hits = BooleanArray(CAPACITY)
    private var head = 0
    private var size = 0

    /** Adaptive factor, 1 = neutral. */
    var level = 1f
        private set

    val speedMultiplier: Float get() = baseSpeed * level
    /** Weight multiplier for darting. */
    val dartMultiplier: Float get() = level
    /** Freeze durations grow when the game gets easier. */
    val freezeMultiplier: Float get() = 1f / level

    fun recordAttempt(time: Double, hit: Boolean) {
        times[head] = time
        hits[head] = hit
        head = (head + 1) % CAPACITY
        if (size < CAPACITY) size++
    }

    /** Accuracy over the window, or NaN while there are too few attempts to judge. */
    fun accuracy(now: Double): Float {
        var attempts = 0
        var caught = 0
        for (i in 0 until size) {
            val idx = (head - 1 - i + CAPACITY) % CAPACITY
            if (now - times[idx] > WINDOW) break
            attempts++
            if (hits[idx]) caught++
        }
        return if (attempts < MIN_ATTEMPTS) Float.NaN else caught.toFloat() / attempts
    }

    fun update(now: Double, dt: Float) {
        if (!enabled) {
            level = 1f
            return
        }
        val acc = accuracy(now)
        if (acc.isNaN()) return
        when {
            acc < LOW_ACCURACY -> level = (level - EASE_RATE * dt).coerceAtLeast(MIN_LEVEL)
            acc > HIGH_ACCURACY -> level = (level + HARDEN_RATE * dt).coerceAtMost(MAX_LEVEL)
        }
    }

    companion object {
        const val WINDOW = 30.0
        const val MIN_ATTEMPTS = 4
        const val LOW_ACCURACY = 0.2f
        const val HIGH_ACCURACY = 0.6f
        const val MIN_LEVEL = 0.6f
        const val MAX_LEVEL = 1.3f
        /** Level change per second; full easing takes about 20 s. */
        const val EASE_RATE = 0.02f
        const val HARDEN_RATE = 0.012f
        private const val CAPACITY = 256
    }
}
