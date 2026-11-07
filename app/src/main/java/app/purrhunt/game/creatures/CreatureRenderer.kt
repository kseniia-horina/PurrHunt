package app.purrhunt.game.creatures

import androidx.compose.ui.graphics.drawscope.DrawScope
import app.purrhunt.domain.Family
import app.purrhunt.game.engine.Creature

/** Draws one family. Caches its static paths and brushes; the per-frame draw allocates nothing. */
interface CreatureRenderer {
    /** Draws in world space; implementations set up their own unit space. */
    fun DrawScope.draw(c: Creature, time: Float)

    /** Shadow footprint relative to the body length (half-axes). */
    val shadowRx: Float get() = 0.42f
    val shadowRy: Float get() = 0.22f

    /** Whether the renderer draws a part outside the unit body in world space (tails). */
    fun DrawScope.drawWorldParts(c: Creature) = Unit
}

object CreatureRenderers {
    fun of(family: Family): CreatureRenderer = when (family) {
        Family.RODENT -> RodentRenderer
        Family.BIRD -> BirdRenderer
        Family.BUTTERFLY -> ButterflyRenderer
        Family.FLY -> FlyRenderer
        Family.FIREFLY -> FireflyRenderer
        Family.CRAWLER -> CrawlerRenderer
        Family.CRICKET -> CricketRenderer
        Family.FISH, Family.TADPOLE -> FishRenderer
        Family.STRIDER -> StriderRenderer
    }
}
