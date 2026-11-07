package app.purrhunt.game.scenes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import app.purrhunt.domain.Season
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Procedural nature pieces shared by the outdoor scenes. */
object NatureKit {
    /** Dense static lawn: hundreds of short curved blades over a mottled base. */
    fun DrawScope.lawn(rnd: Random, u: Float, base: Color, dark: Color, light: Color, blades: Int, area: (Float, Float) -> Boolean = { _, _ -> true }) {
        val w = size.width
        val h = size.height
        // Mottled patches.
        repeat(60) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            if (!area(x, y)) return@repeat
            val r = (40f + rnd.nextFloat() * 120f) * u
            drawCircle(Brush.radialGradient(listOf(lerp(dark, light, rnd.nextFloat()).copy(alpha = 0.35f), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        }
        val paths = Array(3) { Path() }
        repeat(blades) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            if (!area(x, y)) return@repeat
            val len = (6f + rnd.nextFloat() * 12f) * u
            val a = -PI.toFloat() / 2 + (rnd.nextFloat() - 0.5f) * 1.6f
            val p = paths[rnd.nextInt(3)]
            p.moveTo(x, y)
            p.quadraticTo(x + cos(a) * len * 0.5f + (rnd.nextFloat() - 0.5f) * 4f * u, y + sin(a) * len * 0.5f, x + cos(a) * len, y + sin(a) * len)
        }
        drawPath(paths[0], dark.copy(alpha = 0.7f), style = Stroke(1.6f * u, cap = StrokeCap.Round))
        drawPath(paths[1], base.copy(alpha = 0.8f), style = Stroke(1.4f * u, cap = StrokeCap.Round))
        drawPath(paths[2], light.copy(alpha = 0.6f), style = Stroke(1.2f * u, cap = StrokeCap.Round))
    }

    /** A rounded leafy bush seen from above, made of overlapping leaf clusters. */
    fun DrawScope.bush(cx: Float, cy: Float, r: Float, rnd: Random, leaf: Color, dark: Color, snow: Boolean) {
        drawCircle(Color.Black.copy(alpha = 0.35f), r * 1.02f, Offset(cx + r * 0.12f, cy + r * 0.16f))
        repeat(26) {
            val a = rnd.nextFloat() * 2f * PI.toFloat()
            val d = r * 0.65f * kotlin.math.sqrt(rnd.nextFloat())
            val px = cx + cos(a) * d
            val py = cy + sin(a) * d
            val lr = r * (0.28f + rnd.nextFloat() * 0.18f)
            val shade = 1f - (py - cy + r) / (2 * r) * 0.5f
            drawCircle(
                Brush.radialGradient(listOf(lerp(leaf, Color.White, 0.12f * shade), leaf, dark), Offset(px - lr * 0.3f, py - lr * 0.3f), lr * 1.2f),
                lr, Offset(px, py),
            )
            // Leaf veins hint.
            drawLine(dark.copy(alpha = 0.35f), Offset(px - lr * 0.4f, py), Offset(px + lr * 0.3f, py - lr * 0.2f), strokeWidth = lr * 0.06f)
        }
        if (snow) {
            repeat(9) {
                val a = rnd.nextFloat() * 2f * PI.toFloat()
                val d = r * 0.5f * rnd.nextFloat()
                drawCircle(Color(0xFFE6ECF2), r * (0.15f + rnd.nextFloat() * 0.15f), Offset(cx + cos(a) * d - r * 0.1f, cy + sin(a) * d - r * 0.1f), alpha = 0.9f)
            }
        }
    }

    /** A rounded stone with a little moss. */
    fun DrawScope.stone(cx: Float, cy: Float, r: Float, rnd: Random, moss: Boolean) {
        val p = Path()
        val n = 9
        for (i in 0..n) {
            val a = i * 2f * PI.toFloat() / n
            val rr = r * (0.85f + rnd.nextFloat() * 0.2f)
            val x = cx + cos(a) * rr * 1.1f
            val y = cy + sin(a) * rr * 0.85f
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        drawOval(Color.Black.copy(alpha = 0.35f), Offset(cx - r * 1.05f + r * 0.15f, cy - r * 0.8f + r * 0.2f), Size(r * 2.2f, r * 1.75f))
        drawPath(p, Brush.radialGradient(listOf(Color(0xFFA8A49A), Color(0xFF77736A), Color(0xFF45423D)), Offset(cx - r * 0.35f, cy - r * 0.35f), r * 1.4f))
        repeat(14) {
            drawCircle(Color.Black.copy(alpha = 0.12f), r * 0.04f, Offset(cx + (rnd.nextFloat() - 0.5f) * r * 1.4f, cy + (rnd.nextFloat() - 0.5f) * r * 1.1f))
        }
        if (moss) drawOval(Color(0x885E7A3A), Offset(cx - r * 0.2f, cy + r * 0.1f), Size(r * 0.9f, r * 0.5f))
    }

    /** A clump of tall grass, top-down: blades radiating from a root. */
    fun DrawScope.grassTuft(cx: Float, cy: Float, r: Float, rnd: Random, dark: Color, light: Color) {
        drawCircle(Color.Black.copy(alpha = 0.3f), r * 0.8f, Offset(cx + r * 0.1f, cy + r * 0.15f))
        repeat(60) {
            val a = rnd.nextFloat() * 2f * PI.toFloat()
            val len = r * (0.5f + rnd.nextFloat() * 0.6f)
            val bend = (rnd.nextFloat() - 0.5f) * 0.6f
            val c = lerp(dark, light, rnd.nextFloat())
            val ex = cx + cos(a) * len
            val ey = cy + sin(a) * len
            val mx = cx + cos(a + bend) * len * 0.5f
            val my = cy + sin(a + bend) * len * 0.5f
            val p = Path().apply { moveTo(cx, cy); quadraticTo(mx, my, ex, ey) }
            drawPath(p, c, style = Stroke(r * 0.045f, cap = StrokeCap.Round))
        }
    }

    /** A flower: white daisy or blue cornflower (cat eyes see blue well). */
    fun DrawScope.flower(cx: Float, cy: Float, r: Float, rnd: Random, blue: Boolean) {
        drawCircle(Color.Black.copy(alpha = 0.25f), r * 1.05f, Offset(cx + r * 0.2f, cy + r * 0.25f))
        val petals = if (blue) 9 else 14
        val start = rnd.nextFloat() * 360f
        for (i in 0 until petals) {
            rotate(start + i * 360f / petals, Offset(cx, cy)) {
                if (blue) {
                    drawOval(Color(0xFF4F6FC4), Offset(cx + r * 0.15f, cy - r * 0.18f), Size(r * 0.85f, r * 0.36f))
                    drawLine(Color(0xFF314C9A), Offset(cx + r * 0.25f, cy), Offset(cx + r * 0.9f, cy), strokeWidth = r * 0.05f)
                } else {
                    drawOval(Color(0xFFF2EEE4), Offset(cx + r * 0.15f, cy - r * 0.12f), Size(r * 0.85f, r * 0.24f))
                }
            }
        }
        drawCircle(if (blue) Color(0xFF243A78) else Color(0xFFE8B930), r * 0.28f, Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.3f), r * 0.1f, Offset(cx - r * 0.08f, cy - r * 0.08f))
    }

    /** Fallen leaves (autumn): small rotated leaf shapes in warm colours. */
    fun DrawScope.fallenLeaves(rnd: Random, u: Float, count: Int, area: (Float, Float) -> Boolean) {
        val colors = listOf(Color(0xFFB8642A), Color(0xFFC9932F), Color(0xFF8E4A22), Color(0xFFA67B2E))
        repeat(count) {
            val x = rnd.nextFloat() * size.width
            val y = rnd.nextFloat() * size.height
            if (!area(x, y)) return@repeat
            leaf(x, y, (7f + rnd.nextFloat() * 6f) * u, rnd.nextFloat() * 360f, colors[rnd.nextInt(colors.size)])
        }
    }

    fun DrawScope.leaf(x: Float, y: Float, len: Float, angle: Float, color: Color) {
        rotate(angle, Offset(x, y)) {
            val p = Path().apply {
                moveTo(x - len, y)
                quadraticTo(x, y - len * 0.6f, x + len, y)
                quadraticTo(x, y + len * 0.6f, x - len, y)
                close()
            }
            drawPath(p, Color.Black.copy(alpha = 0.25f))
            drawPath(p, color)
            drawLine(lerp(color, Color.Black, 0.35f), Offset(x - len, y), Offset(x + len, y), strokeWidth = len * 0.08f)
        }
    }

    /** Snow patches on the ground (winter). */
    fun DrawScope.snowPatches(rnd: Random, u: Float, count: Int, area: (Float, Float) -> Boolean) {
        repeat(count) {
            val x = rnd.nextFloat() * size.width
            val y = rnd.nextFloat() * size.height
            if (!area(x, y)) return@repeat
            val r = (20f + rnd.nextFloat() * 70f) * u
            drawCircle(Brush.radialGradient(listOf(Color(0xE6EEF2F6), Color(0x99DCE4EC), Color.Transparent), Offset(x, y), r), r, Offset(x, y))
        }
    }

    fun groundTint(season: Season, base: Color): Color = when (season) {
        Season.WINTER -> lerp(base, Color(0xFF8A9488), 0.45f)
        Season.AUTUMN -> lerp(base, Color(0xFF7A6A3A), 0.35f)
        Season.SPRING -> lerp(base, Color(0xFF5E8A48), 0.15f)
        Season.SUMMER -> base
    }
}

/**
 * Tall grass tips drawn every frame with a gentle wind sway. Blades are generated once per layout;
 * per frame only the shared paths are rebuilt (no allocation).
 */
class SwayingGrass(private val count: Int, private val seed: Int) {
    private var forLayout: SceneLayout? = null
    private val xs = FloatArray(count)
    private val ys = FloatArray(count)
    private val lens = FloatArray(count)
    private val angles = FloatArray(count)
    private val shades = IntArray(count)
    private val paths = Array(3) { Path() }
    private var strokes = emptyArray<Stroke>()

