package app.purrhunt.game.creatures

import app.purrhunt.domain.Species
import app.purrhunt.game.engine.Action
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.engine.CreatureState
import app.purrhunt.game.engine.SoundCue
import app.purrhunt.game.engine.World
import app.purrhunt.game.scenes.SpotKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sparrows, tits and bullfinches: hop on the ground, peck, turn their heads. A close touch makes them
 * flutter up and land somewhere else; sometimes they just fly across the garden.
 * [Creature.aux0] marks a flight that should land (1) or leave the screen (0).
 */
object BirdBehavior : FlightBehavior() {
    private const val HOP_RATE = 4f

    override fun spawn(c: Creature, w: World) {
        placeAtEdge(c, w)
        c.flying = true
        c.altitude = 1f
        landSomewhere(c, w)
    }

    private fun landSomewhere(c: Creature, w: World) {
        val feeders = w.layout.feeders
        if (feeders.isNotEmpty() && w.random.chance(0.35f)) {
            val f = w.random.pick(feeders)
            val a = w.random.range(0f, (2 * PI).toFloat())
            c.setTarget(f.x + cos(a) * f.radius * 0.6f, f.y + sin(a) * f.radius * 0.6f)
        } else {
            w.layout.randomPoint(c.species.medium, w.random, w.scratch)
            c.setTarget(w.scratch[0], w.scratch[1])
        }
        c.flying = true
        c.aux0 = 1f
        c.enter(CreatureState.SPAWN, 8f)
        c.targetSpeed = c.config.fleeSpeed * 0.7f * w.dp * w.difficulty.speedMultiplier
    }

    override fun idleAction(c: Creature, w: World): Action = if (w.random.chance(0.6f)) Action.PECK else Action.LOOK

    override fun update(c: Creature, w: World, dt: Float) {
        tick(c, w, dt)
        when (c.state) {
            CreatureState.SPAWN, CreatureState.FLEE -> {
                val landing = c.aux0 == 1f
                val arrived = fly(c, w, dt, landing)
                if (arrived || (c.stateDone && c.state == CreatureState.FLEE)) {
                    if (landing) land(c, w) else c.active = false
                }
                if (isOutside(c, w) && c.state == CreatureState.FLEE && !landing) c.active = false
            }
            CreatureState.IDLE -> {
                brake(c, dt, 14f)
                if (c.action == Action.PECK && (c.actionTime * 3f).toInt() % 2 == 0) c.aux1 = 1f else c.aux1 = 0f
                if (c.stateDone) decideBird(c, w)
            }
            CreatureState.WANDER, CreatureState.DART -> {
                hop(c, w, dt)
                if (c.stateDone || c.distanceToTarget() < c.length * 0.3f) decideBird(c, w)
            }
            CreatureState.FREEZE -> {
                brake(c, dt, 20f)
                if (c.stateDone) decideBird(c, w)
            }
            CreatureState.HIDE -> {
                val spot = c.spot
                if (spot == null) {
                    decideBird(c, w)
                } else if (c.aux2 == 0f) {
                    if (fly(c, w, dt, landing = true, cruise = 0.7f)) {
                        c.aux2 = 1f
                        c.flying = false
                        c.stateDuration = c.stateTime + c.config.hideTime.sample(w.random)
                        c.catchable = false
                    }
                } else {
                    c.visibility += (0f - c.visibility) * (dt * 5f).coerceAtMost(1f)
                    if (w.bedtime && c.visibility < 0.02f) c.active = false
                    else if (c.stateDone && !w.bedtime) peekFromBush(c, w)
                }
            }
            CreatureState.PEEK -> {
                c.visibility += (0.8f - c.visibility) * (dt * 3f).coerceAtMost(1f)
                c.heading = c.aux3 + sin(c.stateTime * 2.5f) * 0.6f
                if (c.stateDone) {
                    c.heading = c.aux3
                    c.spot = null
                    startWander(c, w)
                }
            }
            CreatureState.CAUGHT -> {
                brake(c, dt)
                updateCaught(c, w, dt)
            }
            CreatureState.EXIT -> {
                c.flying = true
                fly(c, w, dt, landing = false)
                c.visibility = 1f
                if (isOutside(c, w) || c.stateDone) c.active = false
                val spot = c.spot
                if (spot != null && c.distanceTo(spot.x, spot.y) < spot.radius * 0.5f) c.active = false
            }
        }
        if (c.state != CreatureState.HIDE && c.state != CreatureState.PEEK) {
            c.visibility += (1f - c.visibility) * (dt * 3f).coerceAtMost(1f)
        }
    }

