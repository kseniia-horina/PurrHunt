package app.purrhunt.ui.achievements

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.purrhunt.R
import app.purrhunt.data.SettingsRepository
import app.purrhunt.data.StatsRepository
import app.purrhunt.domain.Achievement
import app.purrhunt.domain.CatStats
import app.purrhunt.ui.components.PurrScaffold
import app.purrhunt.ui.components.StarShape
import app.purrhunt.ui.components.descRes
import app.purrhunt.ui.components.titleRes
import app.purrhunt.ui.theme.PurrColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AchievementItem(val achievement: Achievement, val fraction: Float, val progress: Long, val unlocked: Boolean)

@HiltViewModel
class AchievementsViewModel @Inject constructor(settings: SettingsRepository, stats: StatsRepository) : ViewModel() {
    val items: StateFlow<List<AchievementItem>> = stats.observeStatsOrEmpty(settings.appState.map { it.currentCatId })
        .map { s -> itemsFor(s) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), itemsFor(CatStats()))

    private fun itemsFor(s: CatStats) = Achievement.entries
        .map { AchievementItem(it, it.fraction(s), it.progress(s).coerceAtMost(it.target), it.isUnlocked(s)) }
        .sortedByDescending { it.unlocked }
}

@Composable
fun AchievementsScreen(onBack: () -> Unit, viewModel: AchievementsViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    PurrScaffold(title = stringResource(R.string.achievements_title), onBack = onBack) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.achievement.name }) { AchievementBadge(it) }
        }
    }
}

@Composable
private fun AchievementBadge(item: AchievementItem) {
    val title = stringResource(item.achievement.titleRes)
    val status = if (item.unlocked) stringResource(R.string.achievement_unlocked)
    else stringResource(R.string.achievement_progress, displayValue(item.achievement, item.progress), displayValue(item.achievement, item.achievement.target))
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "$title. $status" },
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Medal(item.unlocked, item.fraction)
                StarShape(Modifier.size(30.dp), color = if (item.unlocked) Color(0xFF3A2A00) else PurrColors.Muted.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(
                stringResource(item.achievement.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            if (item.unlocked) {
                Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            } else {
                LinearProgressIndicator(progress = { item.fraction }, modifier = Modifier.fillMaxWidth())
                Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Medal(unlocked: Boolean, fraction: Float) {
    Canvas(Modifier.size(72.dp)) {
        val r = size.minDimension / 2
        if (unlocked) {
            drawCircle(
                Brush.radialGradient(listOf(PurrColors.Amber, PurrColors.AmberDeep), center = Offset(r * 0.7f, r * 0.7f), radius = r * 1.3f),
                radius = r,
            )
            drawCircle(Color.White.copy(alpha = 0.25f), radius = r * 0.8f, style = Stroke(r * 0.06f))
        } else {
            drawCircle(PurrColors.NightSurfaceHigh, radius = r)
            drawArc(PurrColors.Sky, -90f, 360f * fraction, false, style = Stroke(r * 0.12f))
        }
    }
}

/** Play time is stored in milliseconds but shown in hours. */
private fun displayValue(a: Achievement, v: Long): Int =
    if (a == Achievement.MARATHON) (v / 3_600_000L).toInt() else v.toInt()
