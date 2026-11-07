package app.purrhunt.game.engine

/** Shared state machine of every creature: Spawn → Idle → Wander → Dart → Freeze → Hide/Peek → Flee → Caught → Exit. */
enum class CreatureState { SPAWN, IDLE, WANDER, DART, FREEZE, HIDE, PEEK, FLEE, CAUGHT, EXIT }

/** Idle animations layered on top of a state. */
enum class Action { NONE, GROOM, SNIFF, PECK, LOOK, RUB_LEGS, CHIRP, FLIPPED, OPEN_WINGS, FOLD_WINGS, PERCHED }

/** Inclusive random range. */
data class Span(val min: Float, val max: Float) {
    fun sample(r: GameRandom): Float = r.range(min, max)
    fun scaled(k: Float) = Span(min * k, max * k)
}
