package app.purrhunt.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Shared drawing helpers. Creatures are drawn in a local unit space: the body is about 1 unit long,
 * centred on the origin and facing +x. [creatureSpace] sets up that transform.
 */
object RenderKit {
    private val shadowBrush = Brush.radialGradient(
        0f to Color.Black.copy(alpha = 0.55f),
        0.6f to Color.Black.copy(alpha = 0.25f),
        1f to Color.Transparent,
        center = Offset.Zero,
        radius = 1f,
    )

    private val glowBrush = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.55f),
        0.55f to Color.White.copy(alpha = 0.18f),
        1f to Color.Transparent,
        center = Offset.Zero,
        radius = 1f,
    )

    /** Soft blurred ellipse shadow. Higher creatures cast larger, fainter shadows further away. */
    fun DrawScope.softShadow(x: Float, y: Float, rx: Float, ry: Float, headingRad: Float, altitude: Float, alpha: Float = 1f) {
        val spread = 1f + altitude * 0.8f
        val offset = altitude * rx * 0.9f
        val a = alpha * (1f - altitude * 0.65f)
        withTransform({
            translate(x + offset * 0.4f + rx * 0.08f, y + offset + ry * 0.25f)
            rotate((headingRad * 180f / PI).toFloat(), Offset.Zero)
            scale(rx * spread, ry * spread, Offset.Zero)
        }) {
            drawCircle(shadowBrush, radius = 1f, center = Offset.Zero, alpha = a)
        }
    }

    /** "Cat contrast" halo: a light glow that separates prey from the background. */
    fun DrawScope.contrastHalo(x: Float, y: Float, radius: Float, alpha: Float) {
        withTransform({
            translate(x, y)
            scale(radius, radius, Offset.Zero)
        }) {
            drawCircle(glowBrush, radius = 1f, center = Offset.Zero, alpha = alpha)
        }
    }

    /** Applies translate → rotate → scale so [block] draws in unit creature space. */
    inline fun DrawScope.creatureSpace(x: Float, y: Float, headingRad: Float, scale: Float, block: DrawScope.() -> Unit) {
        withTransform({
            translate(x, y)
            rotate((headingRad * 180f / PI).toFloat(), Offset.Zero)
            scale(scale, scale, Offset.Zero)
        }, block)
    }

    /**
     * Glossy eye: dark iris, wet rim, specular highlight; [blink] 0..1 closes it with a lid.
     */
    fun DrawScope.eye(cx: Float, cy: Float, r: Float, blink: Float, iris: Color = Color(0xFF0B0907), lid: Color) {
        val open = 1f - blink
        if (open < 0.1f) {
            drawLine(lid, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = r * 0.5f)
            return
        }
        drawOval(Color.Black.copy(alpha = 0.35f), Offset(cx - r * 1.2f, cy - r * 1.2f * open), Size(r * 2.4f, r * 2.4f * open))
        drawOval(iris, Offset(cx - r, cy - r * open), Size(r * 2f, r * 2f * open))
        drawOval(Color.White.copy(alpha = 0.18f), Offset(cx - r * 0.9f, cy - r * 0.9f * open), Size(r * 1.8f, r * 1.8f * open), style = Stroke(r * 0.18f))
        drawCircle(Color.White.copy(alpha = 0.9f), radius = r * 0.32f, center = Offset(cx - r * 0.3f, cy - r * 0.35f * open))
        drawCircle(Color.White.copy(alpha = 0.4f), radius = r * 0.15f, center = Offset(cx + r * 0.35f, cy + r * 0.3f * open))
    }

    /**
     * Volume gradient for a body: light on the back, darker on the flanks, with the light source top-left.
     * Built once per palette and reused; coordinates are in unit space.
     */
    fun bodyBrush(light: Color, base: Color, dark: Color, centerX: Float = -0.08f, centerY: Float = -0.1f, radius: Float = 0.6f): Brush =
        Brush.radialGradient(
            0f to light,
            0.45f to base,
            1f to dark,
            center = Offset(centerX, centerY),
            radius = radius,
        )

    fun highlightBrush(radius: Float, alpha: Float = 0.35f, center: Offset = Offset.Zero): Brush = Brush.radialGradient(
        0f to Color.White.copy(alpha = alpha),
        1f to Color.Transparent,
        center = center,
        radius = radius,
    )

    /**
     * Fur: many short strokes radiating along the outline of an ellipse (rx, ry) and over the back.
     * The seeded random keeps the pattern identical every frame. Returns one path for a single draw call.
     */
    fun furPath(seed: Int, rx: Float, ry: Float, strokes: Int, length: Float, cx: Float = 0f): Path {
        val rnd = Random(seed)
        val p = Path()
        for (i in 0 until strokes) {
            val onEdge = i % 3 != 0
            val a = rnd.nextFloat() * 2f * PI.toFloat()
            val k = if (onEdge) 0.92f + rnd.nextFloat() * 0.12f else rnd.nextFloat() * 0.8f
            val px = cx + cos(a) * rx * k
            val py = sin(a) * ry * k
            // Hair lies backwards (towards -x) and slightly outward.
            val out = if (onEdge) 0.6f else 0.2f
            val dx = -1f + cos(a) * out + (rnd.nextFloat() - 0.5f) * 0.4f
            val dy = sin(a) * out + (rnd.nextFloat() - 0.5f) * 0.4f
            val len = length * (0.6f + rnd.nextFloat() * 0.8f)
            val norm = kotlin.math.sqrt(dx * dx + dy * dy)
            p.moveTo(px, py)
            p.lineTo(px + dx / norm * len, py + dy / norm * len)
        }
        return p
    }

    /** Scale-like feather layers: rows of overlapping arcs. */
    fun featherPath(seed: Int, rx: Float, ry: Float, rows: Int, perRow: Int, cx: Float = 0f): Path {
        val rnd = Random(seed)
        val p = Path()
        for (r in 0 until rows) {
            val fx = cx + rx * (0.7f - r * 1.4f / rows)
            val half = ry * kotlin.math.sqrt((1f - ((fx - cx) / rx) * ((fx - cx) / rx)).coerceAtLeast(0.05f)) * 0.9f
            for (k in 0 until perRow) {
                val fy = -half + (k + 0.5f) * 2f * half / perRow + (rnd.nextFloat() - 0.5f) * 0.02f
                val w = rx * 1.4f / rows
                p.moveTo(fx, fy - half / perRow)
                p.quadraticTo(fx - w, fy, fx, fy + half / perRow)
            }
        }
        return p
    }

    fun shade(c: Color, k: Float): Color = if (k >= 0f) lerp(c, Color.White, k) else lerp(c, Color.Black, -k)
}
