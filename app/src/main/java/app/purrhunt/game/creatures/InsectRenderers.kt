package app.purrhunt.game.creatures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import app.purrhunt.domain.Species
import app.purrhunt.game.engine.Action
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.render.RenderKit
import app.purrhunt.game.render.RenderKit.creatureSpace
import app.purrhunt.game.render.SpriteCache
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Six legs as two-segment lines; [phase] drives an alternating tripod gait, [wave] makes them flail. */
internal fun DrawScope.insectLegs(color: Color, phase: Float, moving: Float, reach: Float, width: Float, wave: Float = 0f) {
    for (i in 0 until 3) {
        val baseX = 0.08f - i * 0.12f
        val tripod = if (i % 2 == 0) 1f else -1f
        for (side in 0..1) {
            val s = if (side == 0) -1f else 1f
            val swing = sin(phase + (if (side == 0) 0f else PI.toFloat())) * tripod * 0.06f * moving
            val flail = sin(phase * 3f + i + side * 2f) * wave
            val kneeX = baseX + 0.02f + swing + flail * 0.5f
            val kneeY = s * reach * 0.6f
            val footX = baseX + (i - 1) * -0.12f + swing * 1.6f + flail
            val footY = s * reach * (1f + flail)
            drawLine(color, Offset(baseX, s * 0.04f), Offset(kneeX, kneeY), strokeWidth = width, cap = StrokeCap.Round)
            drawLine(color, Offset(kneeX, kneeY), Offset(footX, footY), strokeWidth = width * 0.8f, cap = StrokeCap.Round)
        }
    }
}

/** Butterflies: two wing pairs with species patterns, flapping by foreshortening, folding when resting. */
object ButterflyRenderer : CreatureRenderer {
    private val sprites = SpriteCache(Species.entries.size, 0.5f, 0.62f)
    private val forewing = Path().apply {
        moveTo(0.04f, -0.02f)
        cubicTo(0.12f, -0.2f, 0.26f, -0.42f, 0.18f, -0.55f)
        cubicTo(0.05f, -0.56f, -0.08f, -0.48f, -0.1f, -0.38f)
        cubicTo(-0.08f, -0.2f, -0.04f, -0.08f, -0.02f, -0.02f)
        close()
    }
    private val hindwing = Path().apply {
        moveTo(-0.01f, -0.02f)
        cubicTo(-0.02f, -0.2f, -0.12f, -0.4f, -0.26f, -0.38f)
        cubicTo(-0.36f, -0.3f, -0.34f, -0.14f, -0.2f, -0.03f)
        close()
    }
    private val swallowTail = Path().apply {
        moveTo(-0.28f, -0.3f)
        lineTo(-0.46f, -0.28f)
        lineTo(-0.42f, -0.24f)
        lineTo(-0.3f, -0.24f)
        close()
    }
    private val veins = Path().apply {
        for (k in 0 until 6) {
            moveTo(0.0f, -0.02f)
            val a = -1.2f - k * 0.28f
            lineTo(cos(a + 1.57f) * 0.05f + 0.1f * cos(a), -0.5f * abs(sin(a)) + 0.04f)
        }
        for (k in 0 until 4) {
            moveTo(-0.05f, -0.02f)
            lineTo(-0.12f - k * 0.05f, -0.3f + k * 0.04f)
        }
    }
    private val veinStroke = Stroke(0.006f)
    private val antennaStroke = Stroke(0.008f, cap = StrokeCap.Round)

