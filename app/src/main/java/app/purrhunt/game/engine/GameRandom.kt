package app.purrhunt.game.engine

import kotlin.random.Random

/** Seeded random source shared by the whole simulation so runs are reproducible in tests. */
class GameRandom(seed: Long) {
    private val random = Random(seed)

    fun float(): Float = random.nextFloat()
    fun range(min: Float, max: Float): Float = min + (max - min) * random.nextFloat()
    fun int(until: Int): Int = random.nextInt(until)
    fun chance(p: Float): Boolean = random.nextFloat() < p
    fun sign(): Float = if (random.nextBoolean()) 1f else -1f
    fun <T> pick(list: List<T>): T = list[random.nextInt(list.size)]
}
