package app.purrhunt.domain

data class CaughtPrey(val species: Species, val timestamp: Long)

/** Everything the game reports when a session ends. */
data class SessionResult(
    val catId: Long,
    val startedAt: Long,
    val durationMs: Long,
    val location: Location,
    val night: Boolean,
    val mode: GameMode,
    val catches: List<CaughtPrey>,
    val misses: Int,
    val bestStreak: Int,
    /** Touch counts on a [HeatmapGrid.COLS] x [HeatmapGrid.ROWS] grid. */
    val heatmap: IntArray,
    val interactions: List<Interaction>,
)

data class SessionSummary(
    val sessionId: Long,
    val durationMs: Long,
    val location: Location,
    val night: Boolean,
    val mode: GameMode,
    val catches: Int,
    val misses: Int,
    val perSpecies: Map<Species, Int>,
    val stars: Int,
    val rare: List<Species>,
    val newAchievements: List<Achievement>,
) {
    val accuracy: Float get() = if (catches + misses == 0) 0f else catches.toFloat() / (catches + misses)
}

object HeatmapGrid {
    const val COLS = 12
    const val ROWS = 20

    fun cellIndex(nx: Float, ny: Float): Int {
        val c = (nx * COLS).toInt().coerceIn(0, COLS - 1)
        val r = (ny * ROWS).toInt().coerceIn(0, ROWS - 1)
        return r * COLS + c
    }

    fun encode(cells: IntArray): String = cells.joinToString(",")

    fun decode(s: String): IntArray {
        val out = IntArray(COLS * ROWS)
        if (s.isBlank()) return out
        s.split(',').forEachIndexed { i, v -> if (i < out.size) out[i] = v.toIntOrNull() ?: 0 }
        return out
    }

    fun sum(maps: List<IntArray>): IntArray {
        val out = IntArray(COLS * ROWS)
        for (m in maps) for (i in out.indices) if (i < m.size) out[i] += m[i]
        return out
    }
}
