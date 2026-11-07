package app.purrhunt.game.creatures

import app.purrhunt.domain.Species
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.engine.CreatureState
import app.purrhunt.game.engine.SoundCue
import app.purrhunt.game.engine.World
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fish and tadpoles: smooth gliding with body waves, a sharp turn and burst when a paw lands near,
 * and hiding under lily pads. Tadpoles use faster, wigglier darts.
 */
object FishBehavior : FlightBehavior() {
    override fun spawn(c: Creature, w: World) {
        val shelters = w.layout.spotsFor(c.species.medium)
        if (shelters.isNotEmpty() && w.random.chance(0.6f)) {
            val s = w.random.pick(shelters)
            c.x = s.x
            c.y = s.y
            c.heading = w.random.range(-3.14f, 3.14f)
            c.spot = s
            c.visibility = 0f
            c.enter(CreatureState.HIDE, 0f)
            c.aux2 = 1f
            c.stateDuration = w.random.range(0.3f, 1.2f)
            c.catchable = false
        } else {
            w.layout.randomPoint(c.species.medium, w.random, w.scratch)
            c.x = w.scratch[0]
            c.y = w.scratch[1]
            c.heading = w.random.range(-3.14f, 3.14f)
            c.visibility = 0f
            startWander(c, w)
        }
    }

    override fun update(c: Creature, w: World, dt: Float) {
        tick(c, w, dt)
        when (c.state) {
            CreatureState.SPAWN -> decide(c, w)
            CreatureState.WANDER -> {
                val arrived = steer(c, w, dt, arrive = c.length * 0.5f, accel = 2f)
                if (arrived || c.stateDone) decide(c, w)
            }
            CreatureState.DART -> {
                val arrived = steer(c, w, dt, arrive = c.length * 0.5f, accel = 10f)
                if (w.random.chance(dt * 3f)) w.ripple(c.x, c.y, c.length * 0.5f)
                if (arrived || c.stateDone) if (w.random.chance(0.5f)) startFreeze(c, w) else decide(c, w)
            }
            CreatureState.IDLE, CreatureState.FREEZE -> {
                // Hovering: fins keep it nearly still, a slow drift remains.
                brake(c, dt, 3f)
                c.heading = wrap(c.heading + sin(c.age * 0.8f + c.seed) * dt * 0.3f)
                if (c.stateDone) decide(c, w)
            }
            CreatureState.HIDE -> {
                val spot = c.spot
                if (spot == null) {
                    decide(c, w)
                } else if (c.aux2 == 0f) {
                    if (steer(c, w, dt, arrive = spot.radius * 0.4f, accel = 4f)) {
                        c.aux2 = 1f
                        c.catchable = false
                        c.stateDuration = c.stateTime + c.config.hideTime.sample(w.random)
                    }
                } else {
                    brake(c, dt, 4f)
                    c.visibility += (0.0f - c.visibility) * (dt * 3f).coerceAtMost(1f)
                    if (w.bedtime && c.visibility < 0.02f) c.active = false
                    else if (c.stateDone && !w.bedtime) {
                        c.enter(CreatureState.PEEK, c.config.peekTime.sample(w.random))
                        c.catchable = true
                        val a = w.random.range(-3.14f, 3.14f)
                        c.heading = a
                        c.x = spot.x + cos(a) * spot.radius * 0.5f
                        c.y = spot.y + sin(a) * spot.radius * 0.5f
                    }
                }
            }
            CreatureState.PEEK -> {
                c.visibility += (0.7f - c.visibility) * (dt * 2f).coerceAtMost(1f)
                brake(c, dt, 3f)
                if (c.stateDone) {
                    c.spot = null
                    startWander(c, w)
                }
            }
            CreatureState.FLEE -> {
                val arrived = steer(c, w, dt, arrive = c.length * 0.4f, accel = 14f)
                if (isOutside(c, w)) c.active = false
                val spot = c.spot
                if (arrived && spot != null) {
                    c.enter(CreatureState.HIDE, 0f)
                    c.aux2 = 1f
                    c.catchable = false
                    c.stateDuration = c.config.hideTime.sample(w.random)
                } else if (arrived || c.stateDone) {
                    startFreeze(c, w)
                }
            }
            CreatureState.CAUGHT -> {
                brake(c, dt)
                updateCaught(c, w, dt)
            }
            CreatureState.EXIT -> updateExit(c, w, dt)
        }
        if (c.state != CreatureState.HIDE && c.state != CreatureState.PEEK && c.state != CreatureState.EXIT) {
            c.visibility += (1f - c.visibility) * (dt * 1.5f).coerceAtMost(1f)
        }
        keepInWater(c, w)
    }

    override fun startFlee(c: Creature, w: World, fx: Float, fy: Float) {
        // A sharp turn away from the paw first: fish flick around on the spot.
        c.heading = kotlin.math.atan2(c.y - fy, c.x - fx)
        super.startFlee(c, w, fx, fy)
        w.ripple(c.x, c.y, c.length * 0.8f)
        w.sound(SoundCue.WATER_BLOOP, c.x, 0.5f)
    }

