package app.purrhunt.game.creatures

import app.purrhunt.domain.Species
import app.purrhunt.game.engine.Action
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.engine.CreatureState
import app.purrhunt.game.engine.SoundCue
import app.purrhunt.game.engine.World
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ladybirds and ground beetles crawl over grass and stones with a meandering path. The beetle sometimes
 * flips onto its back and waves its legs, or opens its wing cases and flies a short distance.
 */
object CrawlerBehavior : FlightBehavior() {
    override fun spawn(c: Creature, w: World) {
        val spots = w.layout.spotsFor(c.species.medium)
        if (spots.isNotEmpty() && w.random.chance(0.5f)) {
            val s = w.random.pick(spots)
            val a = s.exitAngle + w.random.range(-0.6f, 0.6f)
            c.x = s.x + cos(a) * s.radius * 0.6f
            c.y = s.y + sin(a) * s.radius * 0.6f
            c.heading = a
            c.visibility = 0f
        } else {
            placeAtEdge(c, w)
        }
        startWander(c, w)
    }

    override fun idleAction(c: Creature, w: World): Action = Action.SNIFF

    override fun update(c: Creature, w: World, dt: Float) {
        tick(c, w, dt)
        when (c.state) {
            CreatureState.SPAWN -> startWander(c, w)
            CreatureState.WANDER, CreatureState.DART -> {
                if (c.flying) {
                    if (fly(c, w, dt, landing = true, cruise = 0.7f)) land(c, w)
                } else {
                    c.heading = wrap(c.heading + sin(c.age * 3f + c.seed) * dt * 1.2f)
                    val arrived = steer(c, w, dt, accel = 6f)
                    if (arrived || c.stateDone) decideCrawler(c, w)
                }
            }
            CreatureState.IDLE -> {
                brake(c, dt, 12f)
                when (c.action) {
                    Action.OPEN_WINGS -> if (c.actionTime > 0.6f) takeOff(c, w)
                    else -> if (c.stateDone) decideCrawler(c, w)
                }
            }
            CreatureState.FREEZE -> {
                brake(c, dt, 14f)
                if (c.stateDone) decideCrawler(c, w)
            }
            CreatureState.HIDE -> {
                val spot = c.spot
                if (spot == null) {
                    decideCrawler(c, w)
                } else if (c.aux2 == 0f) {
                    if (steer(c, w, dt, arrive = spot.radius * 0.3f)) {
                        c.aux2 = 1f
                        c.catchable = false
                        c.stateDuration = c.stateTime + c.config.hideTime.sample(w.random)
                    }
                } else {
                    c.visibility += (0f - c.visibility) * (dt * 4f).coerceAtMost(1f)
                    if (w.bedtime && c.visibility < 0.02f) c.active = false
                    else if (c.stateDone && !w.bedtime) {
                        c.enter(CreatureState.PEEK, c.config.peekTime.sample(w.random))
                        c.heading = spot.exitAngle + w.random.range(-0.5f, 0.5f)
                        c.x = spot.x + cos(c.heading) * spot.radius * 0.8f
                        c.y = spot.y + sin(c.heading) * spot.radius * 0.8f
                        c.catchable = true
                    }
                }
            }
            CreatureState.PEEK -> {
                c.visibility += (0.8f - c.visibility) * (dt * 3f).coerceAtMost(1f)
                if (c.stateDone) {
                    c.spot = null
                    startWander(c, w)
                }
            }
            CreatureState.FLEE -> {
                val arrived = if (c.flying) fly(c, w, dt, landing = true) else steer(c, w, dt, accel = 10f)
                if (isOutside(c, w)) c.active = false
                else if (arrived || c.stateDone) {
                    if (c.flying) land(c, w) else startFreeze(c, w)
                }
            }
            CreatureState.CAUGHT -> {
                brake(c, dt)
                updateCaught(c, w, dt)
            }
            CreatureState.EXIT -> {
                if (c.flying) {
                    fly(c, w, dt, landing = false)
                    if (isOutside(c, w) || c.stateDone) c.active = false
                } else {
                    updateExit(c, w, dt)
                }
            }
        }
        if (c.state != CreatureState.HIDE && c.state != CreatureState.PEEK && c.state != CreatureState.EXIT) {
            c.visibility += (1f - c.visibility) * (dt * 3f).coerceAtMost(1f)
        }
    }

    private fun decideCrawler(c: Creature, w: World) {
        if (w.random.chance(c.config.specialChance)) {
            val flip = c.species == Species.BEETLE && w.random.chance(0.5f)
            c.enter(CreatureState.IDLE, if (flip) w.random.range(1.5f, 3f) else 5f)
            c.action = if (flip) Action.FLIPPED else Action.OPEN_WINGS
            c.actionTime = 0f
            c.targetSpeed = 0f
            return
        }
        decide(c, w)
    }

