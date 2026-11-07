package app.purrhunt.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import app.purrhunt.game.scenes.Bitmaps
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Pre-rendered static body layers (gradient volume + fur) per slot, at the creature's on-screen size.
 * Drawing a bitmap each frame is far cheaper than hundreds of fur strokes.
 */
class SpriteCache(slots: Int, @PublishedApi internal val halfWidth: Float, @PublishedApi internal val halfHeight: Float) {
    @PublishedApi internal val bitmaps = arrayOfNulls<ImageBitmap>(slots)
    @PublishedApi internal val scales = FloatArray(slots)

    /**
     * Draws slot [slot] in unit space; [ppu] is pixels per unit (the body length in pixels).
     * [paint] runs only when the sprite has to be (re)built; inlining keeps the frame allocation free.
     */
    inline fun DrawScope.drawSprite(slot: Int, ppu: Float, crossinline paint: DrawScope.() -> Unit) {
        var bmp = bitmaps[slot]
        if (bmp == null || abs(scales[slot] - ppu) > ppu * 0.1f) {
            bmp = build(ppu) { paint() }
            bitmaps[slot] = bmp
            scales[slot] = ppu
        }
        val s = scales[slot]
        withTransform({ scale(1f / s, 1f / s, Offset.Zero) }) {
            drawImage(bmp, Offset(-halfWidth * s, -halfHeight * s))
        }
    }

    @PublishedApi
    internal fun build(ppu: Float, paint: DrawScope.() -> Unit): ImageBitmap {
        val w = ceil(halfWidth * 2 * ppu).toInt()
        val h = ceil(halfHeight * 2 * ppu).toInt()
        return Bitmaps.render(w, h, 1f) {
            withTransform({
                translate(halfWidth * ppu, halfHeight * ppu)
                scale(ppu, ppu, Offset.Zero)
            }) { paint() }
        }
    }
}
