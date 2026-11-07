package app.purrhunt.game.creatures

import app.purrhunt.domain.Species
import app.purrhunt.game.engine.Action
import app.purrhunt.game.engine.Behavior
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.engine.CreatureState
import app.purrhunt.game.engine.SoundCue
import app.purrhunt.game.engine.World
import app.purrhunt.game.scenes.HidingSpot
import app.purrhunt.game.scenes.SpotKind
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mice and rats: stick to walls and objects, run in short darts, freeze for 0.5–2 s, groom,
 * slip into burrows or behind crates and peek out again.
 */
object RodentBehavior : Behavior() {
    private fun isRat(c: Creature) = c.species == Species.RAT

    override fun spawn(c: Creature, w: World) {
        val spots = w.layout.spotsFor(c.species.medium)
        if (spots.isNotEmpty() && w.random.chance(0.7f)) {
            val spot = w.random.pick(spots)
            enterShelter(c, w, spot, peekSoon = true)
            c.visibility = 0f
        } else {
            enterFromEdge(c, w)
        }
        c.tail.snap(c.x - c.dirX * c.length * 0.45f, c.y - c.dirY * c.length * 0.45f, c.dirX, c.dirY, c.length * 0.1f)
    }

    private fun enterFromEdge(c: Creature, w: World) {
        val fromLeft = w.random.chance(0.5f)
        c.x = if (fromLeft) -c.length else w.width + c.length
        c.y = w.random.range(w.layout.floorTop + c.length, w.height - c.length)
        c.heading = if (fromLeft) 0f else Math.PI.toFloat()
        startWander(c, w)
        c.targetSpeed = c.config.dartSpeed * 0.6f * w.dp
    }

    /** Puts the creature inside a shelter (hidden) and starts the hide timer. */
    private fun enterShelter(c: Creature, w: World, spot: HidingSpot, peekSoon: Boolean) {
        c.spot = spot
        inside(c, spot)
        c.heading = spot.exitAngle
        c.enter(CreatureState.HIDE, if (peekSoon) w.random.range(0.3f, 1.2f) else c.config.hideTime.sample(w.random))
        c.aux2 = 1f
        c.speed = 0f
        c.catchable = false
    }

    private fun inside(c: Creature, spot: HidingSpot) {
        if (spot.isWallBurrow) {
            c.x = spot.x
            c.y = spot.clipLine - c.length * 0.6f
            c.clipAboveY = spot.clipLine
        } else {
            c.x = spot.x
            c.y = spot.y
        }
    }

    override fun idleAction(c: Creature, w: World): Action = if (w.random.chance(0.55f)) Action.GROOM else Action.SNIFF

    override fun update(c: Creature, w: World, dt: Float) {
        tick(c, w, dt)
        when (c.state) {
            CreatureState.SPAWN -> decide(c, w)
            CreatureState.IDLE -> {
                brake(c, dt)
                if (c.stateDone) decide(c, w)
            }
            CreatureState.WANDER -> {
                val arrived = steer(c, w, dt)
                if (arrived || c.stateDone) {
                    if (w.random.chance(0.4f)) startFreeze(c, w) else decide(c, w)
                }
            }
            CreatureState.DART -> {
                val arrived = steer(c, w, dt, accel = 16f)
                if (arrived || c.stateDone) {
                    // Real mice dart, then freeze to listen.
                    if (w.random.chance(0.6f)) startFreeze(c, w) else decide(c, w)
                }
            }
            CreatureState.FREEZE -> {
                brake(c, dt, rate = 20f)
                if (c.stateDone) decide(c, w)
            }
            CreatureState.HIDE -> updateHide(c, w, dt)
            CreatureState.PEEK -> updatePeek(c, w, dt)
            CreatureState.FLEE -> {
                val arrived = steer(c, w, dt, accel = 18f)
                val spot = c.spot
                if (spot != null && spot.isWallBurrow && c.distanceTo(spot.x, spot.y) < c.length * 1.5f) {
                    c.clipAboveY = spot.clipLine
                }
                when {
                    arrived && spot != null -> enterShelter(c, w, spot, peekSoon = false)
                    arrived || c.stateDone -> startFreeze(c, w)
                    isOutside(c, w) -> c.active = false
                }
            }
            CreatureState.CAUGHT -> {
                brake(c, dt)
                updateCaught(c, w, dt)
            }
            CreatureState.EXIT -> updateExit(c, w, dt)
        }
        updateVisibility(c, dt)
        releaseClip(c)
        c.tail.update(
            c.x - c.dirX * c.length * 0.45f, c.y - c.dirY * c.length * 0.45f, c.dirX, c.dirY, c.age,
            wiggle = c.length * if (c.speed < 5f) 0.02f else 0.06f,
        )
    }

