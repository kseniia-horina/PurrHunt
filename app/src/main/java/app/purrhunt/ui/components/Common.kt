package app.purrhunt.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.purrhunt.R
import app.purrhunt.domain.CatProfile
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurrScaffold(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

/** Round cat avatar: the profile photo, or a paw print in the cat's coat colour. */
@Composable
fun CatAvatar(cat: CatProfile?, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(cat?.photoPath) {
        cat?.photoPath?.let { path ->
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(File(path).absolutePath, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }
    val color = cat?.let { Color(it.colorArgb) } ?: MaterialTheme.colorScheme.surfaceVariant
    val description = cat?.let { stringResource(R.string.cd_cat_avatar, it.name) } ?: stringResource(R.string.cd_paw_logo)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(2.dp, color, CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            PawPrint(color = color, modifier = Modifier.size(size * 0.6f))
        }
    }
}

@Composable
fun PawPrint(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawPaw(color, Offset(size.width / 2, size.height / 2), size.minDimension) }
}

/** Paw print centred at [center], [extent] wide. Used for avatars, the logo and touch traces. */
fun DrawScope.drawPaw(color: Color, center: Offset, extent: Float, alpha: Float = 1f) {
    val u = extent / 44f
    drawOval(
        color, topLeft = Offset(center.x - 11f * u, center.y - 2f * u),
        size = androidx.compose.ui.geometry.Size(22f * u, 18f * u), alpha = alpha,
    )
    val toes = PAW_TOES
    for (i in toes.indices step 2) {
        drawOval(
            color,
            topLeft = Offset(center.x + toes[i] * u - 4.5f * u, center.y + toes[i + 1] * u - 5.5f * u),
            size = androidx.compose.ui.geometry.Size(9f * u, 11f * u),
            alpha = alpha,
        )
    }
}

private val PAW_TOES = floatArrayOf(-16f, -6f, -6f, -14f, 6f, -14f, 16f, -6f)

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StarRow(stars: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StarShape(Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(stars.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun StarShape(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) {
        val path = Path()
        val cx = size.width / 2
        val cy = size.height / 2
        val outer = size.minDimension / 2
        val inner = outer * 0.45f
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = Math.toRadians((-90 + i * 36).toDouble())
            val x = cx + r * kotlin.math.cos(a).toFloat()
            val y = cy + r * kotlin.math.sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }
}

fun Modifier.screenPadding(): Modifier = this.fillMaxWidth().padding(horizontal = 16.dp)
