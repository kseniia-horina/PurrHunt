package app.purrhunt.game.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import app.purrhunt.game.render.RenderKit.contrastHalo
import kotlin.math.sin

/** A striped pheasant-like feather on the end of the toy string. */
object FeatherArt {
    /** Vane in unit space: quill at the origin, the feather points along +x and is 1 unit long. */
    private val vane = Path().apply {
        moveTo(0.02f, 0f)
        cubicTo(0.2f, -0.16f, 0.6f, -0.2f, 1f, -0.02f)
        lineTo(1f, 0.02f)
        cubicTo(0.6f, 0.18f, 0.25f, 0.15f, 0.02f, 0f)
        close()
    }
    private val barbs = Path().apply {
        var t = 0.1f
        while (t < 0.98f) {
            moveTo(t, 0f)
            lineTo(t + 0.07f, -0.17f * (1f - (t - 0.5f) * (t - 0.5f) * 2.4f))
            moveTo(t, 0f)
            lineTo(t + 0.07f, 0.15f * (1f - (t - 0.5f) * (t - 0.5f) * 2.4f))
            t += 0.025f
        }
    }
    private val stripes = Path().apply {
        var t = 0.18f
        while (t < 0.95f) {
            moveTo(t, -0.25f)
            lineTo(t + 0.03f, 0.25f)
            t += 0.11f
        }
    }
    private val body = Brush.linearGradient(
        listOf(Color(0xFFF1E3C8), Color(0xFFC79A5C), Color(0xFF8A5A2E)),
        start = Offset(0f, -0.2f),
        end = Offset(1f, 0.2f),
    )
    private val barbStroke = Stroke(0.006f, cap = StrokeCap.Round)
    private val stripeStroke = Stroke(0.035f)
    private val quillStroke = Stroke(0.014f, cap = StrokeCap.Round)

    fun draw(scope: DrawScope, x: Float, y: Float, angle: Float, dp: Float, time: Float, flutter: Float, contrast: Boolean) = with(scope) {
        val length = 84f * dp
        if (contrast) contrastHalo(x, y, length * 0.9f, 0.6f)
        val wobble = sin(time * 9f) * 4f + flutter * sin(time * 40f) * 18f
        withTransform({
            translate(x, y)
            rotate(angle * 57.2958f + wobble, Offset.Zero)
            scale(length, length, Offset.Zero)
        }) {
            drawPath(vane, body)
            clipPath(vane) {
                drawPath(stripes, Color(0xFF3B2614), alpha = 0.55f, style = stripeStroke)
                drawPath(barbs, Color.White, alpha = 0.25f, style = barbStroke)
            }
            drawLine(Color(0xFFF7EEDB), Offset(-0.05f, 0f), Offset(0.98f, 0f), strokeWidth = quillStroke.width, cap = StrokeCap.Round)
        }
    }
}