    private fun DrawScope.paintWing(s: Species) {
        if (s == Species.SWALLOWTAIL) {
            drawPath(swallowTail, Color(0xFF1A1714))
            drawPath(forewing, Color(0xFFEFDA82))
            drawPath(hindwing, Color(0xFFEFDA82))
            clipPath(forewing) {
                // Black bars from the leading edge and a dark margin.
                for (k in 0 until 4) drawLine(Color(0xFF17140F), Offset(0.06f + k * 0.03f, -0.1f - k * 0.1f), Offset(-0.06f + k * 0.02f, -0.12f - k * 0.1f), strokeWidth = 0.04f)
                drawPath(forewing, Color(0xFF17140F), style = Stroke(0.08f))
            }
            clipPath(hindwing) {
                drawPath(hindwing, Color(0xFF17140F), style = Stroke(0.1f))
                for (k in 0 until 4) drawCircle(Color(0xFF5C8BD6), 0.018f, Offset(-0.14f - k * 0.04f, -0.33f + k * 0.04f))
                drawCircle(Color(0xFFE0682C), 0.03f, Offset(-0.22f, -0.07f))
            }
        } else {
            drawPath(forewing, Brush.radialGradient(listOf(Color(0xFFC75335), Color(0xFF8E2C1C)), Offset(0.02f, -0.15f), 0.45f))
            drawPath(hindwing, Brush.radialGradient(listOf(Color(0xFFB8462C), Color(0xFF6E2316)), Offset(-0.05f, -0.1f), 0.35f))
            clipPath(forewing) {
                drawPath(forewing, Color(0xFF2E1E19), style = Stroke(0.05f))
                // Peacock eyespot near the tip.
                val e = Offset(0.1f, -0.44f)
                drawCircle(Color(0xFFE8D6A0), 0.09f, e)
                drawCircle(Color(0xFF2A1A16), 0.068f, e)
                drawCircle(Color(0xFF6E8FC8), 0.05f, e)
                drawCircle(Color(0xFFF0E8D8), 0.018f, Offset(e.x - 0.015f, e.y - 0.01f))
                drawRect(Color(0xFF2B1D17), Offset(0.06f, -0.2f), Size(0.04f, 0.08f))
            }
            clipPath(hindwing) {
                drawPath(hindwing, Color(0xFF3A2620), style = Stroke(0.06f))
                val e = Offset(-0.22f, -0.27f)
                drawCircle(Color(0xFF1A1210), 0.075f, e)
                drawCircle(Color(0xFF4F72B5), 0.04f, e)
            }
        }
        drawPath(veins, Color.Black.copy(alpha = 0.25f), style = veinStroke)
    }

    override val shadowRx: Float get() = 0.3f
    override val shadowRy: Float get() = 0.3f

    override fun DrawScope.draw(c: Creature, time: Float) {
        val open = when (c.action) {
            Action.FOLD_WINGS -> 0.12f + 0.88f * (0.5f + 0.5f * cos(c.actionTime * 1.6f))
            Action.PERCHED -> 0.92f + 0.08f * sin(c.actionTime * 2f)
            else -> 0.12f + 0.88f * abs(cos(c.gait * PI.toFloat()))
        }
        val lift = 1f + c.altitude * 0.3f
        creatureSpace(c.x, c.y, c.heading, c.length * lift) {
            for (side in 0..1) {
                withTransform({ scale(1f, open * (if (side == 0) 1f else -1f), Offset.Zero) }) {
                    with(sprites) { drawSprite(c.species.ordinal, c.length) { paintWing(c.species) } }
                }
            }
            // Furry body, head and clubbed antennae.
            drawOval(Color(0xFF2A221D), Offset(-0.24f, -0.035f), Size(0.36f, 0.07f))
            drawOval(Color(0xFF4A3B30), Offset(-0.05f, -0.045f), Size(0.13f, 0.09f))
            drawCircle(Color(0xFF231C18), 0.04f, Offset(0.12f, 0f))
            drawLine(Color(0xFF231C18), Offset(0.14f, -0.01f), Offset(0.32f, -0.09f), strokeWidth = antennaStroke.width, cap = StrokeCap.Round)
            drawLine(Color(0xFF231C18), Offset(0.14f, 0.01f), Offset(0.32f, 0.09f), strokeWidth = antennaStroke.width, cap = StrokeCap.Round)
            drawCircle(Color(0xFF231C18), 0.016f, Offset(0.32f, -0.09f))
            drawCircle(Color(0xFF231C18), 0.016f, Offset(0.32f, 0.09f))
        }
    }
}

