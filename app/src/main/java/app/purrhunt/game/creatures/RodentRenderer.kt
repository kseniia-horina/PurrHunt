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
import kotlin.math.sin

/** Top-down mice and rats with gradient volume, fur strokes, twitching whiskers and a springy tail. */
object RodentRenderer : CreatureRenderer {
    private class Look(
        val body: Brush,
        val furDark: Color,
        val furLight: Color,
        val earOuter: Color,
        val earInner: Color,
        val skin: Color,
        val iris: Color,
        val tail: Color,
        val lid: Color,
        val bodyPath: Path,
        val furDarkPath: Path,
        val furLightPath: Path,
        val earSize: Float,
        val headWidth: Float,
        val tailWidth: Float,
    )

    private val looks = HashMap<Species, Look>()
    private val furStroke = Stroke(width = 0.007f, cap = StrokeCap.Round)
    private val whiskerStroke = Stroke(width = 0.005f, cap = StrokeCap.Round)
    private val whiskerColor = Color(0xCCEFE8E0)
    private val nose = Color(0xFFD98C8C)
    private val headLight = RenderKit.highlightBrush(0.12f, 0.28f, Offset(0.3f, -0.03f))
    private val sprites = SpriteCache(Species.entries.size, 0.6f, 0.32f)

    private fun look(s: Species): Look = looks.getOrPut(s) {
        when (s) {
            Species.RAT -> build(
                s, base = Color(0xFF6B5E50), light = Color(0xFF8E8070), dark = Color(0xFF3B332B),
                ear = Color(0xFF8A6E66), earIn = Color(0xFFC79A93), skin = Color(0xFFC49E95), iris = Color(0xFF080605),
                tail = Color(0xFFB2938A), earSize = 0.085f, width = 0.22f, rear = 0.5f, tailWidth = 0.065f,
            )
            Species.WHITE_MOUSE -> build(
                s, base = Color(0xFFEDE8E1), light = Color(0xFFFFFFFF), dark = Color(0xFFBDB5AA),
                ear = Color(0xFFF2C9C8), earIn = Color(0xFFF4A9B0), skin = Color(0xFFF2B9BA), iris = Color(0xFFB02A3A),
                tail = Color(0xFFF0C4C4), earSize = 0.14f, width = 0.2f, rear = 0.46f, tailWidth = 0.045f,
            )
            Species.MOUSELET -> build(
                s, base = Color(0xFF9B9189), light = Color(0xFFC4BAB0), dark = Color(0xFF615851),
                ear = Color(0xFFB09088), earIn = Color(0xFFE2AAA6), skin = Color(0xFFDDB2AA), iris = Color(0xFF080605),
                tail = Color(0xFFD1ADA5), earSize = 0.16f, width = 0.22f, rear = 0.42f, tailWidth = 0.05f,
            )
            else -> build(
                s, base = Color(0xFF887E75), light = Color(0xFFB2A79C), dark = Color(0xFF574F49),
                ear = Color(0xFFA3857D), earIn = Color(0xFFDBA29E), skin = Color(0xFFD4ADA5), iris = Color(0xFF080605),
                tail = Color(0xFFC9A69D), earSize = 0.14f, width = 0.2f, rear = 0.46f, tailWidth = 0.045f,
            )
        }
    }

    private fun build(
        s: Species, base: Color, light: Color, dark: Color, ear: Color, earIn: Color, skin: Color, iris: Color,
        tail: Color, earSize: Float, width: Float, rear: Float, tailWidth: Float,
    ): Look {
        val body = Path().apply {
            moveTo(-rear, 0f)
            cubicTo(-rear, -width, -0.2f, -width * 1.15f, 0.06f, -width * 0.95f)
            cubicTo(0.2f, -width * 0.82f, 0.3f, -width * 0.62f, 0.42f, -width * 0.3f)
            quadraticTo(0.52f, -0.04f, 0.54f, 0f)
            quadraticTo(0.52f, 0.04f, 0.42f, width * 0.3f)
            cubicTo(0.3f, width * 0.62f, 0.2f, width * 0.82f, 0.06f, width * 0.95f)
            cubicTo(-0.2f, width * 1.15f, -rear, width, -rear, 0f)
            close()
        }
        return Look(
            body = RenderKit.bodyBrush(light, base, dark, centerX = -0.05f, centerY = -0.06f, radius = 0.55f),
            furDark = dark.copy(alpha = 0.35f),
            furLight = light.copy(alpha = 0.3f),
            earOuter = ear,
            earInner = earIn,
            skin = skin,
            iris = iris,
            tail = tail,
            lid = dark,
            bodyPath = body,
            furDarkPath = RenderKit.furPath(s.ordinal * 31 + 1, rear * 0.9f, width * 0.9f, 170, 0.03f, cx = -0.02f),
            furLightPath = RenderKit.furPath(s.ordinal * 31 + 2, rear * 0.8f, width * 0.75f, 110, 0.025f, cx = -0.05f),
            earSize = earSize,
            headWidth = width,
            tailWidth = tailWidth,
        )
    }