    private fun takeOff(c: Creature, w: World) {
        w.layout.randomPoint(c.species.medium, w.random, w.scratch)
        c.setTarget(w.scratch[0], w.scratch[1])
        c.flying = true
        c.action = Action.NONE
        c.enter(CreatureState.WANDER, 6f)
        c.targetSpeed = c.config.dartSpeed * 2.5f * w.dp
    }

    private fun land(c: Creature, w: World) {
        c.flying = false
        c.altitude = 0f
        startFreeze(c, w)
    }

    override fun startFlee(c: Creature, w: World, fx: Float, fy: Float) {
        if (c.action == Action.FLIPPED) return // a beetle on its back can't run
        if (w.random.chance(0.35f)) {
            c.flying = true
            pointAwayFrom(c, w, fx, fy, w.width * 0.4f)
            c.spot = null
            c.enter(CreatureState.FLEE, 3f)
            c.targetSpeed = c.config.dartSpeed * 2.5f * w.dp
        } else {
            super.startFlee(c, w, fx, fy)
        }
    }

    override fun startExit(c: Creature, w: World, preferShelter: Boolean) {
        if (c.flying || w.random.chance(0.4f)) {
            c.flying = true
            c.spot = null
            nearestEdge(c, w)
            c.enter(CreatureState.EXIT, EXIT_MAX_TIME)
            c.targetSpeed = c.config.dartSpeed * 2.5f * w.dp
        } else {
            super.startExit(c, w, preferShelter)
        }
    }

    override fun ambientSound(c: Creature, w: World) = w.sound(SoundCue.INSECT_RUSTLE, c.x, 0.2f)

    override fun caughtSound(c: Creature, w: World) = w.sound(SoundCue.INSECT_RUSTLE, c.x, 0.7f)
}

/**
 * Crickets sit and chirp, then jump in arcs. A jump stores its start in [Creature.aux0]/[Creature.aux1];
 * [Creature.aux2] counts the jumps left in a fleeing sequence.
 */
object CricketBehavior : FlightBehavior() {
    override fun spawn(c: Creature, w: World) {
        placeAtEdge(c, w)
        w.layout.randomPoint(c.species.medium, w.random, w.scratch)
        jump(c, w, w.scratch[0], w.scratch[1], 0.6f)
    }

    override fun idleAction(c: Creature, w: World): Action = Action.CHIRP

    override fun startDart(c: Creature, w: World) {
        val a = c.heading + w.random.range(-1.2f, 1.2f)
        val d = w.random.range(70f, 150f) * w.dp * w.difficulty.speedMultiplier
        var tx = c.x + cos(a) * d
        var ty = c.y + sin(a) * d
        if (!w.layout.allows(c.species.medium, tx, ty) || tx < 0 || ty < 0 || tx > w.width || ty > w.height) {
            w.layout.randomPoint(c.species.medium, w.random, w.scratch)
            tx = w.scratch[0]
            ty = w.scratch[1]
        }
        c.aux2 = 0f
        jump(c, w, tx, ty, c.config.dartTime.sample(w.random))
    }

    private fun jump(c: Creature, w: World, tx: Float, ty: Float, duration: Float) {
        c.aux0 = c.x
        c.aux1 = c.y
        c.setTarget(tx, ty)
        c.heading = c.angleTo(tx, ty)
        c.enter(CreatureState.DART, duration)
        c.speed = c.distanceToTarget() / duration
    }

