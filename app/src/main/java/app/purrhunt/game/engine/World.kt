package app.purrhunt.game.engine

import app.purrhunt.domain.Family
import app.purrhunt.domain.Medium
import app.purrhunt.domain.Species
import app.purrhunt.game.creatures.Behaviors
import app.purrhunt.game.scenes.SceneLayout

/** Everything the creatures can see: layout, difficulty, random source, effects and other creatures. */
class World(
    val random: GameRandom,
    var events: GameEvents,
    val difficulty: AdaptiveDifficulty,
    var layout: SceneLayout,
) {
    val creatures = Array(MAX_CREATURES) { Creature(it) }
    val ripples = EffectPool(24)
    val marks = EffectPool(16)
    /** Scratch buffer for point queries so behaviours never allocate. */
    val scratch = FloatArray(2)

    var time = 0.0
    var bedtime = false

    val width: Float get() = layout.width
    val height: Float get() = layout.height
    val dp: Float get() = layout.dp

    fun behaviorOf(c: Creature): Behavior = Behaviors.of(c.species.family)

    fun activeCount(): Int {
        var n = 0
        for (i in creatures.indices) if (creatures[i].active) n++
        return n
    }

    fun obtain(): Creature? {
        for (i in creatures.indices) if (!creatures[i].active) return creatures[i]
        return null
    }

    fun spawn(species: Species): Creature? {
        val c = obtain() ?: return null
        c.reset(species, species.sizeDp * dp, random.int(Int.MAX_VALUE))
        behaviorOf(c).spawn(c, this)
        return c
    }

    fun step(dt: Float) {
        time += dt
        difficulty.update(time, dt)
        for (i in creatures.indices) {
            val c = creatures[i]
            if (c.active) behaviorOf(c).update(c, this, dt)
        }
        ripples.update(dt)
        marks.update(dt)
    }

    fun startBedtime() {
        if (bedtime) return
        bedtime = true
        for (i in creatures.indices) {
            val c = creatures[i]
            if (c.active) behaviorOf(c).bedtime(c, this)
        }
    }

    /** Equal-power stereo position and distance falloff are applied by the audio layer. */
    fun sound(cue: SoundCue, x: Float, volume: Float) {
        events.onSound(cue, (x / width).coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
    }

    fun ripple(x: Float, y: Float, size: Float) {
        if (layout.isWater(x, y)) ripples.add(x, y, size, 1.2f)
    }

    fun isWaterCreature(s: Species) = s.medium == Medium.WATER || s.medium == Medium.WATER_SURFACE

    fun isFlying(s: Species) = s.medium == Medium.AIR || s.family == Family.BIRD

    companion object {
        const val MAX_CREATURES = 8
    }
}

/** Fixed-size pool of short-lived effects (water ripples, paw prints). */
class EffectPool(val capacity: Int) {
    val xs = FloatArray(capacity)
    val ys = FloatArray(capacity)
    val sizes = FloatArray(capacity)
    val ages = FloatArray(capacity) { Float.MAX_VALUE }
    val lifetimes = FloatArray(capacity) { 1f }
    val angles = FloatArray(capacity)
    private var next = 0

    fun add(x: Float, y: Float, size: Float, lifetime: Float, angle: Float = 0f) {
        xs[next] = x
        ys[next] = y
        sizes[next] = size
        ages[next] = 0f
        lifetimes[next] = lifetime
        angles[next] = angle
        next = (next + 1) % capacity
    }

    fun update(dt: Float) {
        for (i in 0 until capacity) if (ages[i] < lifetimes[i]) ages[i] += dt
    }

    fun alive(i: Int): Boolean = ages[i] < lifetimes[i]

    /** 0 at birth .. 1 at the end of life. */
    fun progress(i: Int): Float = (ages[i] / lifetimes[i]).coerceIn(0f, 1f)

    fun clear() {
        ages.fill(Float.MAX_VALUE)
    }
}
