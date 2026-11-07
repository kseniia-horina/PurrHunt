package app.purrhunt.game.engine

import app.purrhunt.domain.Species

/**
 * Tunable behaviour of one species. Speeds are in dp per second, times in seconds,
 * weights are relative chances of picking the next state after the current one ends.
 */
data class BehaviorConfig(
    val walkSpeed: Float,
    val dartSpeed: Float,
    val fleeSpeed: Float,
    /** Max turning speed in radians per second. */
    val turnRate: Float,
    val idleTime: Span,
    val wanderTime: Span,
    val dartTime: Span,
    val freezeTime: Span,
    val hideTime: Span,
    val peekTime: Span,
    val weightIdle: Float,
    val weightWander: Float,
    val weightDart: Float,
    val weightFreeze: Float,
    val weightHide: Float,
    /** 0..1: how much wander targets stick to walls and objects. */
    val edgeAffinity: Float = 0f,
    /** Chance that an idle period plays a special animation (grooming, pecking, rubbing legs...). */
    val actionChance: Float = 0.5f,
    /** Seconds between ambient sounds while visible. */
    val soundInterval: Span = Span(3f, 8f),
    /** Body wobble frequency (steps, wing beats, tail beats) per second at walk speed. */
    val gaitRate: Float = 6f,
    /** Chance per decision of a fly-over / special move (birds crossing the screen, beetle taking off). */
    val specialChance: Float = 0f,
)

object SpeciesConfigs {
    private val mouse = BehaviorConfig(
        walkSpeed = 70f, dartSpeed = 360f, fleeSpeed = 520f, turnRate = 9f,
        idleTime = Span(0.6f, 1.8f), wanderTime = Span(1f, 2.5f), dartTime = Span(0.25f, 0.6f),
        freezeTime = Span(0.5f, 2f), hideTime = Span(1.5f, 4f), peekTime = Span(1f, 2.5f),
        weightIdle = 1.2f, weightWander = 2f, weightDart = 2.4f, weightFreeze = 1.6f, weightHide = 0.6f,
        edgeAffinity = 0.7f, actionChance = 0.55f, soundInterval = Span(2.5f, 6f), gaitRate = 9f,
    )

