package app.purrhunt.game.creatures

import app.purrhunt.game.engine.Behavior
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.engine.World
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Helpers shared by flying and swimming families. */
abstract class FlightBehavior : Behavior() {
    /** Enters from a random screen edge, heading inwards. */
    protected fun placeAtEdge(c: Creature, w: World) {
        val pad = c.length
        when (w.random.int(4)) {
            0 -> { c.x = -pad; c.y = w.random.range(pad, w.height - pad); c.heading = 0f }
            1 -> { c.x = w.width + pad; c.y = w.random.range(pad, w.height - pad); c.heading = PI.toFloat() }
            2 -> { c.x = w.random.range(pad, w.width - pad); c.y = -pad; c.heading = (PI / 2).toFloat() }
            else -> { c.x = w.random.range(pad, w.width - pad); c.y = w.height + pad; c.heading = (-PI / 2).toFloat() }
        }
    }

    /** A point far from (fx, fy) for [c]'s medium. */
    protected fun pointAwayFrom(c: Creature, w: World, fx: Float, fy: Float, distance: Float) {
        val a = kotlin.math.atan2(c.y - fy, c.x - fx) + w.random.range(-0.6f, 0.6f)
        var tx = c.x + cos(a) * distance
        var ty = c.y + sin(a) * distance
        val m = w.layout.margin
        tx = tx.coerceIn(m, w.width - m)
        ty = ty.coerceIn(m, w.height - m)
        if (!w.layout.allows(c.species.medium, tx, ty)) {
            w.layout.randomPoint(c.species.medium, w.random, w.scratch)
            tx = w.scratch[0]
            ty = w.scratch[1]
        }
        c.setTarget(tx, ty)
    }

    /** Flight towards the target; altitude rises after take-off and drops when [landing] near the end. */
    protected fun fly(c: Creature, w: World, dt: Float, landing: Boolean, cruise: Float = 1f): Boolean {
        val arrived = steer(c, w, dt, arrive = c.length * 0.4f, accel = 5f)
        val d = c.distanceToTarget()
        val wanted = if (landing && d < c.length * 3f) (d / (c.length * 3f)) * cruise else cruise
        c.altitude += (wanted - c.altitude) * (dt * 4f).coerceAtMost(1f)
        return arrived
    }

    /** Water creatures turn back when they reach the shore. */
    protected fun keepInWater(c: Creature, w: World) {
        val water = w.layout.water ?: return
        if (c.isLeaving) return
        if (!w.layout.allows(c.species.medium, c.x, c.y)) {
            c.setTarget(water.cx, water.cy)
            c.heading = wrap(c.heading + angleDiff(c.angleTo(water.cx, water.cy), c.heading) * 0.2f)
        }
    }
}
