package app.purrhunt.game.scenes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import app.purrhunt.domain.Season
import app.purrhunt.game.engine.World
import app.purrhunt.game.scenes.NatureKit.bush
import app.purrhunt.game.scenes.NatureKit.fallenLeaves
import app.purrhunt.game.scenes.NatureKit.flower
import app.purrhunt.game.scenes.NatureKit.grassTuft
import app.purrhunt.game.scenes.NatureKit.lawn
import app.purrhunt.game.scenes.NatureKit.leaf
import app.purrhunt.game.scenes.NatureKit.snowPatches
import app.purrhunt.game.scenes.NatureKit.stone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val NIGHT_TINT = Color(0xFF07102A)

private fun DrawScope.vignette(alpha: Float) {
    drawRect(
        Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = alpha),
            center = Offset(size.width / 2, size.height / 2),
            radius = maxOf(size.width, size.height) * 0.75f,
        ),
    )
}

/** Night: everything darkens and turns blue, with soft moonlight from the top. */
private fun DrawScope.nightOver(onlyDrawn: Boolean) {
    val mode = if (onlyDrawn) BlendMode.SrcAtop else BlendMode.SrcOver
    drawRect(NIGHT_TINT, alpha = 0.62f, blendMode = mode)
    drawRect(
        Brush.radialGradient(listOf(Color(0x3390A8E0), Color.Transparent), Offset(size.width * 0.7f, 0f), maxOf(size.width, size.height) * 0.7f),
        blendMode = mode,
    )
}

/** Garden: lawn with seed trays, bushes and an overhanging branch. Snow in winter, leaves in autumn. */
object GardenArt : SceneArt {
    private val grass = SwayingGrass(160, 31)
    private val grassColors = arrayOf(Color(0xFF34482A), Color(0xFF4C6638), Color(0xFF6A8448))
    private val winterGrass = arrayOf(Color(0xFF55604E), Color(0xFF79847A), Color(0xFFB9C3C8))
    private val anywhere: (SceneLayout, Float, Float) -> Boolean = { _, _, _ -> true }

    override fun DrawScope.background(layout: SceneLayout, look: SceneLook) {
        val u = layout.dp
        val rnd = Random(3)
        val base = NatureKit.groundTint(look.season, Color(0xFF4C6638))
        drawRect(base)
        lawn(rnd, u, base, lerp(base, Color.Black, 0.35f), lerp(base, Color(0xFFB8C27A), 0.35f), 2600)
        // Worn dirt around the seed trays.
        for (f in layout.feeders) {
            drawCircle(Brush.radialGradient(listOf(Color(0xCC6B5638), Color.Transparent), Offset(f.x, f.y), f.radius * 2.2f), f.radius * 2.2f, Offset(f.x, f.y))
        }
        for (f in layout.feeders) seedTray(f, u, rnd)
        // Shadow of the branch.
        drawPath(branchPath(layout, 18f * u, 22f * u), Color.Black.copy(alpha = 0.28f), style = Stroke(16f * u, cap = StrokeCap.Round))
        when (look.season) {
            Season.WINTER -> snowPatches(rnd, u, 40) { _, _ -> true }
            Season.AUTUMN -> fallenLeaves(rnd, u, 70) { _, _ -> true }
            else -> Unit
        }
        vignette(0.5f)
        if (look.night) nightOver(false)
    }

    private fun DrawScope.seedTray(f: HidingSpot, u: Float, rnd: Random) {
        val r = f.radius * 0.7f
        drawRect(Color.Black.copy(alpha = 0.3f), Offset(f.x - r + 4f * u, f.y - r + 6f * u), Size(r * 2, r * 2))
        drawRect(Color(0xFF7A5A3A), Offset(f.x - r, f.y - r), Size(r * 2, r * 2))
        drawRect(Color(0xFF4E3824), Offset(f.x - r * 0.82f, f.y - r * 0.82f), Size(r * 1.64f, r * 1.64f))
        repeat(40) {
            drawOval(
                if (rnd.nextBoolean()) Color(0xFFD8C08A) else Color(0xFF2B2622),
                Offset(f.x + (rnd.nextFloat() - 0.5f) * r * 1.5f, f.y + (rnd.nextFloat() - 0.5f) * r * 1.5f),
                Size(2.8f * u, 1.8f * u),
            )
        }
    }

