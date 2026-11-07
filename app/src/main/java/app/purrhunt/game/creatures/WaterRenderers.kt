package app.purrhunt.game.creatures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import app.purrhunt.domain.Species
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.render.RenderKit.creatureSpace
import app.purrhunt.game.render.RenderKit.eye
import kotlin.math.PI
import kotlin.math.sin

/**
 * Fish bodies bend with a travelling wave: the outline is rebuilt each frame from a spine of points
 * (reusing the same Path and arrays), the tail fin follows the last segment.
 */
object FishRenderer : CreatureRenderer {
    private const val N = 12
    private val spineX = FloatArray(N)
    private val spineY = FloatArray(N)
    private val outline = Path()
    private val fin = Path()
    private val dorsal = Path()

    private class Look(val body: Brush, val back: Color, val fin: Color, val belly: Color, val width: Float, val tailSize: Float)

    private val crucian = Look(
        Brush.linearGradient(listOf(Color(0xFFD4B565), Color(0xFFA58436), Color(0xFF6E5520)), Offset(0f, -0.12f), Offset(0f, 0.12f)),
        Color(0xFF4F3E18), Color(0xAA8E6E2C), Color(0xFFE4CC86), 0.2f, 0.22f,
    )
    private val goldfish = Look(
        Brush.linearGradient(listOf(Color(0xFFFFC46A), Color(0xFFF08A24), Color(0xFFB4520E)), Offset(0f, -0.12f), Offset(0f, 0.12f)),
        Color(0xFFB34A0C), Color(0x99FF9A3C), Color(0xFFFFD79A), 0.2f, 0.34f,
    )
    private val tadpole = Look(
        Brush.radialGradient(listOf(Color(0xFF5C5544), Color(0xFF2E2A20)), Offset(0.15f, -0.05f), 0.3f),
        Color(0xFF1E1B14), Color(0x995A5242), Color(0xFF6C6450), 0.3f, 0.0f,
    )

    override val shadowRx: Float get() = 0.42f
    override val shadowRy: Float get() = 0.16f

    /** Half width of the body at spine parameter t (0 = snout, 1 = tail root). */
    private fun halfWidth(t: Float, w: Float, isTadpole: Boolean): Float =
        if (isTadpole) {
            if (t < 0.35f) w * sin((t / 0.35f) * PI.toFloat() * 0.5f + 0.4f).coerceAtLeast(0.2f) else w * 0.18f * (1f - (t - 0.35f) / 0.65f) + 0.004f
        } else {
            w * sin((t * 0.95f + 0.05f) * PI.toFloat()).coerceAtLeast(0f) * (1f - t * 0.55f) + 0.01f
        }

    override fun DrawScope.draw(c: Creature, time: Float) {
        val isTadpole = c.species == Species.TADPOLE
        val l = when (c.species) {
            Species.GOLDFISH -> goldfish
            Species.TADPOLE -> tadpole
            else -> crucian
        }
        val swim = (c.speed / (c.length * 1.5f)).coerceIn(0.15f, 1.2f)
        val amp = if (isTadpole) 0.09f else 0.05f
        val phase = c.gait * 2f * PI.toFloat()
        for (i in 0 until N) {
            val t = i / (N - 1f)
            spineX[i] = 0.5f - t
            // The wave grows towards the tail and travels backwards.
            spineY[i] = sin(phase - t * 5f) * amp * swim * t * t * 2f
        }
        creatureSpace(c.x, c.y, c.heading, c.length) {
            // Tail fin following the last segment.
            val tx = spineX[N - 1]
            val ty = spineY[N - 1]
            val dy = ty - spineY[N - 2]
            if (!isTadpole) {
                fin.reset()
                val s = l.tailSize
                fin.moveTo(tx + 0.02f, ty)
                fin.quadraticTo(tx - s * 0.6f, ty + dy * 4f - s * 0.7f, tx - s * 1.1f, ty + dy * 6f - s * 0.75f)
                fin.quadraticTo(tx - s * 0.6f, ty + dy * 5f, tx - s * 1.1f, ty + dy * 6f + s * 0.75f)
                fin.quadraticTo(tx - s * 0.6f, ty + dy * 4f + s * 0.7f, tx + 0.02f, ty)
                fin.close()
                drawPath(fin, l.fin)
                // Pectoral fins.
                val f = sin(c.age * 6f) * 0.03f
                drawOval(l.fin, Offset(0.16f, -0.2f - f), Size(0.12f, 0.1f))
                drawOval(l.fin, Offset(0.16f, 0.1f + f), Size(0.12f, 0.1f))
            }
            outline.reset()
            for (i in 0 until N) {
                val t = i / (N - 1f)
                val hw = halfWidth(t, l.width, isTadpole)
                if (i == 0) outline.moveTo(spineX[i] + 0.02f, spineY[i]) else outline.lineTo(spineX[i], spineY[i] - hw)
            }
            for (i in N - 1 downTo 1) {
                val t = i / (N - 1f)
                outline.lineTo(spineX[i], spineY[i] + halfWidth(t, l.width, isTadpole))
            }
            outline.close()
            drawPath(outline, l.body)
            if (!isTadpole) {
                drawPath(outline, l.belly.copy(alpha = 0.25f), style = Stroke(0.02f))
                dorsal.reset()
                dorsal.moveTo(spineX[2], spineY[2])
                for (i in 3 until N - 2) dorsal.lineTo(spineX[i], spineY[i])
                drawPath(dorsal, l.back.copy(alpha = 0.45f), style = Stroke(0.03f, cap = StrokeCap.Round))
                drawPath(dorsal, l.fin.copy(alpha = 0.5f), style = Stroke(0.01f, cap = StrokeCap.Round))
                // Scale shimmer.
                for (k in 0 until 4) {
                    val i = 3 + k * 2
                    drawCircle(Color.White.copy(alpha = 0.12f), 0.03f, Offset(spineX[i], spineY[i] - 0.06f))
                }
            }
            val blink = if (c.blink > 0f) 1f else 0f
            val eyeX = if (isTadpole) 0.38f else 0.38f
            eye(eyeX, spineY[1] - l.width * 0.55f, 0.03f, blink, if (c.species == Species.GOLDFISH) Color(0xFF231206) else Color(0xFF0C0B08), l.back)
            eye(eyeX, spineY[1] + l.width * 0.55f, 0.03f, blink, if (c.species == Species.GOLDFISH) Color(0xFF231206) else Color(0xFF0C0B08), l.back)
        }
    }
}