    override fun DrawScope.drawWorldParts(c: Creature) {
        val l = look(c.species)
        val t = c.tail
        val base = c.length * l.tailWidth
        for (i in 1 until t.count) {
            val k = 1f - i.toFloat() / t.count
            drawLine(
                l.tail,
                Offset(t.xs[i - 1], t.ys[i - 1]),
                Offset(t.xs[i], t.ys[i]),
                strokeWidth = (base * (0.25f + 0.75f * k)).coerceAtLeast(1f),
                cap = StrokeCap.Round,
            )
        }
    }

    override fun DrawScope.draw(c: Creature, time: Float) {
        val l = look(c.species)
        val lift = 1f + c.altitude * 0.3f
        creatureSpace(c.x, c.y, c.heading, c.length * lift) {
            val moving = (c.speed / (c.length * 2f)).coerceIn(0f, 1f)
            val phase = c.gait * 2f * PI.toFloat()
            val swing = sin(phase) * 0.07f * moving
            // Legs under the body.
            leg(l.skin, 0.2f + swing, -0.15f, 0.075f, 0.05f)
            leg(l.skin, 0.2f - swing, 0.15f, 0.075f, 0.05f)
            leg(l.skin, -0.27f - swing, -0.19f, 0.11f, 0.065f)
            leg(l.skin, -0.27f + swing, 0.19f, 0.11f, 0.065f)

            val breath = 1f + 0.025f * sin(c.age * 7f)
            withTransform({ scale(1f, breath, Offset.Zero) }) {
                with(sprites) {
                    drawSprite(c.species.ordinal, c.length) {
                        drawPath(l.bodyPath, l.body)
                        drawPath(l.furDarkPath, l.furDark, style = furStroke)
                        drawPath(l.furLightPath, l.furLight, style = furStroke)
                        drawCircle(headLight, radius = 0.12f, center = Offset(0.3f, -0.03f))
                    }
                }
            }

            val grooming = c.action == Action.GROOM
            if (grooming) {
                // Front paws rubbing the face.
                val rub = sin(c.actionTime * 14f) * 0.03f
                leg(l.skin, 0.44f + rub, -0.045f, 0.05f, 0.035f)
                leg(l.skin, 0.44f - rub, 0.045f, 0.05f, 0.035f)
            }

            // Ears, twitching now and then.
            val twitch = if (sin(c.age * 1.7f + c.seed) > 0.93f) sin(c.age * 40f) * 0.012f else 0f
            ear(l, 0.2f + twitch, -l.headWidth * 0.7f)
            ear(l, 0.2f - twitch, l.headWidth * 0.7f)

            eye(0.33f, -l.headWidth * 0.36f, 0.026f, eyeBlink(c), l.iris, l.lid)
            eye(0.33f, l.headWidth * 0.36f, 0.026f, eyeBlink(c), l.iris, l.lid)
            drawCircle(nose, radius = 0.02f, center = Offset(0.535f, 0f))

            val sniff = if (c.action == Action.SNIFF || c.state == app.purrhunt.game.engine.CreatureState.FREEZE) {
                sin(c.age * 24f) * 0.012f
            } else {
                sin(c.age * 5f) * 0.004f
            }
            whiskers(sniff)
        }
    }

    private fun eyeBlink(c: Creature): Float = if (c.blink > 0f) 1f else 0f

    private fun DrawScope.leg(color: Color, x: Float, y: Float, w: Float, h: Float) {
        drawOval(color, Offset(x - w / 2, y - h / 2), Size(w, h))
    }

    private fun DrawScope.ear(l: Look, x: Float, y: Float) {
        val s = l.earSize
        // Seen from above an ear is a thin rounded cup standing up from the head.
        drawOval(l.earOuter, Offset(x - s * 0.3f, y - s / 2), Size(s * 0.6f, s))
        drawOval(l.earInner, Offset(x - s * 0.12f, y - s * 0.36f), Size(s * 0.32f, s * 0.72f))
    }

    private fun DrawScope.whiskers(twitch: Float) {
        for (side in 0..1) {
            val sy = if (side == 0) -1f else 1f
            for (k in 0 until 3) {
                val spread = 0.07f + k * 0.055f
                drawLine(
                    whiskerColor,
                    Offset(0.47f, sy * 0.03f),
                    Offset(0.6f + k * 0.02f, sy * (spread + twitch * (k + 1))),
                    strokeWidth = whiskerStroke.width,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