    private fun branchPath(layout: SceneLayout, dx: Float, dy: Float): Path = Path().apply {
        val w = layout.width
        val h = layout.height
        moveTo(w + 20f + dx, h * 0.06f + dy)
        cubicTo(w * 0.75f + dx, h * 0.1f + dy, w * 0.55f + dx, h * 0.02f + dy, w * 0.38f + dx, h * 0.07f + dy)
    }

    override fun DrawScope.foreground(layout: SceneLayout, look: SceneLook) {
        val u = layout.dp
        val rnd = Random(5)
        val snow = look.season == Season.WINTER
        for (s in layout.spots) if (s.kind == SpotKind.BUSH) {
            bush(s.x, s.y, s.radius, rnd, if (look.season == Season.AUTUMN) Color(0xFF6F6A30) else Color(0xFF3F6034), Color(0xFF1D2E18), snow)
        }
        // The overhanging branch with a few twigs and leaves.
        val branch = branchPath(layout, 0f, 0f)
        drawPath(branch, Color(0xFF4A3626), style = Stroke(14f * u, cap = StrokeCap.Round))
        drawPath(branch, Color(0xFF6E5238), style = Stroke(5f * u, cap = StrokeCap.Round))
        val w = layout.width
        val h = layout.height
        for (k in 0 until 6) {
            val bx = w * (0.45f + k * 0.1f)
            val by = h * (0.06f + sin(k * 1.3f) * 0.02f)
            drawLine(Color(0xFF4A3626), Offset(bx, by), Offset(bx + 16f * u, by + (if (k % 2 == 0) 24f else -20f) * u), strokeWidth = 4f * u, cap = StrokeCap.Round)
            if (look.season != Season.WINTER) leaf(bx + 18f * u, by + (if (k % 2 == 0) 28f else -24f) * u, 10f * u, k * 60f, Color(0xFF4F7A3C))
        }
        if (snow) drawPath(branchPath(layout, 0f, -4f * u), Color(0xFFEFF3F7), style = Stroke(6f * u, cap = StrokeCap.Round))
        if (look.night) nightOver(true)
    }

    override fun DrawScope.animatedOver(layout: SceneLayout, look: SceneLook, time: Float) {
        with(grass) { draw(layout, time, if (look.season == Season.WINTER) winterGrass else grassColors, anywhere) }
    }
}

/** Meadow: lawn with daisies and cornflowers, stones and grass clumps; a dark blue night variant. */
object MeadowArt : SceneArt {
    private val grass = SwayingGrass(200, 41)
    private val dayColors = arrayOf(Color(0xFF3E5226), Color(0xFF5E7638), Color(0xFF8C9A4E))
    private val nightColors = arrayOf(Color(0xFF14202A), Color(0xFF1C2C34), Color(0xFF2A3C44))
    private val anywhere: (SceneLayout, Float, Float) -> Boolean = { _, _, _ -> true }

    override fun DrawScope.background(layout: SceneLayout, look: SceneLook) {
        val u = layout.dp
        val rnd = Random(9)
        val base = NatureKit.groundTint(look.season, Color(0xFF5B7238))
        drawRect(base)
        lawn(rnd, u, base, lerp(base, Color.Black, 0.35f), lerp(base, Color(0xFFD0C878), 0.35f), 3000)
        // Scattered small flowers for texture; the big ones are butterfly landing spots.
        repeat(30) {
            flower(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height, (5f + rnd.nextFloat() * 4f) * u, rnd, rnd.nextFloat() < 0.4f)
        }
        for ((i, f) in layout.flowers.withIndex()) flower(f.x, f.y, f.radius, rnd, i % 2 == 1)
        when (look.season) {
            Season.WINTER -> snowPatches(rnd, u, 30) { _, _ -> true }
            Season.AUTUMN -> fallenLeaves(rnd, u, 60) { _, _ -> true }
            else -> Unit
        }
        vignette(0.5f)
        if (look.night) nightOver(false)
    }