    override fun ambientSound(c: Creature, w: World) {
        if (c.state == CreatureState.HIDE) return
        w.sound(if (w.random.chance(0.5f)) SoundCue.WATER_BLOOP else SoundCue.BUBBLES, c.x, if (c.species == Species.TADPOLE) 0.2f else 0.35f)
    }

    override fun caughtSound(c: Creature, w: World) {
        w.sound(SoundCue.WATER_SPLASH, c.x, 0.9f)
        w.ripple(c.x, c.y, c.length * 1.2f)
    }
}

/** Water striders skate in impulses (fast push, then friction), leaving rings on the water. */
object StriderBehavior : FlightBehavior() {
    override fun spawn(c: Creature, w: World) {
        w.layout.randomPoint(c.species.medium, w.random, w.scratch)
        c.x = w.scratch[0]
        c.y = w.scratch[1]
        c.heading = w.random.range(-3.14f, 3.14f)
        c.visibility = 0f
        startFreeze(c, w)
    }

    private fun skate(c: Creature, w: World, angle: Float, strength: Float) {
        c.heading = angle
        c.speed = c.config.dartSpeed * w.dp * strength * w.difficulty.speedMultiplier
        c.enter(CreatureState.DART, c.config.dartTime.sample(w.random) + 0.25f)
        // Rings spread from the pushing leg tips.
        val side = c.length * 0.6f
        w.ripple(c.x - c.dirY * side, c.y + c.dirX * side, c.length * 0.5f)
        w.ripple(c.x + c.dirY * side, c.y - c.dirX * side, c.length * 0.5f)
    }

    override fun startDart(c: Creature, w: World) {
        w.layout.randomPoint(c.species.medium, w.random, w.scratch)
        skate(c, w, c.angleTo(w.scratch[0], w.scratch[1]), w.random.range(0.7f, 1f))
    }

    override fun startWander(c: Creature, w: World) {
        skate(c, w, c.heading + w.random.range(-1f, 1f), w.random.range(0.25f, 0.45f))
        c.enter(CreatureState.WANDER, c.stateDuration)
    }

    override fun update(c: Creature, w: World, dt: Float) {
        tick(c, w, dt)
        when (c.state) {
            CreatureState.SPAWN -> startFreeze(c, w)
            CreatureState.DART, CreatureState.WANDER, CreatureState.FLEE -> {
                // Glide with friction.
                c.speed *= (1f - dt * 3.2f).coerceAtLeast(0f)
                val nx = c.x + c.dirX * c.speed * dt
                val ny = c.y + c.dirY * c.speed * dt
                if (w.layout.allows(c.species.medium, nx, ny)) {
                    c.x = nx
                    c.y = ny
                } else {
                    c.speed = 0f
                }
                if (c.stateDone) {
                    if (c.state == CreatureState.FLEE && c.aux2 > 0f) {
                        c.aux2 -= 1f
                        skate(c, w, c.heading + w.random.range(-0.5f, 0.5f), 1f)
                        c.enter(CreatureState.FLEE, c.stateDuration)
                    } else {
                        decide(c, w)
                    }
                }
            }
            CreatureState.IDLE, CreatureState.FREEZE -> {
                c.speed = 0f
                if (c.stateDone) decide(c, w)
            }
            CreatureState.CAUGHT -> {
                c.speed = 0f
                updateCaught(c, w, dt)
                if (c.stateTime < dt * 1.5f) w.ripple(c.x, c.y, c.length)
            }
            CreatureState.EXIT -> {
                c.speed = c.speed.coerceAtLeast(c.config.walkSpeed * 4f * w.dp)
                c.x += c.dirX * c.speed * dt
                c.y += c.dirY * c.speed * dt
                if (w.random.chance(dt * 4f)) w.ripple(c.x, c.y, c.length * 0.4f)
                if (isOutside(c, w) || c.stateDone || !w.layout.allows(c.species.medium, c.x, c.y)) {
                    c.visibility -= dt * 3f
                    if (c.visibility <= 0f || isOutside(c, w)) c.active = false
                }
            }
            else -> decide(c, w)
        }
        if (c.state != CreatureState.EXIT) c.visibility += (1f - c.visibility) * (dt * 2f).coerceAtMost(1f)
    }

    override fun startHide(c: Creature, w: World) = startFreeze(c, w)

    override fun startFlee(c: Creature, w: World, fx: Float, fy: Float) {
        c.aux2 = 1f + w.random.int(2)
        skate(c, w, kotlin.math.atan2(c.y - fy, c.x - fx) + w.random.range(-0.4f, 0.4f), 1f)
        c.enter(CreatureState.FLEE, c.stateDuration)
        w.sound(SoundCue.WATER_BLOOP, c.x, 0.3f)
    }

    override fun startExit(c: Creature, w: World, preferShelter: Boolean) {
        nearestEdge(c, w)
        c.heading = c.angleTo(c.targetX, c.targetY)
        c.enter(CreatureState.EXIT, EXIT_MAX_TIME)
    }

    override fun ambientSound(c: Creature, w: World) {
        if (c.state == CreatureState.DART) w.sound(SoundCue.WATER_BLOOP, c.x, 0.15f)
    }

    override fun caughtSound(c: Creature, w: World) = w.sound(SoundCue.WATER_SPLASH, c.x, 0.7f)
}
