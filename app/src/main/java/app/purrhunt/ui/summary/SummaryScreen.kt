package app.purrhunt.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.purrhunt.R
import app.purrhunt.data.SettingsRepository
import app.purrhunt.data.StatsRepository
import app.purrhunt.domain.GameMode
import app.purrhunt.domain.Location
import app.purrhunt.domain.SessionSummary
import app.purrhunt.ui.components.PawPrint
import app.purrhunt.ui.components.PurrScaffold
import app.purrhunt.ui.components.SectionTitle
import app.purrhunt.ui.components.SpeciesIcon
import app.purrhunt.ui.components.StarShape
import app.purrhunt.ui.components.StatTile
import app.purrhunt.ui.components.nameRes
import app.purrhunt.ui.components.titleRes
import app.purrhunt.ui.theme.PurrColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class SummaryUiState(val summary: SessionSummary? = null, val treatReminder: Boolean = true)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    stats: StatsRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SummaryUiState())
    val state: StateFlow<SummaryUiState> = _state

    init {
        val id: Long = savedState["sessionId"] ?: 0L
        viewModelScope.launch {
            val s = stats.summaryOf(id)
            val treat = settings.current().treatReminder
            _state.update { SummaryUiState(s, treat) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryScreen(
    onPlayAgain: (Location, Boolean, GameMode) -> Unit,
    onMenu: () -> Unit,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PurrScaffold(title = stringResource(R.string.summary_title), onBack = onMenu) { padding ->
        val s = state.summary ?: return@PurrScaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(stringResource(R.string.summary_caught), s.catches.toString(), Modifier.weight(1f).testTag("summary_caught"))
                    StatTile(stringResource(R.string.summary_misses), s.misses.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(stringResource(R.string.summary_accuracy), stringResource(R.string.percent_format, (s.accuracy * 100).roundToInt()), Modifier.weight(1f))
                    val minutes = (s.durationMs / 60_000).toInt()
                    val seconds = ((s.durationMs / 1000) % 60).toInt()
                    StatTile(stringResource(R.string.summary_duration), stringResource(R.string.duration_format, minutes, seconds), Modifier.weight(1f))
                }

                if (s.stars > 0) {
                    SectionTitle(stringResource(R.string.summary_stars))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(s.stars.coerceAtMost(10)) { StarShape(Modifier.size(32.dp).padding(2.dp)) }
                        if (s.stars > 10) Text("+${s.stars - 10}", style = MaterialTheme.typography.titleMedium)
                    }
                }

                if (s.rare.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.summary_rare))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        s.rare.forEach { sp -> SpeciesBadge(sp, s.perSpecies[sp] ?: 1, highlight = true) }
                    }
                }

                if (s.perSpecies.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.summary_caught))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        s.perSpecies.entries.sortedByDescending { it.value }.forEach { (sp, n) -> SpeciesBadge(sp, n, highlight = false) }
                    }
                } else if (s.catches == 0) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.summary_nothing), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (s.newAchievements.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.summary_new_achievements))
                    s.newAchievements.forEach {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            StarShape(Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(it.titleRes), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                if (state.treatReminder) {
                    Spacer(Modifier.height(20.dp))
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().testTag("summary_treat")) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            PawPrint(PurrColors.Amber, Modifier.size(36.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.summary_treat), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onPlayAgain(s.location, s.night, s.mode) },
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("summary_again"),
                ) { Text(stringResource(R.string.summary_play_again)) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onMenu, modifier = Modifier.fillMaxWidth().testTag("summary_menu")) {
                    Text(stringResource(R.string.summary_to_menu))
                }
            }
        }
    }
}

@Composable
private fun SpeciesBadge(species: app.purrhunt.domain.Species, count: Int, highlight: Boolean) {
    val name = stringResource(species.nameRes)
    Surface(
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$name: $count" },
    ) {
        Column(Modifier.padding(8.dp).width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp)) { SpeciesIcon(species, Modifier.size(64.dp)) }
            Text("×$count", style = MaterialTheme.typography.titleMedium)
            Text(name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}
