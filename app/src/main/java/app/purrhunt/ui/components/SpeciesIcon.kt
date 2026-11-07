package app.purrhunt.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.purrhunt.domain.Species
import app.purrhunt.game.creatures.CreatureRenderers
import app.purrhunt.game.engine.Creature

/** A still portrait of a species drawn with the game renderer; [silhouette] shows an uncaught species. */
@Composable
fun SpeciesIcon(species: Species, modifier: Modifier = Modifier, silhouette: Boolean = false, description: String? = null) {
    val creature = remember(species) { Creature(0) }
    val paint = remember { Paint().apply { colorFilter = ColorFilter.tint(Color(0xFF3A4658), BlendMode.SrcIn) } }
    Canvas(modifier.semantics { if (description != null) contentDescription = description }) {
        val length = size.minDimension * if (species.medium == app.purrhunt.domain.Medium.AIR) 0.7f else 0.62f
        creature.reset(species, length, species.ordinal)
        creature.x = size.width / 2 + length * 0.08f
        creature.y = size.height / 2
        creature.heading = -0.5f
        creature.aux0 = 0.7f
        creature.tail.snap(
            creature.x - creature.dirX * length * 0.45f, creature.y - creature.dirY * length * 0.45f,
            creature.dirX, creature.dirY, length * 0.09f,
        )
        val r = CreatureRenderers.of(species.family)
        if (silhouette) drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
        with(r) {
            drawWorldParts(creature)
            draw(creature, 0f)
        }
        if (silhouette) drawContext.canvas.restore()
    }
}
