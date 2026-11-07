package app.purrhunt.zoo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.purrhunt.R
import app.purrhunt.domain.Species
import app.purrhunt.game.creatures.CreatureRenderers
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.render.RenderKit.softShadow
import app.purrhunt.ui.components.PurrScaffold
import app.purrhunt.ui.components.nameRes

/** Debug-only: every creature drawn large, cycling through all of its animations. */
@Composable
fun ZooScreen(onBack: () -> Unit) {
    var species by remember { mutableStateOf(Species.GRAY_MOUSE) }
    val puppet = remember { ZooPuppet() }
    val frame = remember { mutableLongStateOf(0L) }
    val density = LocalDensity.current.density
    var label by remember { mutableStateOf("") }

    LaunchedEffect(species) {
        puppet.select(species, density)
        var last = -1L
        while (true) {
            withFrameNanos { t ->
                val dt = if (last < 0) 0f else ((t - last) / 1e9f).coerceAtMost(0.05f)
                last = t
                puppet.update(dt)
                frame.longValue = t
                label = puppet.label
            }
        }
    }

    PurrScaffold(title = stringResource(R.string.zoo_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp)) {
                items(Species.entries) { s ->
                    FilterChip(
                        selected = s == species,
                        onClick = { species = s },
                        label = { Text(stringResource(s.nameRes)) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
            Text(
                stringResource(R.string.zoo_state, label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(12.dp),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Brush.radialGradient(listOf(Color(0xFF4A5244), Color(0xFF23281F)))),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    frame.longValue
                    puppet.layout(size.width, size.height)
                    val c: Creature = puppet.creature
                    val r = CreatureRenderers.of(c.species.family)
                    softShadow(c.x, c.y, c.length * r.shadowRx, c.length * r.shadowRy, c.heading, c.altitude)
                    with(r) {
                        drawWorldParts(c)
                        draw(c, puppet.time)
                    }
                }
            }
        }
    }
}