    override fun DrawScope.foreground(layout: SceneLayout, look: SceneLook) {
        val rnd = Random(13)
        for (s in layout.spots) when (s.kind) {
            SpotKind.STONE -> stone(s.x, s.y, s.radius, rnd, moss = true)
            SpotKind.GRASS -> grassTuft(s.x, s.y, s.radius, rnd, Color(0xFF2E4220), Color(0xFF7E924A))
            else -> Unit
        }
        if (look.night) nightOver(true)
    }

    override fun DrawScope.animatedOver(layout: SceneLayout, look: SceneLook, time: Float) {
        with(grass) { draw(layout, time, if (look.night) nightColors else dayColors, anywhere) }
    }
}

/** Water helpers shared by the pond and the mixed scene. */
object WaterKit {
    private val caustic = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.10f),
        0.6f to Color.White.copy(alpha = 0.04f),
        1f to Color.Transparent,
        center = Offset.Zero,
        radius = 1f,
    )
    private val glintStroke = Stroke(2f, cap = StrokeCap.Round)

    fun DrawScope.water(area: WaterArea, u: Float, rnd: Random, season: Season) {
        val oval = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(area.cx - area.rx, area.cy - area.ry, area.cx + area.rx, area.cy + area.ry))
        }
        // Muddy bank ring under the water edge.
        drawOval(Color(0xFF4A4230), Offset(area.cx - area.rx - 14f * u, area.cy - area.ry - 14f * u), Size((area.rx + 14f * u) * 2, (area.ry + 14f * u) * 2))
        drawPath(
            oval,
            Brush.radialGradient(
                0f to Color(0xFF16323A),
                0.6f to Color(0xFF214545),
                0.92f to Color(0xFF3A5A4A),
                1f to Color(0xFF55664A),
                center = Offset(area.cx, area.cy),
                radius = maxOf(area.rx, area.ry),
            ),
        )
        clipPath(oval) {
            // Stones and weeds on the bottom, seen through the water.
            repeat(26) {
                val a = rnd.nextFloat() * 2f * PI.toFloat()
                val d = kotlin.math.sqrt(rnd.nextFloat())
                val x = area.cx + cos(a) * area.rx * d
                val y = area.cy + sin(a) * area.ry * d
                val r = (10f + rnd.nextFloat() * 26f) * u
                drawOval(Color(0xFF5F6A5A).copy(alpha = 0.18f + rnd.nextFloat() * 0.12f), Offset(x - r, y - r * 0.7f), Size(r * 2, r * 1.4f))
            }
            repeat(18) {
                val x = area.cx + (rnd.nextFloat() - 0.5f) * area.rx * 1.6f
                val y = area.cy + (rnd.nextFloat() - 0.5f) * area.ry * 1.6f
                val p = Path().apply {
                    moveTo(x, y)
                    quadraticTo(x + 14f * u, y - 20f * u, x + (rnd.nextFloat() - 0.5f) * 40f * u, y - 46f * u)
                }
                drawPath(p, Color(0xFF3A5A30).copy(alpha = 0.35f), style = Stroke(3f * u, cap = StrokeCap.Round))
            }
            if (season == Season.WINTER) {
                drawPath(oval, Color(0xCCDCE8F0), style = Stroke(34f * u))
                drawPath(oval, Color(0x66FFFFFF), style = Stroke(10f * u))
            }
        }
    }

    /** Moving caustic light and surface glints, inside the water only. */
    fun DrawScope.waterLight(area: WaterArea, u: Float, time: Float) {
        for (i in 0 until 12) {
            val fx = sin(time * 0.13f + i * 1.7f) * 0.6f
            val fy = cos(time * 0.11f + i * 2.3f) * 0.6f
            val x = area.cx + area.rx * fx
            val y = area.cy + area.ry * fy
            val r = (60f + 30f * sin(time * 0.5f + i)) * u
            withTransform({
                translate(x, y)
                scale(r, r * 0.7f, Offset.Zero)
            }) { drawCircle(caustic, 1f, Offset.Zero) }
        }
        for (i in 0 until 16) {
            val phase = (time * 0.35f + i * 0.618f) % 1f
            val fx = ((i * 0.377f) % 1f - 0.5f) * 1.4f
            val fy = ((i * 0.611f) % 1f - 0.5f) * 1.4f
            val x = area.cx + area.rx * fx
            val y = area.cy + area.ry * fy
            if (!area.contains(x, y, 20f * u)) continue
            val a = sin(phase * PI.toFloat())
            drawLine(Color.White, Offset(x - 8f * u, y), Offset(x + 8f * u, y + 1f * u), strokeWidth = glintStroke.width * u, cap = StrokeCap.Round, alpha = a * 0.35f)
        }
    }

    fun DrawScope.lily(s: HidingSpot, rnd: Random, flowerColor: Color?) {
        val r = s.radius
        val notch = rnd.nextFloat() * 360f
        drawCircle(Color.Black.copy(alpha = 0.25f), r, Offset(s.x + r * 0.12f, s.y + r * 0.15f))
        drawArc(
            Brush.radialGradient(listOf(Color(0xFF5E8A40), Color(0xFF3C6A2C), Color(0xFF244A1C)), Offset(s.x - r * 0.3f, s.y - r * 0.3f), r * 1.4f),
            notch + 12f, 336f, true, Offset(s.x - r, s.y - r), Size(r * 2, r * 2),
        )
        for (k in 0 until 9) {
            val a = (notch + 20f + k * 36f) * PI.toFloat() / 180f
            drawLine(Color(0x55203C18), Offset(s.x, s.y), Offset(s.x + cos(a) * r * 0.9f, s.y + sin(a) * r * 0.9f), strokeWidth = r * 0.03f)
        }
        if (flowerColor != null) {
            val fx = s.x + r * 0.35f
            val fy = s.y - r * 0.2f
            for (k in 0 until 10) rotate(k * 36f, Offset(fx, fy)) {
                drawOval(flowerColor, Offset(fx, fy - r * 0.07f), Size(r * 0.38f, r * 0.14f))
            }
            drawCircle(Color(0xFFE8C040), r * 0.09f, Offset(fx, fy))
        }
    }

    fun DrawScope.reeds(s: HidingSpot, rnd: Random, u: Float) {
        repeat(36) {
            val a = rnd.nextFloat() * 2f * PI.toFloat()
            val len = s.radius * (0.6f + rnd.nextFloat() * 0.8f)
            val bx = s.x + (rnd.nextFloat() - 0.5f) * s.radius * 0.5f
            val by = s.y + (rnd.nextFloat() - 0.5f) * s.radius * 0.5f
            drawLine(lerp(Color(0xFF3E5A2A), Color(0xFF8A9A52), rnd.nextFloat()), Offset(bx, by), Offset(bx + cos(a) * len, by + sin(a) * len), strokeWidth = 3f * u, cap = StrokeCap.Round)
        }
        repeat(5) {
            val x = s.x + (rnd.nextFloat() - 0.5f) * s.radius
            val y = s.y + (rnd.nextFloat() - 0.5f) * s.radius
            drawOval(Color(0xFF5A3A22), Offset(x - 4f * u, y - 12f * u), Size(8f * u, 24f * u))
        }
    }
}