/** House fly: striped thorax, checkered abdomen, big red eyes, glassy wings that blur in flight. */
object FlyRenderer : CreatureRenderer {
    private val wingPath = Path().apply {
        moveTo(0.02f, -0.02f)
        cubicTo(-0.1f, -0.22f, -0.42f, -0.26f, -0.48f, -0.14f)
        cubicTo(-0.44f, -0.06f, -0.2f, -0.02f, 0.02f, -0.02f)
        close()
    }
    private val wingVeins = Path().apply {
        moveTo(0f, -0.03f); lineTo(-0.44f, -0.16f)
        moveTo(-0.05f, -0.06f); lineTo(-0.36f, -0.21f)
        moveTo(-0.1f, -0.04f); lineTo(-0.42f, -0.1f)
    }
    private val wingColor = Color(0x66DDE6EE)
    private val veinColor = Color(0x66383838)
    private val abdomen = RenderKit.bodyBrush(Color(0xFF9A948C), Color(0xFF6A645C), Color(0xFF2E2A26), centerX = -0.25f, centerY = -0.05f, radius = 0.25f)
    private val thorax = RenderKit.bodyBrush(Color(0xFF7D7A76), Color(0xFF4E4B48), Color(0xFF1F1D1B), centerX = 0.05f, centerY = -0.05f, radius = 0.2f)
    private val eyeBrush = Brush.radialGradient(listOf(Color(0xFFC0503A), Color(0xFF6E1E15)), Offset(0.26f, -0.06f), 0.1f)
    private val legColor = Color(0xFF1A1816)

    override val shadowRx: Float get() = 0.32f
    override val shadowRy: Float get() = 0.22f

    override fun DrawScope.draw(c: Creature, time: Float) {
        val lift = 1f + c.altitude * 0.4f
        creatureSpace(c.x, c.y, c.heading, c.length * lift) {
            val moving = (c.speed / (c.length * 2f)).coerceIn(0f, 1f)
            if (!c.flying) {
                val rub = c.action == Action.RUB_LEGS
                insectLegs(legColor, c.gait * 6.28f, if (rub) 0f else moving, 0.3f, 0.025f)
                if (rub) {
                    val r = sin(c.actionTime * 18f) * 0.03f
                    drawLine(legColor, Offset(0.15f, -0.04f), Offset(0.34f + r, -0.02f), strokeWidth = 0.025f, cap = StrokeCap.Round)
                    drawLine(legColor, Offset(0.15f, 0.04f), Offset(0.34f - r, 0.02f), strokeWidth = 0.025f, cap = StrokeCap.Round)
                }
            }
            drawOval(abdomen, Offset(-0.42f, -0.16f), Size(0.42f, 0.32f))
            for (k in 0 until 3) drawLine(Color(0x55000000), Offset(-0.33f + k * 0.09f, -0.14f), Offset(-0.33f + k * 0.09f, 0.14f), strokeWidth = 0.02f)
            drawOval(thorax, Offset(-0.05f, -0.15f), Size(0.28f, 0.3f))
            for (k in -1..1) drawLine(Color(0xFF151413), Offset(-0.02f, k * 0.06f), Offset(0.2f, k * 0.06f), strokeWidth = 0.022f)
            drawOval(eyeBrush, Offset(0.18f, -0.15f), Size(0.17f, 0.14f))
            drawOval(eyeBrush, Offset(0.18f, 0.01f), Size(0.17f, 0.14f))
            drawCircle(Color.White.copy(alpha = 0.35f), 0.02f, Offset(0.24f, -0.1f))
            drawCircle(Color.White.copy(alpha = 0.35f), 0.02f, Offset(0.24f, 0.06f))

            if (c.flying) {
                // Motion blur of beating wings: two faint positions per side.
                val a = sin(c.gait * 6.28f) * 25f
                for (k in 0..1) {
                    val spread = 35f + a * (if (k == 0) 1f else -1f)
                    withTransform({ rotate(-spread, Offset.Zero) }) { drawPath(wingPath, wingColor.copy(alpha = 0.22f)) }
                    withTransform({ scale(1f, -1f, Offset.Zero); rotate(-spread, Offset.Zero) }) { drawPath(wingPath, wingColor.copy(alpha = 0.22f)) }
                }
            } else {
                withTransform({ rotate(-12f, Offset.Zero) }) {
                    drawPath(wingPath, wingColor)
                    drawPath(wingVeins, veinColor, style = Stroke(0.008f))
                }
                withTransform({ scale(1f, -1f, Offset.Zero); rotate(-12f, Offset.Zero) }) {
                    drawPath(wingPath, wingColor)
                    drawPath(wingVeins, veinColor, style = Stroke(0.008f))
                }
            }
        }
    }
}