    private fun updateHide(c: Creature, w: World, dt: Float) {
        val spot = c.spot
        if (spot == null) {
            decide(c, w)
            return
        }
        if (c.aux2 == 0f) {
            // On the way to the shelter.
            if (spot.isWallBurrow) {
                c.setTarget(spot.x, spot.clipLine - c.length * 0.6f)
                if (c.distanceTo(spot.x, spot.y) < c.length * 1.5f) c.clipAboveY = spot.clipLine
            }
            if (steer(c, w, dt, arrive = c.length * 0.2f)) {
                enterShelter(c, w, spot, peekSoon = false)
            }
            return
        }
        c.speed = 0f
        if (c.stateDone && !w.bedtime) startPeek(c, w, spot)
        if (w.bedtime && c.visibility < 0.02f) c.active = false
    }

    private fun startPeek(c: Creature, w: World, spot: HidingSpot) {
        c.enter(CreatureState.PEEK, c.config.peekTime.sample(w.random))
        c.heading = spot.exitAngle + w.random.range(-0.4f, 0.4f)
        if (spot.isWallBurrow) {
            c.x = spot.x
            c.y = spot.clipLine - c.length * 0.15f
            c.clipAboveY = spot.clipLine
        } else {
            val r = spot.radius * 0.85f
            c.x = spot.x + cos(c.heading) * r
            c.y = spot.y + sin(c.heading) * r
        }
        c.catchable = true
        c.aux0 = c.heading
        if (w.random.chance(0.5f)) w.sound(if (isRat(c)) SoundCue.RAT_SQUEAK else SoundCue.MOUSE_SQUEAK, c.x, 0.35f)
    }

    private fun updatePeek(c: Creature, w: World, dt: Float) {
        // Look left and right from the entrance.
        c.heading = c.aux0 + sin(c.stateTime * 3.2f) * 0.5f
        c.speed = 0f
        if (!c.stateDone) return
        val spot = c.spot
        if (spot != null && w.random.chance(0.3f)) {
            enterShelter(c, w, spot, peekSoon = false)
        } else {
            c.heading = c.aux0
            c.spot = null
            if (w.random.chance(0.5f)) startDart(c, w) else startWander(c, w)
        }
    }

    private fun updateVisibility(c: Creature, dt: Float) {
        val target = when {
            c.state == CreatureState.HIDE && c.aux2 == 1f -> 0f
            c.state == CreatureState.PEEK -> 0.75f
            c.state == CreatureState.EXIT -> c.visibility
            else -> 1f
        }
        val rate = if (target < c.visibility) 5f else 3f
        c.visibility += (target - c.visibility) * (rate * dt).coerceAtMost(1f)
    }

    private fun releaseClip(c: Creature) {
        if (!c.clipAboveY.isNaN() && c.state != CreatureState.HIDE && c.state != CreatureState.PEEK &&
            c.state != CreatureState.EXIT && c.y - c.length * 0.7f > c.clipAboveY
        ) {
            c.clipAboveY = Float.NaN
        }
    }

    override fun startDart(c: Creature, w: World) {
        super.startDart(c, w)
        w.sound(SoundCue.MOUSE_RUSTLE, c.x, movementVolume(c, w, 0.5f))
    }

    override fun startFlee(c: Creature, w: World, fx: Float, fy: Float) {
        super.startFlee(c, w, fx, fy)
        w.sound(if (isRat(c)) SoundCue.RAT_SQUEAK else SoundCue.MOUSE_SQUEAK, c.x, 0.6f)
        w.sound(SoundCue.MOUSE_RUSTLE, c.x, movementVolume(c, w, 0.8f))
    }

    override fun ambientSound(c: Creature, w: World) {
        when {
            c.state == CreatureState.HIDE -> Unit
            c.speed > c.config.walkSpeed * w.dp * 1.5f -> w.sound(SoundCue.MOUSE_RUSTLE, c.x, movementVolume(c, w, 0.6f))
            else -> w.sound(if (isRat(c)) SoundCue.RAT_SQUEAK else SoundCue.MOUSE_SQUEAK, c.x, 0.4f)
        }
    }

    override fun caughtSound(c: Creature, w: World) {
        w.sound(if (isRat(c)) SoundCue.RAT_CAUGHT else SoundCue.MOUSE_CAUGHT, c.x, 0.9f)
    }

    override fun bedtime(c: Creature, w: World) {
        if (c.isLeaving) return
        val spot = w.layout.nearestShelter(c.species.medium, c.x, c.y)
        if (spot != null) {
            c.spot = spot
            c.setTarget(spot.x, spot.y)
            c.enter(CreatureState.HIDE, 0f)
            c.aux2 = 0f
            c.targetSpeed = c.config.walkSpeed * 2.5f * w.dp
        } else {
            startExit(c, w, preferShelter = false)
        }
    }

    /** Louder when faster and closer to the screen centre. */
    fun movementVolume(c: Creature, w: World, base: Float): Float {
        val speedFactor = (c.speed / (c.config.dartSpeed * w.dp)).coerceIn(0.3f, 1f)
        val dx = (c.x - w.width / 2) / (w.width / 2)
        val dy = (c.y - w.height / 2) / (w.height / 2)
        val centre = 1f - 0.5f * kotlin.math.sqrt(dx * dx + dy * dy).coerceAtMost(1f)
        return base * speedFactor * centre
    }
}
