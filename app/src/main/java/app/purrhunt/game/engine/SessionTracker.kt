package app.purrhunt.game.engine

import app.purrhunt.domain.CaughtPrey
import app.purrhunt.domain.HeatmapGrid
import app.purrhunt.domain.Interaction
import app.purrhunt.domain.Species

/** Collects what a session produces: catches, misses, streaks, touch heatmap and per-species attention. */
class SessionTracker(private val wallStart: Long) {
    private val catchSpecies = ArrayList<Species?>(256)
    private val catchTimes = ArrayList<Long>(256)
    val heatmap = IntArray(HeatmapGrid.COLS * HeatmapGrid.ROWS)
    private val exposure = FloatArray(Species.entries.size)
    private val nearTouches = IntArray(Species.entries.size)

    var misses = 0
        private set
    var streak = 0
        private set
    var bestStreak = 0
        private set
    val catches: Int get() = catchSpecies.size

    fun touch(nx: Float, ny: Float) {
        heatmap[HeatmapGrid.cellIndex(nx, ny)]++
    }

    fun caught(species: Species?, time: Double) {
        catchSpecies.add(species)
        catchTimes.add(wallStart + (time * 1000).toLong())
        streak++
        if (streak > bestStreak) bestStreak = streak
    }

    fun missed() {
        misses++
        streak = 0
    }

    fun near(species: Species) {
        nearTouches[species.ordinal]++
    }

    fun expose(species: Species, dt: Float) {
        exposure[species.ordinal] += dt
    }

    fun caughtList(): List<CaughtPrey> = catchSpecies.indices.map { CaughtPrey(catchSpecies[it], catchTimes[it]) }

    fun caughtRare(): Boolean = catchSpecies.any { it?.rare == true }

    fun interactions(): List<Interaction> = Species.entries
        .filter { exposure[it.ordinal] > 0f }
        .map { Interaction(it, (exposure[it.ordinal] * 1000).toLong(), nearTouches[it.ordinal]) }
}