    private fun land(c: Creature, w: World) {
        c.flying = false
        c.altitude = 0f
        c.speed = 0f
        c.catchable = true
        startIdle(c, w)
        c.action = Action.LOOK
    }

    private fun decideBird(c: Creature, w: World) {
        if (w.random.chance(c.config.specialChance)) {
            // A flight: relocate within the garden or cross it and leave.
            w.sound(SoundCue.WING_FLAP, c.x, 0.5f)
            if (w.random.chance(0.7f)) {
                landSomewhere(c, w)
                c.enter(CreatureState.FLEE, 6f)
            } else {
                startExit(c, w, preferShelter = false)
            }
            return
        }
        decide(c, w)
    }

    override fun startWander(c: Creature, w: World) {
        super.startWander(c, w)
        // Birds wander in short hops nearby.
        val dx = c.targetX - c.x
        val dy = c.targetY - c.y
        val d = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val reach = c.length * w.random.range(1.5f, 4f)
        if (d > reach) c.setTarget(c.x + dx / d * reach, c.y + dy / d * reach)
    }

    private fun hop(c: Creature, w: World, dt: Float) {
        val phase = (c.stateTime * HOP_RATE * (if (c.state == CreatureState.DART) 1.6f else 1f)) % 1f
        val airborne = phase < 0.45f
        c.altitude = if (airborne) sin(phase / 0.45f * PI.toFloat()) * 0.12f else 0f
        if (airborne) {
            steer(c, w, dt, accel = 30f)
        } else {
            turnTowards(c, c.angleTo(c.targetX, c.targetY), c.config.turnRate * dt)
            c.speed = 0f
        }
    }

    private fun peekFromBush(c: Creature, w: World) {
        val spot = c.spot ?: return
        c.enter(CreatureState.PEEK, c.config.peekTime.sample(w.random))
        c.aux3 = spot.exitAngle + w.random.range(-0.5f, 0.5f)
        c.heading = c.aux3
        c.x = spot.x + cos(c.aux3) * spot.radius * 0.8f
        c.y = spot.y + sin(c.aux3) * spot.radius * 0.8f
        c.catchable = true
        call(c, w, 0.4f)
    }

    override fun startHide(c: Creature, w: World) {
        val spot = w.layout.nearestShelter(c.species.medium, c.x, c.y)
        if (spot == null || spot.kind != SpotKind.BUSH) return startWander(c, w)
        c.spot = spot
        c.setTarget(spot.x, spot.y)
        c.enter(CreatureState.HIDE, 0f)
        c.aux2 = 0f
        c.flying = true
        c.targetSpeed = c.config.fleeSpeed * 0.6f * w.dp
        w.sound(SoundCue.WING_FLAP, c.x, 0.4f)
    }

    override fun startFlee(c: Creature, w: World, fx: Float, fy: Float) {
        w.sound(SoundCue.WING_FLAP, c.x, 0.8f)
        call(c, w, 0.6f)
        c.flying = true
        c.catchable = true
        pointAwayFrom(c, w, fx, fy, w.width * 0.5f)
        c.aux0 = 1f
        c.spot = null
        c.enter(CreatureState.FLEE, 5f)
        c.targetSpeed = c.config.fleeSpeed * w.dp * (0.7f + 0.3f * w.difficulty.speedMultiplier)
    }

    override fun startExit(c: Creature, w: World, preferShelter: Boolean) {
        val spot = if (preferShelter) w.layout.nearestShelter(c.species.medium, c.x, c.y) else null
        c.spot = spot
        if (spot != null) c.setTarget(spot.x, spot.y) else nearestEdge(c, w)
        c.flying = true
        c.aux0 = 0f
        c.enter(CreatureState.EXIT, EXIT_MAX_TIME)
        c.targetSpeed = c.config.fleeSpeed * w.dp
    }

    override fun bedtime(c: Creature, w: World) {
        if (c.isLeaving) return
        startExit(c, w, preferShelter = w.random.chance(0.5f))
    }

    override fun ambientSound(c: Creature, w: World) {
        if (c.state == CreatureState.HIDE && c.aux2 == 1f) return
        call(c, w, 0.45f)
    }

    private fun call(c: Creature, w: World, volume: Float) {
        val cue = when (c.species) {
            Species.TIT -> SoundCue.TIT_TRILL
            Species.BULLFINCH -> SoundCue.BULLFINCH_CALL
            else -> SoundCue.SPARROW_CHIRP
        }
        w.sound(cue, c.x, volume)
    }

    override fun caughtSound(c: Creature, w: World) {
        w.sound(SoundCue.WING_FLAP, c.x, 1f)
        call(c, w, 0.9f)
    }
}