/** Pond: deep water with caustics, lily pads and reeds; leaves float in autumn, ice rims in winter. */
object PondArt : SceneArt {
    override fun DrawScope.background(layout: SceneLayout, look: SceneLook) {
        val u = layout.dp
        val rnd = Random(17)
        val bank = NatureKit.groundTint(look.season, Color(0xFF55663E))
        drawRect(bank)
        lawn(rnd, u, bank, lerp(bank, Color.Black, 0.35f), lerp(bank, Color(0xFFC8C27A), 0.3f), 1600)
        val water = layout.water ?: return
        with(WaterKit) { water(water, u, rnd, look.season) }
        if (look.season == Season.WINTER) snowPatches(rnd, u, 30) { x, y -> !layout.isWater(x, y, -10f * u) }
        vignette(0.55f)
        if (look.night) nightOver(false)
    }

    override fun DrawScope.foreground(layout: SceneLayout, look: SceneLook) {
        val u = layout.dp
        val rnd = Random(19)
        for ((i, s) in layout.spots.withIndex()) when (s.kind) {
            SpotKind.LILY -> with(WaterKit) { lily(s, rnd, if (i % 2 == 0) Color(0xFFF2E6EE) else null) }
            SpotKind.REED -> with(WaterKit) { reeds(s, rnd, u) }
            else -> Unit
        }
        if (look.season == Season.AUTUMN) {
            val water = layout.water
            fallenLeaves(rnd, u, 18) { x, y -> water?.contains(x, y, 30f * u) == true }
        }
        if (look.night) nightOver(true)
    }