/** Fireflies: dark beetle with an orange shield and a pulsing yellow-green glow ([Creature.aux0]). */
object FireflyRenderer : CreatureRenderer {
    private val glow = Brush.radialGradient(
        0f to Color(0xFFF4FFB0),
        0.25f to Color(0xCCC8F25A),
        0.6f to Color(0x4082C83C),
        1f to Color.Transparent,
        center = Offset.Zero,
        radius = 1f,
    )

    override val shadowRx: Float get() = 0.2f
    override val shadowRy: Float get() = 0.15f

    override fun DrawScope.draw(c: Creature, time: Float) {
        val g = c.aux0.coerceIn(0f, 1f)
        val r = c.length * (1.2f + g * 1.4f)
        withTransform({
            translate(c.x - c.dirX * c.length * 0.25f, c.y - c.dirY * c.length * 0.25f)
            scale(r, r, Offset.Zero)
        }) { drawCircle(glow, 1f, Offset.Zero, alpha = 0.35f + 0.65f * g) }
        creatureSpace(c.x, c.y, c.heading, c.length) {
            val beat = abs(cos(c.gait * PI.toFloat()))
            for (side in 0..1) {
                val s = if (side == 0) -1f else 1f
                drawOval(Color(0x55E8F0F8), Offset(-0.3f, if (s < 0) -0.32f * beat - 0.02f else 0.02f), Size(0.45f, 0.3f * beat))
            }
            drawOval(Color(0xFFE6F59A).copy(alpha = 0.5f + 0.5f * g), Offset(-0.48f, -0.12f), Size(0.24f, 0.24f))
            drawOval(Color(0xFF2B241C), Offset(-0.32f, -0.15f), Size(0.42f, 0.3f))
            drawLine(Color(0xFF15110D), Offset(-0.3f, 0f), Offset(0.08f, 0f), strokeWidth = 0.02f)
            drawOval(Color(0xFFD9893C), Offset(0.06f, -0.13f), Size(0.2f, 0.26f))
            drawCircle(Color(0xFFB04040), 0.04f, Offset(0.16f, 0f))
            drawCircle(Color(0xFF1C1712), 0.05f, Offset(0.26f, 0f))
            drawLine(Color(0xFF1C1712), Offset(0.28f, -0.02f), Offset(0.44f, -0.12f), strokeWidth = 0.02f, cap = StrokeCap.Round)
            drawLine(Color(0xFF1C1712), Offset(0.28f, 0.02f), Offset(0.44f, 0.12f), strokeWidth = 0.02f, cap = StrokeCap.Round)
        }
    }
}

/**
 * Ladybird and ground beetle. Wing cases open before take-off and spread in flight; a flipped beetle
 * shows its underside with flailing legs.
 */
object CrawlerRenderer : CreatureRenderer {
    private val ladyShell = Brush.radialGradient(listOf(Color(0xFFF2584A), Color(0xFFC4241A), Color(0xFF7C1410)), Offset(-0.12f, -0.12f), 0.5f)
    private val beetleShell = Brush.radialGradient(listOf(Color(0xFF6A7A52), Color(0xFF2F3A27), Color(0xFF12160F)), Offset(-0.15f, -0.1f), 0.55f)
    private val underside = Brush.radialGradient(listOf(Color(0xFF6A4A32), Color(0xFF3A271A)), Offset(0f, 0f), 0.4f)
    private val hindWing = Color(0x88E2E0D8)
    private val ladyElytron = Path().apply {
        moveTo(0.12f, 0f)
        cubicTo(0.12f, -0.26f, -0.3f, -0.3f, -0.38f, -0.02f)
        lineTo(-0.38f, 0f)
        close()
    }
    private val beetleElytron = Path().apply {
        moveTo(0.1f, 0f)
        cubicTo(0.1f, -0.2f, -0.3f, -0.22f, -0.46f, -0.02f)
        lineTo(-0.46f, 0f)
        close()
    }
    private val grooves = Path().apply {
        for (k in 1..3) {
            moveTo(0.06f, -k * 0.045f)
            quadraticTo(-0.2f, -k * 0.05f, -0.4f, -k * 0.015f)
        }
    }
    private val ladySpots = floatArrayOf(-0.05f, -0.1f, -0.2f, -0.16f, -0.26f, -0.06f)

    override val shadowRx: Float get() = 0.4f
    override val shadowRy: Float get() = 0.3f

