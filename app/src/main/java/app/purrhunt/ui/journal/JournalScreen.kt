package app.purrhunt.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import app.purrhunt.data.BestiaryEntry
import app.purrhunt.data.DailyPoint
import app.purrhunt.data.Journal
import app.purrhunt.data.SettingsRepository
import app.purrhunt.data.StatsRepository
import app.purrhunt.domain.FavoritePrey
import app.purrhunt.domain.Location
import app.purrhunt.ui.components.PurrScaffold
import app.purrhunt.ui.components.SpeciesIcon
import app.purrhunt.ui.components.factRes
import app.purrhunt.ui.components.locationNameRes
import app.purrhunt.ui.components.nameRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

sealed interface JournalUiState {
    data object Loading : JournalUiState
    data object NoCat : JournalUiState
    data class Ready(val journal: Journal) : JournalUiState
}

@HiltViewModel
class JournalViewModel @Inject constructor(settings: SettingsRepository, stats: StatsRepository) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<JournalUiState> = settings.appState.map { it.currentCatId }
        .flatMapLatest { id -> if (id == null) flowOf(JournalUiState.NoCat) else stats.observeJournal(id).map { JournalUiState.Ready(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState.Loading)
}

@Composable
fun JournalScreen(onBack: () -> Unit, onPlayLocation: (Location, Boolean) -> Unit, viewModel: JournalViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(R.string.journal_tab_bestiary, R.string.journal_tab_charts, R.string.journal_tab_heatmap, R.string.journal_tab_favorite)
    PurrScaffold(title = stringResource(R.string.journal_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryScrollableTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background, edgePadding = 8.dp) {
                tabs.forEachIndexed { i, res ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = { Text(stringResource(res)) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("journal_tab_$i"))
                }
            }
            when (val s = state) {
                JournalUiState.Loading -> Unit
                JournalUiState.NoCat -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.journal_no_cat), textAlign = TextAlign.Center)
                }
                is JournalUiState.Ready -> when (tab) {
                    0 -> Bestiary(s.journal.bestiary)
                    1 -> ChartsTab(s.journal.daily)
                    2 -> HeatmapTab(s.journal)
                    else -> FavoriteTab(s.journal, onPlayLocation)
                }
            }
        }
    }
}

@Composable
private fun Bestiary(entries: List<BestiaryEntry>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(entries, key = { it.species.name }) { e ->
            val known = e.caught > 0
            val name = stringResource(e.species.nameRes)
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(12.dp).semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
                    SpeciesIcon(e.species, Modifier.size(96.dp), silhouette = !known, description = name)
                    Text(if (known) name else "???", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                    if (e.species.rare) Text(stringResource(R.string.bestiary_rare), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    if (known) {
                        Text(stringResource(e.species.factRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.bestiary_caught, e.caught), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(R.string.bestiary_best, e.bestSession), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(stringResource(R.string.bestiary_unknown), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private data class Series(val labels: List<String>, val catches: List<Float>, val minutes: List<Float>)

private fun series(daily: List<DailyPoint>, weeks: Boolean, today: LocalDate = LocalDate.now()): Series {
    val byDay = daily.associateBy { it.epochDay }
    return if (!weeks) {
        val days = (13 downTo 0).map { today.minusDays(it.toLong()) }
        Series(
            labels = days.map { it.dayOfMonth.toString() },
            catches = days.map { (byDay[it.toEpochDay()]?.catches ?: 0).toFloat() },
            minutes = days.map { (byDay[it.toEpochDay()]?.playMs ?: 0L) / 60_000f },
        )
    } else {
        val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val starts = (11 downTo 0).map { thisWeek.minusWeeks(it.toLong()) }
        Series(
            labels = starts.map { "%d.%02d".format(it.dayOfMonth, it.monthValue) },
            catches = starts.map { s -> (0..6).sumOf { byDay[s.plusDays(it.toLong()).toEpochDay()]?.catches ?: 0 }.toFloat() },
            minutes = starts.map { s -> (0..6).sumOf { byDay[s.plusDays(it.toLong()).toEpochDay()]?.playMs ?: 0L } / 60_000f },
        )
    }
}

@Composable
private fun ChartsTab(daily: List<DailyPoint>) {
    var weeks by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !weeks, onClick = { weeks = false }, label = { Text(stringResource(R.string.chart_by_day)) })
                FilterChip(selected = weeks, onClick = { weeks = true }, label = { Text(stringResource(R.string.chart_by_week)) })
            }
            if (daily.isEmpty()) {
                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.chart_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            val s = series(daily, weeks)
            val catchesTitle = stringResource(R.string.chart_catches)
            val minutesTitle = stringResource(R.string.chart_playtime)
            Spacer(Modifier.height(16.dp))
            Text(catchesTitle, style = MaterialTheme.typography.titleMedium)
            BarChart(
                values = s.catches,
                labels = s.labels,
                format = { it.toInt().toString() },
                description = describe(catchesTitle, s.labels, s.catches) { it.toInt().toString() },
            )
            Spacer(Modifier.height(24.dp))
            Text(minutesTitle, style = MaterialTheme.typography.titleMedium)
            BarChart(
                values = s.minutes,
                labels = s.labels,
                format = { if (it < 10f) "%.1f".format(it) else it.toInt().toString() },
                description = describe(minutesTitle, s.labels, s.minutes) { "%.1f".format(it) },
            )
        }
    }
}

/** Screen readers get the chart as a list of values: the table view of the chart. */
private fun describe(title: String, labels: List<String>, values: List<Float>, fmt: (Float) -> String): String =
    title + ". " + labels.indices.joinToString("; ") { "${labels[it]}: ${fmt(values[it])}" }

@Composable
private fun HeatmapTab(journal: Journal) {
    var all by rememberSaveable { mutableStateOf(false) }
    val cells = if (all) journal.allHeatmap else journal.lastHeatmap
    val caption = stringResource(R.string.heatmap_caption)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !all, onClick = { all = false }, label = { Text(stringResource(R.string.heatmap_last)) })
            FilterChip(selected = all, onClick = { all = true }, label = { Text(stringResource(R.string.heatmap_all)) })
        }
        Spacer(Modifier.height(8.dp))
        Text(caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if (cells.all { it == 0 }) {
            Text(stringResource(R.string.chart_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        Heatmap(cells, caption, Modifier.widthIn(max = 320.dp).fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        HeatLegend(cells.max(), Modifier.widthIn(max = 320.dp))
    }
}

@Composable
private fun FavoriteTab(journal: Journal, onPlay: (Location, Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.favorite_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        val fav = journal.favorite
        if (fav == null) {
            Text(stringResource(R.string.favorite_none), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        val name = stringResource(fav.nameRes)
        SpeciesIcon(fav, Modifier.size(180.dp), description = name)
        Text(name, style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.favorite_rate, journal.favoriteRate), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        val location = FavoritePrey.suggestedLocation(fav)
        val night = fav.nightOnly
        Text(stringResource(R.string.favorite_hint, stringResource(locationNameRes(location, night))), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onPlay(location, night) }, modifier = Modifier.widthIn(min = 200.dp).testTag("favorite_play")) {
            Text(stringResource(R.string.favorite_play))
        }
    }
}