    override fun DrawScope.animatedUnder(layout: SceneLayout, look: SceneLook, time: Float, world: World?) {
        val water = layout.water ?: return
        with(WaterKit) { waterLight(water, layout.dp, time) }
    }
}

/** Mixed scene: a pond corner with lawn, a stone with a burrow, a bush and flowers. */
object MixArt : SceneArt {
    private val grass = SwayingGrass(140, 51)
    private val colors = arrayOf(Color(0xFF34482A), Color(0xFF4C6638), Color(0xFF7A8E4A))
    private val dry: (SceneLayout, Float, Float) -> Boolean = { l, x, y -> !l.isWater(x, y, -24f * l.dp) }

    override fun DrawScope.background(layout: SceneLayout, look: SceneLook) {
        val u = layout.dp
        val rnd = Random(23)
        val base = NatureKit.groundTint(look.season, Color(0xFF526C3A))
        drawRect(base)
        lawn(rnd, u, base, lerp(base, Color.Black, 0.35f), lerp(base, Color(0xFFC8C27A), 0.3f), 2400)
        layout.water?.let { with(WaterKit) { water(it, u, rnd, look.season) } }
        for ((i, f) in layout.flowers.withIndex()) flower(f.x, f.y, f.radius, rnd, i % 2 == 0)
        for (b in layout.burrows) {
            drawOval(Color(0xFF3A2A1C), Offset(b.x - b.radius * 1.4f, b.y - b.radius), Size(b.radius * 2.8f, b.radius * 2f))
            drawOval(Brush.radialGradient(listOf(Color(0xFF050302), Color(0xFF1E140C)), Offset(b.x, b.y), b.radius), Offset(b.x - b.radius, b.y - b.radius * 0.7f), Size(b.radius * 2, b.radius * 1.4f))
        }
        when (look.season) {
            Season.WINTER -> snowPatches(rnd, u, 24) { x, y -> !layout.isWater(x, y, -10f * u) }
            Season.AUTUMN -> fallenLeaves(rnd, u, 40) { x, y -> !layout.isWater(x, y, -10f * u) }
            else -> Unit
        }
        vignette(0.5f)
        if (look.night) nightOver(false)
    }

    override fun DrawScope.foreground(layout: SceneLayout, look: SceneLook) {
        val u = layout.dp
        val rnd = Random(29)
        for ((i, s) in layout.spots.withIndex()) when (s.kind) {
            SpotKind.STONE -> stone(s.x, s.y, s.radius, rnd, moss = true)
            SpotKind.BUSH -> bush(s.x, s.y, s.radius, rnd, Color(0xFF3F6034), Color(0xFF1D2E18), look.season == Season.WINTER)
            SpotKind.GRASS -> grassTuft(s.x, s.y, s.radius, rnd, Color(0xFF2E4220), Color(0xFF7E924A))
            SpotKind.LILY -> with(WaterKit) { lily(s, rnd, if (i % 2 == 0) Color(0xFFF2E6EE) else null) }
            else -> Unit
        }
        if (look.season == Season.AUTUMN) {
            val water = layout.water
            fallenLeaves(rnd, u, 10) { x, y -> water?.contains(x, y, 30f * u) == true }
        }
        if (look.night) nightOver(true)
    }

    override fun DrawScope.animatedUnder(layout: SceneLayout, look: SceneLook, time: Float, world: World?) {
        val water = layout.water ?: return
        with(WaterKit) { waterLight(water, layout.dp, time) }
    }

    override fun DrawScope.animatedOver(layout: SceneLayout, look: SceneLook, time: Float) {
        with(grass) { draw(layout, time, colors, dry) }
    }
}