    override fun update(c: Creature, w: World, dt: Float) {
        tick(c, w, dt)
        when (c.state) {
            CreatureState.SPAWN -> decide(c, w)
            CreatureState.DART, CreatureState.FLEE -> {
                val t = (c.stateTime / c.stateDuration).coerceIn(0f, 1f)
                c.x = c.aux0 + (c.targetX - c.aux0) * t
                c.y = c.aux1 + (c.targetY - c.aux1) * t
                c.altitude = sin(t * PI.toFloat()) * 0.8f
                if (t >= 1f) {
                    c.altitude = 0f
                    c.speed = 0f
                    if (isOutside(c, w)) {
                        c.active = false
                    } else if (c.aux2 > 0f) {
                        c.aux2 -= 1f
                        hopAway(c, w)
                    } else if (c.state == CreatureState.FLEE) {
                        startFreeze(c, w)
                    } else if (w.random.chance(0.5f)) {
                        startIdle(c, w)
                    } else {
                        decide(c, w)
                    }
                }
            }
            CreatureState.IDLE -> {
                c.speed = 0f
                if (c.action == Action.CHIRP && (c.actionTime % 1.6f) < 0.03f) w.sound(SoundCue.CRICKET_CHIRP, c.x, 0.45f)
                if (c.stateDone) decide(c, w)
            }
            CreatureState.WANDER -> {
                if (steer(c, w, dt) || c.stateDone) decide(c, w)
            }
            CreatureState.FREEZE -> {
                brake(c, dt, 20f)
                if (c.stateDone) decide(c, w)
            }
            CreatureState.HIDE -> {
                val spot = c.spot
                if (spot == null) decide(c, w) else if (c.aux2 == 0f && c.stateDuration == 0f) {
                    jump(c, w, spot.x, spot.y, 0.55f)
                    c.enter(CreatureState.HIDE, 0.55f)
                    c.aux2 = -1f
                } else if (c.aux2 == -1f) {
                    val t = (c.stateTime / 0.55f).coerceIn(0f, 1f)
                    c.x = c.aux0 + (c.targetX - c.aux0) * t
                    c.y = c.aux1 + (c.targetY - c.aux1) * t
                    c.altitude = sin(t * PI.toFloat()) * 0.8f
                    if (t >= 1f) {
                        c.aux2 = -2f
                        c.catchable = false
                        c.stateDuration = c.stateTime + c.config.hideTime.sample(w.random)
                    }
                } else {
                    c.visibility += (0f - c.visibility) * (dt * 5f).coerceAtMost(1f)
                    if (w.bedtime && c.visibility < 0.02f) c.active = false
                    else if (c.stateDone && !w.bedtime) {
                        c.visibility = 1f
                        c.catchable = true
                        c.aux2 = 0f
                        c.heading = spot.exitAngle
                        startDart(c, w)
                    }
                }
            }
            CreatureState.CAUGHT -> {
                c.speed = 0f
                updateCaught(c, w, dt)
            }
            CreatureState.EXIT -> {
                // Leaves in a chain of jumps towards the nearest edge; aux3 is the time left in the jump.
                if (c.aux3 <= 0f) {
                    c.aux0 = c.x
                    c.aux1 = c.y
                    c.setTarget(c.x + cos(c.heading) * EXIT_JUMP_DP * w.dp, c.y + sin(c.heading) * EXIT_JUMP_DP * w.dp)
                    c.aux3 = EXIT_JUMP_TIME
                }
                c.aux3 -= dt
                val t = (1f - c.aux3 / EXIT_JUMP_TIME).coerceIn(0f, 1f)
                c.x = c.aux0 + (c.targetX - c.aux0) * t
                c.y = c.aux1 + (c.targetY - c.aux1) * t
                c.altitude = sin(t * PI.toFloat()) * 0.8f
                if (isOutside(c, w) || c.stateDone) c.active = false
            }
            CreatureState.PEEK -> decide(c, w)
        }
        if (c.state != CreatureState.HIDE) c.visibility += (1f - c.visibility) * (dt * 3f).coerceAtMost(1f)
    }

    private fun hopAway(c: Creature, w: World) {
        val a = c.heading + w.random.range(-0.5f, 0.5f)
        val d = 130f * w.dp
        jump(c, w, (c.x + cos(a) * d).coerceIn(-c.length * 2, w.width + c.length * 2), (c.y + sin(a) * d).coerceIn(-c.length * 2, w.height + c.length * 2), 0.5f)
        c.enter(CreatureState.FLEE, 0.5f)
    }

    override fun startHide(c: Creature, w: World) {
        val spot = w.layout.nearestShelter(c.species.medium, c.x, c.y) ?: return startDart(c, w)
        c.spot = spot
        c.aux2 = 0f
        c.enter(CreatureState.HIDE, 0f)
    }

    override fun startFlee(c: Creature, w: World, fx: Float, fy: Float) {
        c.heading = kotlin.math.atan2(c.y - fy, c.x - fx)
        c.aux2 = 1f + w.random.int(2)
        c.spot = null
        hopAway(c, w)
    }

    override fun startExit(c: Creature, w: World, preferShelter: Boolean) {
        nearestEdge(c, w)
        c.heading = c.angleTo(c.targetX, c.targetY)
        c.aux3 = 0f
        c.spot = null
        c.enter(CreatureState.EXIT, EXIT_MAX_TIME)
    }

    private const val EXIT_JUMP_DP = 140f
    private const val EXIT_JUMP_TIME = 0.5f

    override fun ambientSound(c: Creature, w: World) {
        if (c.state == CreatureState.IDLE || c.state == CreatureState.FREEZE) w.sound(SoundCue.CRICKET_CHIRP, c.x, 0.35f)
    }

    override fun caughtSound(c: Creature, w: World) = w.sound(SoundCue.INSECT_RUSTLE, c.x, 0.7f)
}
