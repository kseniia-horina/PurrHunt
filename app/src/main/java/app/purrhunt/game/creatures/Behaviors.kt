package app.purrhunt.game.creatures

import app.purrhunt.domain.Family
import app.purrhunt.game.engine.Behavior

/** Behaviour per body plan. Pure Kotlin: no Android types, covered by unit tests. */
object Behaviors {
    fun of(family: Family): Behavior = when (family) {
        Family.RODENT -> RodentBehavior
        else -> RodentBehavior
    }
}