    override fun DrawScope.draw(c: Creature, time: Float) {
        val lady = c.species == Species.LADYBUG
        val lift = 1f + c.altitude * 0.4f
        creatureSpace(c.x, c.y, c.heading, c.length * lift) {
            val moving = (c.speed / (c.length * 2f)).coerceIn(0f, 1f)
            val phase = c.gait * 2f * PI.toFloat()
            if (c.action == Action.FLIPPED) {
                insectLegs(Color(0xFF1A1410), phase * 2f, 1f, 0.36f, 0.035f, wave = 0.08f)
                drawOval(underside, Offset(-0.4f, -0.2f), Size(0.7f, 0.4f))
                for (k in 0 until 4) drawLine(Color(0x55000000), Offset(-0.32f + k * 0.1f, -0.16f), Offset(-0.32f + k * 0.1f, 0.16f), strokeWidth = 0.015f)
                drawCircle(Color(0xFF2A1E16), 0.08f, Offset(0.32f, 0f))
                return@creatureSpace
            }
            if (!c.flying) insectLegs(Color(0xFF14110E), phase, moving, if (lady) 0.28f else 0.36f, if (lady) 0.03f else 0.032f)

            val opening = when {
                c.flying -> 1f
                c.action == Action.OPEN_WINGS -> (c.actionTime / 0.6f).coerceIn(0f, 1f)
                else -> 0f
            }
            if (opening > 0f) {
                val beat = if (c.flying) abs(cos(c.gait * PI.toFloat() * 3f)) else 0.4f
                for (side in 0..1) {
                    val s = if (side == 0) -1f else 1f
                    drawOval(hindWing, Offset(-0.38f, if (s < 0) -0.06f - 0.5f * beat * opening else 0.06f), Size(0.42f, 0.5f * beat * opening))
                }
            }
            val elytron = if (lady) ladyElytron else beetleElytron
            for (side in 0..1) {
                val s = if (side == 0) 1f else -1f
                withTransform({
                    scale(1f, s, Offset.Zero)
                    rotate(-opening * 35f, Offset(0.1f, 0f))
                }) {
                    drawPath(elytron, if (lady) ladyShell else beetleShell)
                    if (lady) {
                        for (k in ladySpots.indices step 2) drawCircle(Color(0xFF120A08), 0.045f, Offset(ladySpots[k], ladySpots[k + 1]))
                    } else {
                        drawPath(grooves, Color(0x66000000), style = Stroke(0.012f))
                        drawPath(grooves, Color(0x337DA0FF), style = Stroke(0.006f))
                    }
                }
            }
            if (lady) drawCircle(Color(0xFF120A08), 0.04f, Offset(-0.02f, 0f))
            // Pronotum and head.
            if (lady) {
                drawOval(Color(0xFF15110F), Offset(0.06f, -0.15f), Size(0.14f, 0.3f))
                drawOval(Color(0xFFF2EEE6), Offset(0.1f, -0.13f), Size(0.06f, 0.07f))
                drawOval(Color(0xFFF2EEE6), Offset(0.1f, 0.06f), Size(0.06f, 0.07f))
                drawOval(Color(0xFF0E0B09), Offset(0.17f, -0.07f), Size(0.09f, 0.14f))
            } else {
                drawOval(beetleShell, Offset(0.06f, -0.14f), Size(0.2f, 0.28f))
                drawOval(Color(0xFF14180F), Offset(0.22f, -0.07f), Size(0.13f, 0.14f))
                drawLine(Color(0xFF0E0F0B), Offset(0.34f, -0.03f), Offset(0.4f, -0.05f), strokeWidth = 0.02f)
                drawLine(Color(0xFF0E0F0B), Offset(0.34f, 0.03f), Offset(0.4f, 0.05f), strokeWidth = 0.02f)
            }
            val twitch = sin(c.age * 9f + c.seed) * 0.02f
            val ant = if (lady) 0.1f else 0.28f
            drawLine(Color(0xFF14110E), Offset(0.26f, -0.04f), Offset(0.26f + ant, -0.08f - ant * 0.5f + twitch), strokeWidth = 0.016f, cap = StrokeCap.Round)
            drawLine(Color(0xFF14110E), Offset(0.26f, 0.04f), Offset(0.26f + ant, 0.08f + ant * 0.5f - twitch), strokeWidth = 0.016f, cap = StrokeCap.Round)
            drawCircle(Color.White.copy(alpha = 0.25f), 0.05f, Offset(-0.1f, -0.1f))
        }
    }
}

