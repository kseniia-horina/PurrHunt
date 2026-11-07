package app.purrhunt.game.engine

/** Every sound the simulation can ask for. The audio layer maps cues to files in res/raw. */
enum class SoundCue(val variants: Int = 1, val loop: Boolean = false) {
    MOUSE_SQUEAK(4),
    MOUSE_RUSTLE(2),
    MOUSE_CAUGHT,
    RAT_SQUEAK(2),
    RAT_CAUGHT,
    SPARROW_CHIRP(2),
    TIT_TRILL(2),
    BULLFINCH_CALL,
    WING_FLAP,
    FLY_BUZZ(loop = true),
    CRICKET_CHIRP,
    INSECT_RUSTLE,
    BEETLE_HUM(loop = true),
    WATER_BLOOP,
    WATER_SPLASH,
    BUBBLES,
    FIREFLY_TWINKLE,
    FEATHER_SWISH,
    DOT_CHIME,
}

/** Callbacks from the simulation. Implementations must not block: they run inside the game step. */
interface GameEvents {
    /** [pan] is 0 (left) .. 1 (right), [volume] 0..1. */
    fun onSound(cue: SoundCue, pan: Float, volume: Float)
    fun onCatch(creature: Creature)
    fun onMiss(x: Float, y: Float)
    fun onToyCaught(x: Float, y: Float) = Unit

    object None : GameEvents {
        override fun onSound(cue: SoundCue, pan: Float, volume: Float) = Unit
        override fun onCatch(creature: Creature) = Unit
        override fun onMiss(x: Float, y: Float) = Unit
    }
}
