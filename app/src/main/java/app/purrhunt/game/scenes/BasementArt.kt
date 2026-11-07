package app.purrhunt.game.scenes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import app.purrhunt.game.engine.World
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Basement: brick wall with burrows in the skirting, plank floor, crates and a sack. Dim and muted. */
object BasementArt : SceneArt {
    private val mortar = Color(0xFF26201C)
    private val brickA = Color(0xFF553629)
    private val brickB = Color(0xFF6C4636)
    private val plankA = Color(0xFF43332A)
    private val plankB = Color(0xFF594536)
    private val seam = Color(0xFF1E1610)
    private val skirting = Color(0xFF35271E)
    private val crateWood = Color(0xFF7A5B3E)
    private val crateDark = Color(0xFF4A3524)
    private val burlap = Color(0xFF7F6B4A)

    override fun DrawScope.background(layout: SceneLayout, look: SceneLook) {
        val u = layout.dp
        val w = size.width
        val h = size.height
        val top = layout.floorTop
        val rnd = Random(7)

        // Floor planks running away from the wall.
        val plankW = 62f * u
        var x = -rnd.nextFloat() * plankW
        while (x < w) {
            val c = lerp(plankA, plankB, rnd.nextFloat())
            drawRect(c, Offset(x, top), Size(plankW, h - top))
            // Grain: long wavy strokes along the board.
            val grain = Path()
            repeat(12) {
                var gx = x + rnd.nextFloat() * plankW
                var gy = top
                grain.moveTo(gx, gy)
                while (gy < h) {
                    val ny = gy + (60f + rnd.nextFloat() * 80f) * u
                    val nx = (gx + (rnd.nextFloat() - 0.5f) * 6f * u).coerceIn(x + 2f, x + plankW - 2f)
                    grain.quadraticTo(gx + (rnd.nextFloat() - 0.5f) * 8f * u, (gy + ny) / 2, nx, ny)
                    gx = nx
                    gy = ny
                }
            }
            drawPath(grain, Color.Black.copy(alpha = 0.22f), style = Stroke(1.1f * u))
            // A knot here and there.
            if (rnd.nextFloat() < 0.6f) {
                val kx = x + plankW * (0.25f + rnd.nextFloat() * 0.5f)
                val ky = top + (h - top) * rnd.nextFloat()
                for (ring in 0 until 4) {
                    val rx = (5f + ring * 4f) * u
                    drawOval(Color.Black.copy(alpha = 0.18f), Offset(kx - rx / 2, ky - rx), Size(rx, rx * 2), style = Stroke(1.2f * u))
                }
                drawOval(Color(0xFF2A1D14), Offset(kx - 2.5f * u, ky - 5f * u), Size(5f * u, 10f * u))
            }
            // Board joint and nails.
            val jy = top + (h - top) * rnd.nextFloat()
            drawLine(seam, Offset(x, jy), Offset(x + plankW, jy), strokeWidth = 2f * u)
            drawCircle(Color(0xFF15100C), 2f * u, Offset(x + plankW * 0.2f, jy + 6f * u))
            drawCircle(Color(0xFF15100C), 2f * u, Offset(x + plankW * 0.8f, jy + 6f * u))
            drawLine(seam, Offset(x, top), Offset(x, h), strokeWidth = 2.5f * u)
            x += plankW
        }

        // Brick wall.
        drawRect(mortar, Offset.Zero, Size(w, top))
        val bh = 22f * u
        val bw = 56f * u
        var row = 0
        var by = top - bh
        while (by > -bh) {
            var bx = if (row % 2 == 0) 0f else -bw / 2
            while (bx < w) {
                val c = lerp(brickA, brickB, rnd.nextFloat())
                val inset = 1.8f * u
                drawRect(c, Offset(bx + inset, by + inset), Size(bw - inset * 2, bh - inset * 2))
                drawLine(Color.White.copy(alpha = 0.07f), Offset(bx + inset, by + inset), Offset(bx + bw - inset, by + inset), strokeWidth = 1.5f * u)
                drawLine(Color.Black.copy(alpha = 0.25f), Offset(bx + inset, by + bh - inset), Offset(bx + bw - inset, by + bh - inset), strokeWidth = 1.5f * u)
                repeat(5) {
                    drawCircle(
                        Color.Black.copy(alpha = 0.18f),
                        radius = (0.6f + rnd.nextFloat()) * u,
                        center = Offset(bx + inset + rnd.nextFloat() * (bw - inset * 2), by + inset + rnd.nextFloat() * (bh - inset * 2)),
                    )
                }
                bx += bw
            }
            by -= bh
            row++
        }
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent), 0f, top), Offset.Zero, Size(w, top))

        // Skirting board and its shadow on the floor.
        drawRect(skirting, Offset(0f, top - 12f * u), Size(w, 12f * u))
        drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, top - 12f * u), Offset(w, top - 12f * u), strokeWidth = 1.5f * u)
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent), top, top + 18f * u), Offset(0f, top), Size(w, 18f * u))

        // Burrows: dark arches in the wall.
        for (s in layout.burrows) {
            val r = s.radius
            val hole = Brush.radialGradient(listOf(Color(0xFF020101), Color(0xFF120C09)), center = Offset(s.x, top), radius = r * 1.3f)
            drawArc(hole, 180f, 180f, true, Offset(s.x - r, top - r * 1.3f), Size(r * 2, r * 2.6f))
            drawArc(Color(0xFF6A4A3A), 185f, 170f, false, Offset(s.x - r, top - r * 1.3f), Size(r * 2, r * 2.6f), style = Stroke(1.8f * u))
            // Crumbs of plaster at the entrance.
            repeat(6) {
                drawCircle(Color(0xFF6B5A4E), (0.8f + rnd.nextFloat()) * u, Offset(s.x + (rnd.nextFloat() - 0.5f) * r * 2.4f, top + rnd.nextFloat() * 8f * u))
            }
        }

        // Shadows of the props (the props themselves are in the foreground).
        for (s in layout.spots) {
            when (s.kind) {
                SpotKind.CRATE -> drawRect(Color.Black.copy(alpha = 0.35f), Offset(s.x - s.radius + 8f * u, s.y - s.radius + 10f * u), Size(s.radius * 2, s.radius * 2))
                SpotKind.SACK -> drawOval(Color.Black.copy(alpha = 0.35f), Offset(s.x - s.radius + 6f * u, s.y - s.radius * 0.7f + 10f * u), Size(s.radius * 2.1f, s.radius * 1.6f))
                else -> Unit
            }
        }

        // Warm lamp light from the upper left and a vignette.
        drawRect(
            Brush.radialGradient(
                listOf(Color(0x33E8C890), Color.Transparent),
                center = Offset(w * 0.3f, top + (h - top) * 0.25f),
                radius = maxOf(w, h) * 0.6f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                0.5f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.6f),
                center = Offset(w / 2, h / 2),
                radius = maxOf(w, h) * 0.75f,
            ),
        )
    }

    override fun DrawScope.foreground(layout: SceneLayout, look: SceneLook) {
        val u = layout.dp
        val rnd = Random(11)
        for (s in layout.spots) {
            when (s.kind) {
                SpotKind.CRATE -> crate(s, u, (rnd.nextFloat() - 0.5f) * 12f)
                SpotKind.SACK -> sack(s, u, rnd)
                else -> Unit
            }
        }
    }

    private fun DrawScope.crate(s: HidingSpot, u: Float, angle: Float) {
        val side = s.radius * 2f
        val left = s.x - s.radius
        val top = s.y - s.radius
        rotate(angle, Offset(s.x, s.y)) {
            drawRect(crateDark, Offset(left, top), Size(side, side))
            val planks = 4
            val pw = (side - 12f * u) / planks
            for (i in 0 until planks) {
                val c = lerp(crateWood, crateDark, i * 0.12f)
                drawRect(c, Offset(left + 6f * u + i * pw + 1f * u, top + 6f * u), Size(pw - 2f * u, side - 12f * u))
                drawLine(Color.Black.copy(alpha = 0.2f), Offset(left + 6f * u + i * pw + pw * 0.5f, top + 8f * u), Offset(left + 6f * u + i * pw + pw * 0.45f, top + side - 8f * u), strokeWidth = 1f * u)
            }
            // Frame boards.
            drawRect(Color(0xFF5E432C), Offset(left, top), Size(side, 7f * u))
            drawRect(Color(0xFF5E432C), Offset(left, top + side - 7f * u), Size(side, 7f * u))
            for (cx in floatArrayOf(left + 4f * u, left + side - 4f * u)) for (cy in floatArrayOf(top + 3.5f * u, top + side - 3.5f * u)) {
                drawCircle(Color(0xFF1C140E), 1.8f * u, Offset(cx, cy))
            }
            drawRect(
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.3f)), Offset(left, top), Offset(left + side, top + side)),
                Offset(left, top), Size(side, side),
            )
        }
    }

    private fun DrawScope.sack(s: HidingSpot, u: Float, rnd: Random) {
        val p = Path()
        val n = 10
        for (i in 0..n) {
            val a = i * 2f * Math.PI.toFloat() / n
            val r = s.radius * (0.92f + rnd.nextFloat() * 0.16f)
            val px = s.x + cos(a) * r * 1.05f
            val py = s.y + sin(a) * r * 0.78f
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
        }
        p.close()
        drawPath(p, Brush.radialGradient(listOf(Color(0xFF9C875F), burlap, Color(0xFF4F422C)), center = Offset(s.x - s.radius * 0.3f, s.y - s.radius * 0.3f), radius = s.radius * 1.4f))
        clipPath(p) {
            // Woven texture.
            var t = s.x - s.radius * 1.2f
            while (t < s.x + s.radius * 1.2f) {
                drawLine(Color.Black.copy(alpha = 0.12f), Offset(t, s.y - s.radius), Offset(t + s.radius * 0.3f, s.y + s.radius), strokeWidth = 1f * u)
                drawLine(Color.White.copy(alpha = 0.06f), Offset(t, s.y + s.radius), Offset(t + s.radius * 0.3f, s.y - s.radius), strokeWidth = 1f * u)
                t += 4f * u
            }
            drawLine(Color.Black.copy(alpha = 0.25f), Offset(s.x - s.radius * 0.5f, s.y - s.radius * 0.2f), Offset(s.x + s.radius * 0.2f, s.y + s.radius * 0.3f), strokeWidth = 3f * u, cap = StrokeCap.Round)
        }
        // Tied neck pointing to the upper right.
        val nx = s.x + s.radius * 0.75f
        val ny = s.y - s.radius * 0.55f
        drawOval(Color(0xFF5E4E33), Offset(nx - 10f * u, ny - 7f * u), Size(20f * u, 14f * u))
        drawLine(Color(0xFFB59B6A), Offset(nx - 8f * u, ny), Offset(nx + 8f * u, ny + 3f * u), strokeWidth = 2.5f * u, cap = StrokeCap.Round)
    }

    override fun DrawScope.animatedUnder(layout: SceneLayout, look: SceneLook, time: Float, world: World?) {
        // Dust motes drifting through the lamp light.
        val w = size.width
        val h = size.height
        for (i in 0 until 14) {
            val fx = (i * 0.137f + 0.05f) % 1f
            val fy = (i * 0.291f + 0.1f) % 1f
            val px = w * (0.1f + fx * 0.5f) + sin(time * 0.3f + i) * 30f * layout.dp
            val py = layout.floorTop + (h - layout.floorTop) * fy * 0.6f + cos(time * 0.23f + i * 1.7f) * 24f * layout.dp
            val a = 0.08f + 0.06f * sin(time * 0.8f + i * 2.1f)
            drawCircle(Color(0xFFF2E2C0), radius = (1.2f + (i % 3) * 0.5f) * layout.dp, center = Offset(px, py), alpha = a)
        }
    }
}