    private val configs: Map<Species, BehaviorConfig> = mapOf(
        Species.GRAY_MOUSE to mouse,
        Species.WHITE_MOUSE to mouse.copy(weightHide = 0.4f),
        Species.MOUSELET to mouse.copy(
            walkSpeed = 90f, dartSpeed = 440f, fleeSpeed = 600f, turnRate = 12f,
            freezeTime = Span(0.4f, 1.4f), weightDart = 3f, gaitRate = 12f,
        ),
        Species.RAT to mouse.copy(
            walkSpeed = 55f, dartSpeed = 420f, fleeSpeed = 480f, turnRate = 6f,
            dartTime = Span(0.2f, 0.4f), idleTime = Span(1f, 2.5f), wanderTime = Span(1.5f, 3f),
            weightDart = 1.8f, weightWander = 2.4f, weightFreeze = 1.2f, gaitRate = 7f,
        ),

        Species.SPARROW to BehaviorConfig(
            walkSpeed = 60f, dartSpeed = 140f, fleeSpeed = 560f, turnRate = 10f,
            idleTime = Span(0.5f, 1.5f), wanderTime = Span(0.6f, 1.6f), dartTime = Span(0.15f, 0.3f),
            freezeTime = Span(0.4f, 1.2f), hideTime = Span(2f, 4f), peekTime = Span(1f, 2f),
            weightIdle = 2.4f, weightWander = 2f, weightDart = 1.2f, weightFreeze = 0.8f, weightHide = 0.3f,
            actionChance = 0.8f, soundInterval = Span(1.5f, 4f), gaitRate = 7f, specialChance = 0.08f,
        ),
        Species.TIT to BehaviorConfig(
            walkSpeed = 70f, dartSpeed = 160f, fleeSpeed = 600f, turnRate = 12f,
            idleTime = Span(0.4f, 1.2f), wanderTime = Span(0.5f, 1.3f), dartTime = Span(0.15f, 0.3f),
            freezeTime = Span(0.3f, 1f), hideTime = Span(2f, 4f), peekTime = Span(1f, 2f),
            weightIdle = 2.2f, weightWander = 2f, weightDart = 1.4f, weightFreeze = 0.6f, weightHide = 0.3f,
            actionChance = 0.8f, soundInterval = Span(1.5f, 4f), gaitRate = 8f, specialChance = 0.1f,
        ),
        Species.BULLFINCH to BehaviorConfig(
            walkSpeed = 45f, dartSpeed = 110f, fleeSpeed = 500f, turnRate = 7f,
            idleTime = Span(0.8f, 2f), wanderTime = Span(0.6f, 1.5f), dartTime = Span(0.15f, 0.3f),
            freezeTime = Span(0.6f, 1.6f), hideTime = Span(2f, 4f), peekTime = Span(1f, 2f),
            weightIdle = 2.8f, weightWander = 1.6f, weightDart = 0.8f, weightFreeze = 1f, weightHide = 0.3f,
            actionChance = 0.8f, soundInterval = Span(2.5f, 5f), gaitRate = 6f, specialChance = 0.06f,
        ),

        Species.BUTTERFLY to BehaviorConfig(
            walkSpeed = 90f, dartSpeed = 150f, fleeSpeed = 320f, turnRate = 3f,
            idleTime = Span(1.5f, 4f), wanderTime = Span(2f, 5f), dartTime = Span(0.5f, 1f),
            freezeTime = Span(1f, 2.5f), hideTime = Span(1f, 2f), peekTime = Span(1f, 2f),
            weightIdle = 1.4f, weightWander = 3f, weightDart = 0.8f, weightFreeze = 0.6f, weightHide = 0f,
            actionChance = 0.7f, soundInterval = Span(4f, 9f), gaitRate = 7f,
        ),
        Species.SWALLOWTAIL to BehaviorConfig(
            walkSpeed = 80f, dartSpeed = 140f, fleeSpeed = 300f, turnRate = 2.5f,
            idleTime = Span(2f, 4f), wanderTime = Span(2f, 5f), dartTime = Span(0.5f, 1f),
            freezeTime = Span(1f, 2.5f), hideTime = Span(1f, 2f), peekTime = Span(1f, 2f),
            weightIdle = 1.6f, weightWander = 3f, weightDart = 0.6f, weightFreeze = 0.6f, weightHide = 0f,
            actionChance = 0.7f, soundInterval = Span(4f, 9f), gaitRate = 5.5f,
        ),
        Species.FLY to BehaviorConfig(
            walkSpeed = 30f, dartSpeed = 420f, fleeSpeed = 520f, turnRate = 18f,
            idleTime = Span(0.8f, 2.5f), wanderTime = Span(0.8f, 2f), dartTime = Span(0.12f, 0.35f),
            freezeTime = Span(0.5f, 1.5f), hideTime = Span(1f, 2f), peekTime = Span(1f, 2f),
            weightIdle = 1.2f, weightWander = 1.4f, weightDart = 3f, weightFreeze = 0.8f, weightHide = 0f,
            actionChance = 0.7f, soundInterval = Span(1f, 2f), gaitRate = 40f,
        ),
        Species.LADYBUG to BehaviorConfig(
            walkSpeed = 28f, dartSpeed = 55f, fleeSpeed = 120f, turnRate = 4f,
            idleTime = Span(0.8f, 2f), wanderTime = Span(2f, 4f), dartTime = Span(0.5f, 1f),
            freezeTime = Span(0.8f, 2f), hideTime = Span(2f, 4f), peekTime = Span(1f, 2f),
            weightIdle = 1f, weightWander = 3f, weightDart = 1f, weightFreeze = 1f, weightHide = 0.4f,
            actionChance = 0.3f, soundInterval = Span(4f, 8f), gaitRate = 10f, specialChance = 0.05f,
        ),
        Species.BEETLE to BehaviorConfig(
            walkSpeed = 45f, dartSpeed = 110f, fleeSpeed = 170f, turnRate = 5f,
            idleTime = Span(0.6f, 1.5f), wanderTime = Span(1.5f, 3f), dartTime = Span(0.4f, 0.8f),
            freezeTime = Span(0.6f, 1.6f), hideTime = Span(2f, 4f), peekTime = Span(1f, 2f),
            weightIdle = 1f, weightWander = 3f, weightDart = 1.6f, weightFreeze = 1f, weightHide = 0.5f,
            actionChance = 0.35f, soundInterval = Span(4f, 8f), gaitRate = 12f, specialChance = 0.12f,
        ),
        Species.CRICKET to BehaviorConfig(
            walkSpeed = 25f, dartSpeed = 260f, fleeSpeed = 320f, turnRate = 6f,
            idleTime = Span(1.2f, 3f), wanderTime = Span(0.5f, 1.2f), dartTime = Span(0.45f, 0.6f),
            freezeTime = Span(0.6f, 1.6f), hideTime = Span(2f, 4f), peekTime = Span(1f, 2f),
            weightIdle = 2f, weightWander = 0.8f, weightDart = 2.4f, weightFreeze = 1f, weightHide = 0.3f,
            actionChance = 0.8f, soundInterval = Span(1.5f, 3.5f), gaitRate = 10f,
        ),

        Species.CRUCIAN to BehaviorConfig(
            walkSpeed = 55f, dartSpeed = 260f, fleeSpeed = 380f, turnRate = 2.6f,
            idleTime = Span(1f, 2.5f), wanderTime = Span(2f, 4.5f), dartTime = Span(0.3f, 0.6f),
            freezeTime = Span(0.8f, 2f), hideTime = Span(1.5f, 3.5f), peekTime = Span(1f, 2f),
            weightIdle = 1f, weightWander = 3f, weightDart = 1.2f, weightFreeze = 0.8f, weightHide = 0.7f,
            actionChance = 0.3f, soundInterval = Span(4f, 9f), gaitRate = 2.2f,
        ),
        Species.GOLDFISH to BehaviorConfig(
            walkSpeed = 50f, dartSpeed = 240f, fleeSpeed = 360f, turnRate = 2.4f,
            idleTime = Span(1f, 2.5f), wanderTime = Span(2f, 4.5f), dartTime = Span(0.3f, 0.6f),
            freezeTime = Span(0.8f, 2f), hideTime = Span(1.5f, 3.5f), peekTime = Span(1f, 2f),
            weightIdle = 1f, weightWander = 3f, weightDart = 1f, weightFreeze = 0.8f, weightHide = 0.6f,
            actionChance = 0.3f, soundInterval = Span(4f, 9f), gaitRate = 2f,
        ),
        Species.TADPOLE to BehaviorConfig(
            walkSpeed = 40f, dartSpeed = 300f, fleeSpeed = 360f, turnRate = 8f,
            idleTime = Span(0.5f, 1.5f), wanderTime = Span(0.6f, 1.5f), dartTime = Span(0.2f, 0.45f),
            freezeTime = Span(0.4f, 1.2f), hideTime = Span(1.5f, 3f), peekTime = Span(1f, 2f),
            weightIdle = 1f, weightWander = 1.4f, weightDart = 3f, weightFreeze = 1f, weightHide = 0.4f,
            actionChance = 0.2f, soundInterval = Span(4f, 9f), gaitRate = 9f,
        ),
        Species.WATER_STRIDER to BehaviorConfig(
            walkSpeed = 20f, dartSpeed = 330f, fleeSpeed = 420f, turnRate = 7f,
            idleTime = Span(0.6f, 2f), wanderTime = Span(0.3f, 0.6f), dartTime = Span(0.15f, 0.3f),
            freezeTime = Span(0.6f, 2f), hideTime = Span(1.5f, 3f), peekTime = Span(1f, 2f),
            weightIdle = 1.4f, weightWander = 0.6f, weightDart = 3f, weightFreeze = 1.2f, weightHide = 0f,
            actionChance = 0.2f, soundInterval = Span(3f, 7f), gaitRate = 4f,
        ),

        Species.FIREFLY to BehaviorConfig(
            walkSpeed = 45f, dartSpeed = 90f, fleeSpeed = 200f, turnRate = 2f,
            idleTime = Span(1f, 2.5f), wanderTime = Span(2f, 5f), dartTime = Span(0.5f, 1f),
            freezeTime = Span(1f, 2.5f), hideTime = Span(1f, 2f), peekTime = Span(1f, 2f),
            weightIdle = 1f, weightWander = 3f, weightDart = 0.6f, weightFreeze = 1.2f, weightHide = 0f,
            actionChance = 0.3f, soundInterval = Span(5f, 10f), gaitRate = 30f,
        ),
        Species.GIANT_FIREFLY to BehaviorConfig(
            walkSpeed = 40f, dartSpeed = 80f, fleeSpeed = 180f, turnRate = 1.8f,
            idleTime = Span(1f, 2.5f), wanderTime = Span(2f, 5f), dartTime = Span(0.5f, 1f),
            freezeTime = Span(1f, 2.5f), hideTime = Span(1f, 2f), peekTime = Span(1f, 2f),
            weightIdle = 1f, weightWander = 3f, weightDart = 0.6f, weightFreeze = 1.2f, weightHide = 0f,
            actionChance = 0.3f, soundInterval = Span(5f, 10f), gaitRate = 26f,
        ),
    )

    fun of(species: Species): BehaviorConfig = configs.getValue(species)
}
