package app.purrhunt.ui.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.purrhunt.domain.HeatmapGrid
import app.purrhunt.ui.theme.PurrColors

private val Ink = PurrColors.Cream
private val MutedInk = PurrColors.Muted
private val Grid = Color(0xFF2A3546)
private val BarColor = PurrColors.Sky

/**
 * Single-series bar chart on Canvas: rounded data ends on a shared baseline, a recessive grid with
 * two value labels, sparse x labels, and tap-to-read values.
 */
@Composable
fun BarChart(values: List<Float>, labels: List<String>, format: (Float) -> String, description: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    var selected by remember(values) { mutableIntStateOf(-1) }
    val labelStyle = TextStyle(color = MutedInk, fontSize = 11.sp)
    val valueStyle = TextStyle(color = Ink, fontSize = 13.sp)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = description }
            .pointerInput(values) {
                detectTapGestures { p ->
                    val left = 40.dp.toPx()
                    val slot = (size.width - left) / values.size.coerceAtLeast(1)
                    val i = ((p.x - left) / slot).toInt()
                    selected = if (i in values.indices && i != selected) i else -1
                }
            },
    ) {
        val left = 40.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val top = 22.dp.toPx()
        val max = niceMax(values.maxOrNull() ?: 0f)
        // Recessive grid with value labels at 0, half and max.
        for (k in 0..2) {
            val y = bottom - (bottom - top) * k / 2f
            drawLine(Grid, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val text = format(max * k / 2f)
            val m = measurer.measure(text, labelStyle)
            drawText(m, topLeft = Offset(left - m.size.width - 6.dp.toPx(), y - m.size.height / 2f))
        }
        if (values.isEmpty()) return@Canvas
        val slot = (size.width - left) / values.size
        val gap = 2.dp.toPx()
        val barW = (slot - gap).coerceAtMost(28.dp.toPx())
        val radius = CornerRadius(4.dp.toPx())
        val labelEvery = ((values.size + 6) / 7).coerceAtLeast(1)
        values.forEachIndexed { i, v ->
            val x = left + i * slot + (slot - barW) / 2
            val h = (bottom - top) * v / max
            if (v > 0f) {
                // Rounded top, square foot on the baseline.
                val path = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(x, bottom - h, x + barW, bottom, radius, radius, CornerRadius.Zero, CornerRadius.Zero))
                }
                drawPath(path, if (selected == -1 || selected == i) BarColor else BarColor.copy(alpha = 0.4f))
            }
            if (i % labelEvery == 0 || i == values.lastIndex) {
                val m = measurer.measure(labels[i], labelStyle)
                drawText(m, topLeft = Offset(x + barW / 2 - m.size.width / 2f, bottom + 4.dp.toPx()))
            }
        }
        if (selected in values.indices) {
            val v = values[selected]
            val m = measurer.measure("${labels[selected]}: ${format(v)}", valueStyle)
            val cx = left + selected * slot + slot / 2
            val y = bottom - (bottom - top) * v / max - m.size.height - 6.dp.toPx()
            val x = (cx - m.size.width / 2f).coerceIn(left, size.width - m.size.width)
            drawRoundRect(PurrColors.NightSurfaceHigh, Offset(x - 6.dp.toPx(), y - 2.dp.toPx()), Size(m.size.width + 12.dp.toPx(), m.size.height + 4.dp.toPx()), CornerRadius(6.dp.toPx()))
            drawText(m, topLeft = Offset(x, y.coerceAtLeast(0f)))
        }
    }
}

/** Rounds the axis maximum up to 1, 2 or 5 × 10^n so the half-way gridline is a readable number. */
fun niceMax(v: Float): Float {
    if (v <= 2f) return 2f
    var base = 1f
    while (base * 10f < v) base *= 10f
    for (k in floatArrayOf(1f, 2f, 5f, 10f)) if (base * k >= v) return base * k
    return base * 10f
}

/** Sequential single-hue ramp: low values sit close to the surface, high values are bright. */
private val heatLow = Color(0xFF1B2A40)
private val heatHigh = Color(0xFFD6E8FF)

fun heatColor(t: Float): Color = lerp(heatLow, heatHigh, t.coerceIn(0f, 1f))

/** Touch heatmap drawn over a phone-shaped frame. */
@Composable
fun Heatmap(cells: IntArray, description: String, modifier: Modifier = Modifier) {
    val max = (cells.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(
        modifier
            .aspectRatio(HeatmapGrid.COLS / HeatmapGrid.ROWS.toFloat())
            .semantics { contentDescription = description },
    ) {
        val cw = size.width / HeatmapGrid.COLS
        val ch = size.height / HeatmapGrid.ROWS
        val gap = 2.dp.toPx()
        for (r in 0 until HeatmapGrid.ROWS) for (c in 0 until HeatmapGrid.COLS) {
            val v = cells[r * HeatmapGrid.COLS + c]
            drawRoundRect(
                heatColor(v.toFloat() / max),
                Offset(c * cw + gap / 2, r * ch + gap / 2),
                Size(cw - gap, ch - gap),
                CornerRadius(3.dp.toPx()),
            )
        }
        drawRoundRect(MutedInk, Offset.Zero, size, CornerRadius(16.dp.toPx()), style = Stroke(2.dp.toPx()))
    }
}

@Composable
fun HeatLegend(max: Int, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(color = MutedInk, fontSize = 11.sp)
    Canvas(modifier.fillMaxWidth().height(32.dp)) {
        val barH = 10.dp.toPx()
        drawRoundRect(Brush.horizontalGradient(listOf(heatLow, heatHigh)), Offset.Zero, Size(size.width, barH), CornerRadius(5.dp.toPx()))
        val zero = measurer.measure("0", style)
        drawText(zero, topLeft = Offset(0f, barH + 4.dp.toPx()))
        val top = measurer.measure(max.toString(), style)
        drawText(top, topLeft = Offset(size.width - top.size.width, barH + 4.dp.toPx()))
    }
}
