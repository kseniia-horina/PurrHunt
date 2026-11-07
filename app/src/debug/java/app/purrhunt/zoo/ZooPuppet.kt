package app.purrhunt.zoo

import app.purrhunt.domain.Family
import app.purrhunt.domain.Species
import app.purrhunt.game.engine.Action
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.engine.CreatureState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Drives one creature through every pose of its family without the behaviour state machine,
 * so each animation can be inspected in isolation.
 */
class ZooPuppet(private val zoom: Float = 2.6f) {
    private data class Pose(
        val state: CreatureState,
        val action: Action = Action.NONE,
        val speed: Float = 0f,
        val flying: Boolean = false,
        val altitude: Float = 0f,
        val visibility: Float = 1f,
        val seconds: Float = 2.5f,
    )

    val creature = Creature(0)
    var time = 0f
        private set
    var label = ""
        private set

    private var poses: List<Pose> = emptyList()
    private var index = 0
    private var poseTime = 0f
    private var angle = 0f
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f
    private var dp = 1f

    fun select(species: Species, density: Float) {
        dp = density
        creature.reset(species, species.sizeDp * dp * zoom, species.ordinal)
        poses = posesFor(species.family)
        index = 0
        poseTime = 0f
        apply(poses[0])
        creature.tail.snap(creature.x, creature.y, 1f, 0f, creature.length * 0.1f)
    }

    fun layout(width: Float, height: Float) = place(width / 2, height / 2, minOf(width, height) * 0.22f)

    fun place(centerX: Float, centerY: Float, circle: Float) {
        cx = centerX
        cy = centerY
        radius = circle
    }

    fun update(dt: Float) {
        time += dt
        poseTime += dt
        val pose = poses.getOrNull(index) ?: return
        if (poseTime >= pose.seconds) {
            index = (index + 1) % poses.size
            poseTime = 0f
            apply(poses[index])
        }
        val p = poses[index]
        val c = creature
        c.stateTime = poseTime
        c.actionTime = poseTime
        c.age += dt
        c.blink = if ((c.age % 3.1f) < 0.15f) 0.1f else 0f
        val speedPx = p.speed * dp * zoom
        c.speed = speedPx
        c.gait += dt * c.config.gaitRate * (0.25f + p.speed / c.config.walkSpeed.coerceAtLeast(1f)).coerceAtMost(4f)
        if (p.speed > 0f) {
            angle += speedPx / radius.coerceAtLeast(1f) * dt
            c.x = cx + cos(angle) * radius
            c.y = cy + sin(angle) * radius
            c.heading = angle + (PI / 2).toFloat()
        } else {
            c.x += (cx - c.x) * (dt * 3f).coerceAtMost(1f)
            c.y += (cy - c.y) * (dt * 3f).coerceAtMost(1f)
        }
        c.altitude = when (p.state) {
            CreatureState.CAUGHT -> sin((poseTime / 0.8f).coerceIn(0f, 1f) * PI.toFloat()) * 0.6f
            else -> p.altitude
        }
        if (p.state == CreatureState.CAUGHT && poseTime < 0.8f) c.heading += dt * 9f * (1f - poseTime / 0.8f)
        if (p.state == CreatureState.DART && c.species.family == Family.CRICKET) {
            c.altitude = sin(((poseTime % 0.5f) / 0.5f) * PI.toFloat()) * 0.7f
        }
        c.tail.update(c.x - c.dirX * c.length * 0.45f, c.y - c.dirY * c.length * 0.45f, c.dirX, c.dirY, c.age, c.length * 0.05f)
    }

    private fun apply(p: Pose) {
        creature.enter(p.state, p.seconds)
        creature.action = p.action
        creature.flying = p.flying
        creature.visibility = p.visibility
        label = if (p.action == Action.NONE) p.state.name else "${p.state.name} · ${p.action.name}"
    }

    private fun posesFor(family: Family): List<Pose> {
        val c = creature.config
        val base = mutableListOf(
            Pose(CreatureState.IDLE),
            Pose(CreatureState.WANDER, speed = c.walkSpeed),
            Pose(CreatureState.DART, speed = c.dartSpeed * 0.5f, seconds = 1.5f),
            Pose(CreatureState.FREEZE),
        )
        when (family) {
            Family.RODENT -> {
                base += Pose(CreatureState.IDLE, Action.GROOM, seconds = 3f)
                base += Pose(CreatureState.IDLE, Action.SNIFF)
                base += Pose(CreatureState.PEEK, visibility = 0.75f)
            }
            Family.BIRD -> {
                base += Pose(CreatureState.IDLE, Action.PECK, seconds = 3f)
                base += Pose(CreatureState.IDLE, Action.LOOK)
                base += Pose(CreatureState.WANDER, speed = c.fleeSpeed * 0.4f, flying = true, altitude = 0.8f, seconds = 3f)
            }
            Family.BUTTERFLY -> {
                base.clear()
                base += Pose(CreatureState.WANDER, speed = c.walkSpeed, flying = true, altitude = 0.7f, seconds = 3f)
                base += Pose(CreatureState.IDLE, Action.FOLD_WINGS, seconds = 3f)
                base += Pose(CreatureState.IDLE, Action.PERCHED, seconds = 3f)
            }
            Family.FLY -> {
                base.clear()
                base += Pose(CreatureState.WANDER, speed = c.dartSpeed * 0.4f, flying = true, altitude = 0.6f, seconds = 3f)
                base += Pose(CreatureState.IDLE, Action.RUB_LEGS, seconds = 3f)
                base += Pose(CreatureState.FREEZE)
            }
            Family.FIREFLY -> {
                base.clear()
                base += Pose(CreatureState.WANDER, speed = c.walkSpeed, flying = true, altitude = 0.6f, seconds = 4f)
                base += Pose(CreatureState.IDLE, flying = true, altitude = 0.6f, seconds = 3f)
            }
            Family.CRAWLER -> {
                base += Pose(CreatureState.IDLE, Action.FLIPPED, seconds = 3f)
                base += Pose(CreatureState.IDLE, Action.OPEN_WINGS, seconds = 2f)
                base += Pose(CreatureState.WANDER, speed = c.dartSpeed, flying = true, altitude = 0.7f, seconds = 2.5f)
            }
            Family.CRICKET -> {
                base += Pose(CreatureState.IDLE, Action.CHIRP, seconds = 3f)
            }
            Family.FISH, Family.TADPOLE, Family.STRIDER -> {
                base += Pose(CreatureState.FLEE, speed = c.fleeSpeed * 0.5f, seconds = 1.5f)
            }
        }
        base += Pose(CreatureState.CAUGHT, seconds = 1.6f)
        return base
    }
}
