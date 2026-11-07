package app.purrhunt.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import app.purrhunt.domain.GameMode
import app.purrhunt.game.creatures.CreatureRenderers
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.engine.GameEngine
import app.purrhunt.game.engine.World
import app.purrhunt.game.render.RenderKit.contrastHalo
import app.purrhunt.game.render.RenderKit.softShadow
import app.purrhunt.game.scenes.Bitmaps
import app.purrhunt.game.scenes.SceneArt
import app.purrhunt.game.scenes.SceneLayout
import app.purrhunt.game.scenes.SceneLook
import app.purrhunt.ui.components.drawPaw
import kotlin.math.sin

/**
 * Draws a frame of the game into any [DrawScope]. The Compose host draws into its Canvas; the same
 * renderer can draw into a SurfaceView canvas through CanvasDrawScope if a device needs a render thread.
 */
interface FrameRenderer {
    fun DrawScope.render(engine: GameEngine, time: Float)
}

class SceneRenderer(
    private val look: SceneLook,
    private val catContrast: Boolean,
) : FrameRenderer {
    private var cachedFor: SceneLayout? = null
    private var background: ImageBitmap? = null
    private var foreground: ImageBitmap? = null
    private var sceneArt: SceneArt? = null

    private val layerPaint = Paint()
    private var layerBounds = Rect.Zero
    private var stringStroke = Stroke(1f)
    private val ringStroke = Stroke(width = 6f, cap = StrokeCap.Round)
    private val rippleStroke = Stroke(width = 2f)
    private val stringPath = Path()
    private val dotGlow = Brush.radialGradient(
        0f to Color(0xFFE8FFFB),
        0.18f to Color(0xFF3CF0D2),
        0.45f to Color(0x6625C8E6),
        1f to Color.Transparent,
        center = Offset.Zero,
        radius = 1f,
    )

    fun prepare(engine: GameEngine, density: Float) {
        val layout = engine.world.layout
        if (cachedFor === layout) return
        val art = SceneArt.of(engine.spec.scene.location)
        val w = layout.width.toInt()
        val h = layout.height.toInt()
        background = Bitmaps.render(w, h, density) { with(art) { background(layout, look) } }
        foreground = Bitmaps.render(w, h, density) { with(art) { foreground(layout, look) } }
        sceneArt = art
        cachedFor = layout
    }

    override fun DrawScope.render(engine: GameEngine, time: Float) {
        val art = sceneArt ?: return
        val world = engine.world
        val layout = world.layout
        background?.let { drawImage(it) }
        with(art) { animatedUnder(layout, look, time, world) }
        drawRipples(world)

        if (engine.spec.mode == GameMode.HUNT) {
            for (c in world.creatures) if (c.active && !c.flying && c.altitude < 0.5f) drawCreature(c, time)
        }
        foreground?.let { drawImage(it) }
        if (engine.spec.mode == GameMode.HUNT) {
            for (c in world.creatures) if (c.active && (c.flying || c.altitude >= 0.5f)) drawCreature(c, time)
        }
        when (engine.spec.mode) {
            GameMode.LASER -> drawLaser(engine, world)
            GameMode.FEATHER -> drawFeather(engine, world, time)
            GameMode.HUNT -> Unit
        }
        drawMarks(world)
        with(art) { animatedOver(layout, look, time) }

        if (engine.dusk > 0f) drawRect(Color(0xFF050814), alpha = engine.dusk * 0.85f)
        drawExitRing(engine)
    }

    private fun DrawScope.drawCreature(c: Creature, time: Float) {
        if (c.visibility < 0.02f) return
        val r = CreatureRenderers.of(c.species.family)
        val clip = !c.clipAboveY.isNaN()
        val faded = c.visibility < 0.98f
        if (faded) {
            layerPaint.alpha = c.visibility
            if (layerBounds.width != size.width || layerBounds.height != size.height) layerBounds = Rect(0f, 0f, size.width, size.height)
            drawContext.canvas.saveLayer(layerBounds, layerPaint)
        }
        if (clip) {
            clipRect(top = c.clipAboveY) { creatureLayers(r, c, time) }
        } else {
            creatureLayers(r, c, time)
        }
        if (faded) drawContext.canvas.restore()
    }

    private fun DrawScope.creatureLayers(r: app.purrhunt.game.creatures.CreatureRenderer, c: Creature, time: Float) {
        softShadow(c.x, c.y, c.length * r.shadowRx, c.length * r.shadowRy, c.heading, c.altitude, alpha = 0.8f)
        if (catContrast) contrastHalo(c.x, c.y, c.length * 0.8f, 0.6f)
        with(r) {
            drawWorldParts(c)
            draw(c, time)
        }
    }

    private fun DrawScope.drawRipples(world: World) {
        val pool = world.ripples
        for (i in 0 until pool.capacity) {
            if (!pool.alive(i)) continue
            val p = pool.progress(i)
            val radius = pool.sizes[i] * (0.2f + p)
            drawCircle(Color.White, radius = radius, center = Offset(pool.xs[i], pool.ys[i]), alpha = (1f - p) * 0.35f, style = rippleStroke)
            drawCircle(Color.White, radius = radius * 0.6f, center = Offset(pool.xs[i], pool.ys[i]), alpha = (1f - p) * 0.2f, style = rippleStroke)
        }
    }

    private fun DrawScope.drawMarks(world: World) {
        val pool = world.marks
        val extent = 30f * world.dp
        for (i in 0 until pool.capacity) {
            if (!pool.alive(i)) continue
            val p = pool.progress(i)
            withTransform({ rotate(pool.angles[i] * 57.3f, Offset(pool.xs[i], pool.ys[i])) }) {
                drawPaw(Color(0xFFF6EEDD), Offset(pool.xs[i], pool.ys[i]), extent * pool.sizes[i] * (0.9f + p * 0.2f), alpha = (1f - p) * 0.5f)
            }
        }
    }

    private fun DrawScope.drawLaser(engine: GameEngine, world: World) {
        val dot = engine.laser
        if (!dot.visible) return
        val r = dot.radiusDp * world.dp
        drawLine(Color(0xFF3CF0D2), Offset(dot.trailX, dot.trailY), Offset(dot.x, dot.y), strokeWidth = r * 0.9f, cap = StrokeCap.Round, alpha = 0.35f)
        val pulse = 1f + 0.08f * sin(world.time.toFloat() * 9f) + dot.flash * 0.8f
        withTransform({
            translate(dot.x, dot.y)
            scale(r * 4f * pulse, r * 4f * pulse, Offset.Zero)
        }) { drawCircle(dotGlow, radius = 1f, center = Offset.Zero) }
        drawCircle(Color(0xFFF2FFFD), radius = r * 0.45f, center = Offset(dot.x, dot.y))
    }

    private fun DrawScope.drawFeather(engine: GameEngine, world: World, time: Float) {
        val f = engine.feather
        stringPath.reset()
        stringPath.moveTo(f.xs[0], f.ys[0])
        for (i in 1 until f.segments) stringPath.lineTo(f.xs[i], f.ys[i])
        if (stringStroke.width != 1.6f * world.dp) stringStroke = Stroke(1.6f * world.dp)
        drawPath(stringPath, Color(0xFFE6DCCB), alpha = 0.75f, style = stringStroke)
        drawCircle(Color(0xFF3A3A3A), radius = 4f * world.dp, center = Offset(f.tipX, f.tipY))
        FeatherArt.draw(this, f.featherX, f.featherY, f.featherAngle, world.dp, time, f.flutter, catContrast)
    }

    private fun DrawScope.drawExitRing(engine: GameEngine) {
        val p = engine.exitDetector.cornerProgress
        if (p <= 0f) return
        val u = engine.world.dp
        val r = 28f * u
        val c = Offset(size.width - 40f * u, 40f * u)
        drawCircle(Color.White.copy(alpha = 0.15f), radius = r, center = c, style = ringStroke)
        drawArc(
            Color(0xFFF4C95D), -90f, 360f * p, false,
            topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2), style = ringStroke,
        )
    }
}