/** Water strider: slender body on three pairs of legs, the long middle pair sweeping back on each push. */
object StriderRenderer : CreatureRenderer {
    private val body = Color(0xFF3B2C20)
    private val legColor = Color(0xFF2A1F17)
    private val dimple = Color(0x55FFFFFF)
    private val dimpleStroke = Stroke(0.012f)

    override val shadowRx: Float get() = 0.3f
    override val shadowRy: Float get() = 0.08f

    override fun DrawScope.draw(c: Creature, time: Float) {
        creatureSpace(c.x, c.y, c.heading, c.length) {
            val push = (c.speed / (c.config.dartSpeed * 0.3f + 1f)).coerceIn(0f, 1f)
            val sweep = push * 0.2f
            leg(Offset(0.12f, 0f), Offset(0.28f, -0.14f), Offset(0.36f, -0.1f), 1f)
            leg(Offset(0.12f, 0f), Offset(0.28f, 0.14f), Offset(0.36f, 0.1f), 1f)
            leg(Offset(0.02f, 0f), Offset(0.1f - sweep, -0.4f), Offset(-0.3f - sweep, -0.75f), 1.2f)
            leg(Offset(0.02f, 0f), Offset(0.1f - sweep, 0.4f), Offset(-0.3f - sweep, 0.75f), 1.2f)
            leg(Offset(-0.06f, 0f), Offset(-0.25f, -0.25f), Offset(-0.6f, -0.35f), 1f)
            leg(Offset(-0.06f, 0f), Offset(-0.25f, 0.25f), Offset(-0.6f, 0.35f), 1f)
            drawOval(body, Offset(-0.32f, -0.045f), Size(0.6f, 0.09f))
            drawOval(Color(0xFF5A4636), Offset(0.12f, -0.05f), Size(0.16f, 0.1f))
            drawCircle(Color(0xFF14100C), 0.022f, Offset(0.26f, -0.035f))
            drawCircle(Color(0xFF14100C), 0.022f, Offset(0.26f, 0.035f))
            drawLine(legColor, Offset(0.28f, -0.01f), Offset(0.42f, -0.06f), strokeWidth = 0.01f)
            drawLine(legColor, Offset(0.28f, 0.01f), Offset(0.42f, 0.06f), strokeWidth = 0.01f)
        }
    }

    private fun DrawScope.leg(root: Offset, knee: Offset, foot: Offset, dimpleScale: Float) {
        drawLine(legColor, root, knee, strokeWidth = 0.018f, cap = StrokeCap.Round)
        drawLine(legColor, knee, foot, strokeWidth = 0.012f, cap = StrokeCap.Round)
        // Surface tension dimple under each foot.
        withTransform({ translate(foot.x, foot.y) }) {
            drawCircle(dimple, 0.03f * dimpleScale, Offset.Zero, style = dimpleStroke)
        }
    }
}