/** Field cricket: big head, long antennae, powerful hind legs that extend while jumping, chirping wings. */
object CricketRenderer : CreatureRenderer {
    private val body = RenderKit.bodyBrush(Color(0xFF5C4632), Color(0xFF34261A), Color(0xFF140E09), centerX = -0.05f, centerY = -0.06f, radius = 0.4f)
    private val wings = Path().apply {
        moveTo(0.05f, -0.1f)
        lineTo(-0.38f, -0.08f)
        quadraticTo(-0.44f, 0f, -0.38f, 0.08f)
        lineTo(0.05f, 0.1f)
        close()
    }
    private val wingLines = Path().apply {
        for (k in 0 until 6) {
            moveTo(0.02f - k * 0.07f, -0.09f)
            lineTo(-0.02f - k * 0.07f, 0.09f)
        }
        moveTo(0.04f, 0f); lineTo(-0.4f, 0f)
    }
    private val leg = Color(0xFF241A11)

    override val shadowRx: Float get() = 0.4f
    override val shadowRy: Float get() = 0.2f

    override fun DrawScope.draw(c: Creature, time: Float) {
        val lift = 1f + c.altitude * 0.5f
        creatureSpace(c.x, c.y, c.heading, c.length * lift) {
            val jump = (c.altitude * 1.5f).coerceIn(0f, 1f)
            // Hind legs: folded zig-zag at rest, stretched back during a jump.
            for (side in 0..1) {
                val s = if (side == 0) -1f else 1f
                val knee = Offset(-0.1f - jump * 0.25f, s * (0.16f - jump * 0.06f))
                val foot = Offset(-0.42f - jump * 0.3f, s * (0.12f + jump * 0.04f))
                drawLine(leg, Offset(-0.05f, s * 0.05f), knee, strokeWidth = 0.07f, cap = StrokeCap.Round)
                drawLine(leg, knee, foot, strokeWidth = 0.035f, cap = StrokeCap.Round)
                drawLine(leg, Offset(0.12f, s * 0.05f), Offset(0.22f, s * 0.16f), strokeWidth = 0.025f, cap = StrokeCap.Round)
                drawLine(leg, Offset(0.02f, s * 0.06f), Offset(0.04f, s * 0.2f), strokeWidth = 0.025f, cap = StrokeCap.Round)
            }
            drawOval(body, Offset(-0.38f, -0.12f), Size(0.62f, 0.24f))
            val chirp = if (c.action == Action.CHIRP && (c.actionTime % 1.6f) < 0.5f) sin(c.age * 90f) * 0.012f else 0f
            withTransform({ translate(0f, chirp) ; scale(1f, if (chirp != 0f) 1.08f else 1f, Offset.Zero) }) {
                drawPath(wings, Color(0xFF2A1F15))
                drawPath(wingLines, Color(0x55C9A97A), style = Stroke(0.008f))
            }
            drawOval(Color(0xFF18110B), Offset(0.12f, -0.11f), Size(0.18f, 0.22f))
            drawOval(Color(0xFF231810), Offset(0.24f, -0.09f), Size(0.14f, 0.18f))
            drawCircle(Color(0xFF080605), 0.025f, Offset(0.32f, -0.06f))
            drawCircle(Color(0xFF080605), 0.025f, Offset(0.32f, 0.06f))
            val sway = sin(c.age * 2.5f + c.seed) * 0.08f
            drawLine(leg, Offset(0.36f, -0.03f), Offset(0.7f, -0.25f + sway), strokeWidth = 0.012f, cap = StrokeCap.Round)
            drawLine(leg, Offset(0.36f, 0.03f), Offset(0.72f, 0.2f - sway), strokeWidth = 0.012f, cap = StrokeCap.Round)
            drawLine(leg, Offset(-0.36f, -0.03f), Offset(-0.5f, -0.06f), strokeWidth = 0.01f)
            drawLine(leg, Offset(-0.36f, 0.03f), Offset(-0.5f, 0.06f), strokeWidth = 0.01f)
            drawCircle(Color.White.copy(alpha = 0.12f), 0.08f, Offset(0.0f, -0.05f))
        }
    }
}