    /** [area] must not capture anything: it is passed every frame. */
    fun DrawScope.draw(layout: SceneLayout, time: Float, colors: Array<Color>, area: (SceneLayout, Float, Float) -> Boolean) {
        if (forLayout !== layout) generate(layout, area)
        for (p in paths) p.reset()
        for (i in 0 until count) {
            val sway = sin(time * 1.3f + xs[i] * 0.004f + ys[i] * 0.002f) * 0.25f + sin(time * 3.1f + i) * 0.05f
            val a = angles[i] + sway
            val ex = xs[i] + cos(a) * lens[i]
            val ey = ys[i] + sin(a) * lens[i]
            val p = paths[shades[i]]
            p.moveTo(xs[i], ys[i])
            p.quadraticTo(xs[i] + cos(angles[i]) * lens[i] * 0.5f, ys[i] + sin(angles[i]) * lens[i] * 0.5f, ex, ey)
        }
        for (k in 0 until 3) drawPath(paths[k], colors[k], style = strokes[k])
    }

    private fun generate(layout: SceneLayout, area: (SceneLayout, Float, Float) -> Boolean) {
        val rnd = Random(seed)
        val u = layout.dp
        strokes = arrayOf(Stroke(2.2f * u, cap = StrokeCap.Round), Stroke(1.8f * u, cap = StrokeCap.Round), Stroke(1.4f * u, cap = StrokeCap.Round))
        var i = 0
        var guard = 0
        while (i < count && guard++ < count * 20) {
            val x = rnd.nextFloat() * layout.width
            val y = rnd.nextFloat() * layout.height
            if (!area(layout, x, y)) continue
            // Tall grass grows mostly near the edges so the middle stays clear for prey.
            val edge = minOf(minOf(x, layout.width - x), minOf(y, layout.height - y)) / (minOf(layout.width, layout.height) * 0.5f)
            if (rnd.nextFloat() < edge * 1.4f) continue
            xs[i] = x
            ys[i] = y
            lens[i] = (14f + rnd.nextFloat() * 22f) * layout.dp
            angles[i] = -PI.toFloat() / 2 + (rnd.nextFloat() - 0.5f) * 1.4f
            shades[i] = rnd.nextInt(3)
            i++
        }
        forLayout = layout
    }
}
