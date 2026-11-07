package app.purrhunt.game.scenes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.purrhunt.domain.Location
import app.purrhunt.domain.Season
import app.purrhunt.game.engine.World

data class SceneLook(val night: Boolean, val season: Season)

/**
 * Art of one location. Static layers are rendered once into bitmaps ([background] under the creatures,
 * [foreground] with occluders over them); the animated hooks run every frame and must not allocate.
 */
interface SceneArt {
    fun DrawScope.background(layout: SceneLayout, look: SceneLook)
    fun DrawScope.foreground(layout: SceneLayout, look: SceneLook) = Unit
    /** Per-frame layer between background and creatures (water caustics, dust). */
    fun DrawScope.animatedUnder(layout: SceneLayout, look: SceneLook, time: Float, world: World?) = Unit
    /** Per-frame layer above everything (swaying grass tips). */
    fun DrawScope.animatedOver(layout: SceneLayout, look: SceneLook, time: Float) = Unit

    companion object {
        fun of(location: Location): SceneArt = when (location) {
            Location.BASEMENT -> BasementArt
            Location.GARDEN -> GardenArt
            Location.MEADOW -> MeadowArt
            Location.POND -> PondArt
            Location.MIX -> MixArt
        }
    }
}

object Bitmaps {
    /** Renders [block] once into a bitmap of the given pixel size. */
    fun render(width: Int, height: Int, density: Float, block: DrawScope.() -> Unit): ImageBitmap {
        val bitmap = ImageBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
        CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat()), block)
        return bitmap
    }
}
