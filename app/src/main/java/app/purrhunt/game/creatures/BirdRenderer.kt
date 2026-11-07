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
import app.purrhunt.game.engine.Action
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.render.RenderKit
import app.purrhunt.game.render.RenderKit.creatureSpace
import app.purrhunt.game.render.RenderKit.eye
import app.purrhunt.game.render.SpriteCache
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Top-down songbirds: layered feathers on the back and wings, a fanned tail, turning head and pecking,
 * wings that spread and beat with a bend in flight.
 */
object BirdRenderer : CreatureRenderer {
    private class Look(
        val back: Brush,
        val streak: Color,
        val wing: Brush,
        val wingBar: Color,
        val wingEdge: Color,
        val tail: Color,
        val crown: Color,
        val cheek: Color,
        val side: Color,
        val beak: Color,
        val featherPath: Path,
    )

    private val looks = HashMap<Species, Look>()
    private val sprites = SpriteCache(Species.entries.size, 0.45f, 0.3f)
    private val featherStroke = Stroke(0.008f, cap = StrokeCap.Round)
    private val bodyPath = Path().apply { addOval(androidx.compose.ui.geometry.Rect(-0.38f, -0.23f, 0.26f, 0.23f)) }
    private val tailPath = Path().apply {
        moveTo(-0.3f, -0.06f)
        lineTo(-0.6f, -0.11f)
        quadraticTo(-0.64f, 0f, -0.6f, 0.11f)
        lineTo(-0.3f, 0.06f)
        close()
    }
    /** Folded right wing lying along the body (the left one is mirrored). */
    private val foldedWing = Path().apply {
        moveTo(0.14f, -0.13f)
        cubicTo(0.05f, -0.24f, -0.3f, -0.2f, -0.46f, -0.06f)
        cubicTo(-0.3f, -0.1f, -0.05f, -0.08f, 0.14f, -0.13f)
        close()
    }
    /** Spread right wing seen from above, root at the shoulder. */
    private val spreadWing = Path().apply {
        moveTo(0.08f, 0f)
        cubicTo(0.16f, -0.25f, 0.1f, -0.5f, -0.05f, -0.62f)
        lineTo(-0.12f, -0.55f)
        lineTo(-0.15f, -0.48f)
        lineTo(-0.18f, -0.4f)
        cubicTo(-0.2f, -0.25f, -0.2f, -0.1f, -0.12f, 0f)
        close()
    }
    private val primaries = Path().apply {
        for (k in 0 until 6) {
            val t = k / 5f
            moveTo(-0.02f - t * 0.1f, -0.2f - t * 0.05f)
            lineTo(-0.06f - t * 0.12f, -0.4f - t * 0.18f)
        }
    }
    private val beakPath = Path().apply {
        moveTo(0.34f, -0.035f)
        lineTo(0.44f, 0f)
        lineTo(0.34f, 0.035f)
        close()
    }

    private fun look(s: Species): Look = looks.getOrPut(s) {
        when (s) {
            Species.TIT -> Look(
                back = RenderKit.bodyBrush(Color(0xFF9DA868), Color(0xFF7C8A4A), Color(0xFF4F5A2C)),
                streak = Color(0x00000000),
                wing = Brush.linearGradient(listOf(Color(0xFF8395B2), Color(0xFF52627E)), Offset(0f, -0.1f), Offset(-0.4f, -0.2f)),
                wingBar = Color(0xFFEDEFF2), wingEdge = Color(0xFF313A4C), tail = Color(0xFF4A5670),
                crown = Color(0xFF111215), cheek = Color(0xFFF4F4F0), side = Color(0xFFE8C83A), beak = Color(0xFF26272A),
                featherPath = RenderKit.featherPath(21, 0.3f, 0.18f, 5, 6, cx = -0.05f),
            )
            Species.BULLFINCH -> Look(
                back = RenderKit.bodyBrush(Color(0xFFA9A9AE), Color(0xFF85868C), Color(0xFF55565C)),
                streak = Color(0x00000000),
                wing = Brush.linearGradient(listOf(Color(0xFF2A2A2E), Color(0xFF111114)), Offset(0f, -0.1f), Offset(-0.4f, -0.2f)),
                wingBar = Color(0xFFD9D9DC), wingEdge = Color(0xFF050506), tail = Color(0xFF141416),
                crown = Color(0xFF0E0E10), cheek = Color(0xFFD65A4C), side = Color(0xFFD9584A), beak = Color(0xFF1E1E21),
                featherPath = RenderKit.featherPath(22, 0.3f, 0.19f, 5, 6, cx = -0.05f),
            )
            else -> Look(
                back = RenderKit.bodyBrush(Color(0xFFB79670), Color(0xFF8C6B48), Color(0xFF5A432C)),
                streak = Color(0xFF3D2A1A),
                wing = Brush.linearGradient(listOf(Color(0xFF9A7650), Color(0xFF5E4128)), Offset(0f, -0.1f), Offset(-0.4f, -0.2f)),
                wingBar = Color(0xFFE6DCC8), wingEdge = Color(0xFF2E2014), tail = Color(0xFF4E3925),
                crown = Color(0xFF7A7672), cheek = Color(0xFFD9D0C2), side = Color(0xFFBFB4A4), beak = Color(0xFF3A3632),
                featherPath = RenderKit.featherPath(23, 0.3f, 0.18f, 5, 6, cx = -0.05f),
            )
        }
    }

