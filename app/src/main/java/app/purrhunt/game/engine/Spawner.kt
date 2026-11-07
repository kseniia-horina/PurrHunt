package app.purrhunt.game.engine

import app.purrhunt.domain.Species

/**
 * Keeps [targetCount] creatures on stage. A creature that left is replaced after 1–3 s.
 * Rare prey has a [RARE_CHANCE] chance per spawn and appears at most once per session.
 */
class Spawner(
    private val common: List<Species>,
    private val rare: List<Species>,
    var targetCount: Int,
    private val favorite: Species?,
) {
    private val timers = FloatArray(World.MAX_CREATURES)
    private var pending = 0
    var rareSpawned = false
        private set
    private val weights = FloatArray(common.size)

    init {
        for (i in common.indices) weights[i] = if (common[i] == favorite) FAVORITE_WEIGHT else 1f
    }

    fun update(w: World, dt: Float) {
        if (w.bedtime) return
        var i = 0
        while (i < pending) {
            timers[i] -= dt
            if (timers[i] <= 0f) {
                w.spawn(pick(w.random))
                timers[i] = timers[pending - 1]
                pending--
            } else {
                i++
            }
        }
        val missing = targetCount - w.activeCount() - pending
        for (k in 0 until missing) {
            if (pending >= timers.size) break
            timers[pending++] = if (w.time < 2.0) w.random.range(0.2f, 1.6f) else w.random.range(MIN_DELAY, MAX_DELAY)
        }
    }

    fun pick(random: GameRandom): Species {
        if (!rareSpawned && rare.isNotEmpty() && random.chance(RARE_CHANCE)) {
            rareSpawned = true
            return random.pick(rare)
        }
        var total = 0f
        for (wgt in weights) total += wgt
        var r = random.float() * total
        for (i in common.indices) {
            r -= weights[i]
            if (r < 0f) return common[i]
        }
        return common.last()
    }

    companion object {
        const val RARE_CHANCE = 0.03f
        const val FAVORITE_WEIGHT = 2.5f
        const val MIN_DELAY = 1f
        const val MAX_DELAY = 3f
    }
}