    override val shadowRx: Float get() = 0.36f
    override val shadowRy: Float get() = 0.2f

    override fun DrawScope.draw(c: Creature, time: Float) {
        val l = look(c.species)
        val lift = 1f + c.altitude * 0.35f
        creatureSpace(c.x, c.y, c.heading, c.length * lift) {
            drawPath(tailPath, l.tail)
            drawLine(l.wingEdge, Offset(-0.32f, 0f), Offset(-0.6f, 0f), strokeWidth = 0.012f)

            val flying = c.flying || c.altitude > 0.3f
            if (flying) {
                val beat = c.gait * 2f * PI.toFloat()
                val open = 0.35f + 0.65f * abs(cos(beat))
                val bend = sin(beat) * 12f
                wing(l, open, bend, 1f)
                wing(l, open, -bend, -1f)
            }

            with(sprites) {
                drawSprite(c.species.ordinal, c.length) {
                    drawPath(bodyPath, l.side)
                    withTransform({ scale(0.94f, 0.86f, Offset(-0.05f, 0f)) }) { drawPath(bodyPath, l.back) }
                    drawPath(l.featherPath, Color.Black.copy(alpha = 0.18f), style = featherStroke)
                    if (l.streak.alpha > 0f) {
                        for (k in 0 until 7) {
                            val x = -0.25f + k * 0.05f
                            drawLine(l.streak, Offset(x, -0.06f + (k % 2) * 0.02f), Offset(x - 0.06f, -0.03f + (k % 2) * 0.02f), strokeWidth = 0.018f, cap = StrokeCap.Round)
                            drawLine(l.streak, Offset(x, 0.06f - (k % 2) * 0.02f), Offset(x - 0.06f, 0.03f - (k % 2) * 0.02f), strokeWidth = 0.018f, cap = StrokeCap.Round)
                        }
                    }
                }
            }

            if (!flying) {
                foldedWingPair(l)
            }

            // Head: turns while looking around, dips forward while pecking.
            val look = if (c.action == Action.LOOK) sin(c.actionTime * 3.4f) * 0.55f else sin(c.age * 0.9f + c.seed) * 0.12f
            val peck = if (c.action == Action.PECK && c.aux1 == 1f) 0.05f else 0f
            withTransform({
                translate(0.2f + peck, 0f)
                rotate(look * 57.3f, Offset.Zero)
                scale(1f - peck, 1f - peck, Offset.Zero)
            }) {
                drawOval(l.cheek, Offset(-0.07f, -0.115f), Size(0.21f, 0.23f))
                drawOval(l.crown, Offset(-0.06f, -0.075f), Size(0.17f, 0.15f))
                if (c.species == Species.TIT) drawLine(l.crown, Offset(-0.06f, 0f), Offset(-0.18f, 0f), strokeWidth = 0.035f, cap = StrokeCap.Round)
                withTransform({ translate(-0.2f, 0f) }) { drawPath(beakPath, l.beak) }
                val blink = if (c.blink > 0f) 1f else 0f
                eye(0.08f, -0.085f, 0.022f, blink, lid = l.crown)
                eye(0.08f, 0.085f, 0.022f, blink, lid = l.crown)
            }
        }
    }

    private fun DrawScope.foldedWingPair(l: Look) {
        drawPath(foldedWing, l.wing)
        drawLine(l.wingBar, Offset(0.02f, -0.17f), Offset(-0.12f, -0.17f), strokeWidth = 0.015f, cap = StrokeCap.Round)
        withTransform({ scale(1f, -1f, Offset.Zero) }) {
            drawPath(foldedWing, l.wing)
            drawLine(l.wingBar, Offset(0.02f, -0.17f), Offset(-0.12f, -0.17f), strokeWidth = 0.015f, cap = StrokeCap.Round)
        }
    }

    private fun DrawScope.wing(l: Look, open: Float, bendDeg: Float, side: Float) {
        withTransform({
            translate(0.05f, 0f)
            scale(1f, open * side, Offset.Zero)
            rotate(bendDeg, Offset.Zero)
        }) {
            drawPath(spreadWing, l.wing)
            drawPath(primaries, l.wingEdge, style = featherStroke)
            drawLine(l.wingBar, Offset(0.04f, -0.2f), Offset(-0.12f, -0.22f), strokeWidth = 0.02f)
        }
    }
}
