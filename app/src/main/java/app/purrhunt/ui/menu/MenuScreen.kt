package app.purrhunt.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.purrhunt.R
import app.purrhunt.data.CatRepository
import app.purrhunt.data.SettingsRepository
import app.purrhunt.data.StatsRepository
import app.purrhunt.domain.CatProfile
import app.purrhunt.domain.CatStats
import app.purrhunt.domain.Rank
import app.purrhunt.domain.Stars
import app.purrhunt.ui.components.CatAvatar
import app.purrhunt.ui.components.PawPrint
import app.purrhunt.ui.components.StarRow
import app.purrhunt.ui.components.nameRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MenuUiState(
    val loaded: Boolean = false,
    val cat: CatProfile? = null,
    val hasCats: Boolean = false,
    val stats: CatStats = CatStats(),
) {
    val rank: Rank get() = Rank.forCatches(stats.totalCatches)
    val stars: Int get() = Stars.total(stats.totalCatches, stats.rareCatches)
}

@HiltViewModel
class MenuViewModel @Inject constructor(
    settings: SettingsRepository,
    cats: CatRepository,
    stats: StatsRepository,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<MenuUiState> = combine(settings.appState.map { it.currentCatId }, cats.cats) { id, list ->
        (list.firstOrNull { it.id == id } ?: list.firstOrNull()) to list.isNotEmpty()
    }.flatMapLatest { (cat, hasCats) ->
        if (cat == null) flowOf(MenuUiState(loaded = true, cat = null, hasCats = hasCats))
        else stats.observeStats(cat.id).map { MenuUiState(loaded = true, cat = cat, hasCats = hasCats, stats = it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState())
}

@Composable
fun MenuScreen(
    onPlay: () -> Unit,
    onModes: () -> Unit,
    onJournal: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit,
    onCats: () -> Unit,
    onAddCat: () -> Unit,
    onAlbum: () -> Unit,
    onZoo: (() -> Unit)?,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
            PawPrint(MaterialTheme.colorScheme.primary, Modifier.size(36.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        }
        ProfileCard(state, onCats = onCats, onAddCat = onAddCat)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(64.dp).testTag("menu_play")) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.menu_play), style = MaterialTheme.typography.titleLarge)
            }
            MenuButton(Icons.Filled.Favorite, R.string.menu_modes, onModes, "menu_modes")
            MenuButton(Icons.Filled.List, R.string.menu_journal, onJournal, "menu_journal")
            MenuButton(Icons.Filled.Star, R.string.menu_achievements, onAchievements, "menu_achievements")
            MenuButton(Icons.Filled.ThumbUp, R.string.menu_album, onAlbum, "menu_album")
            MenuButton(Icons.Filled.Settings, R.string.menu_settings, onSettings, "menu_settings")
            if (onZoo != null) {
                OutlinedButton(onClick = onZoo, modifier = Modifier.fillMaxWidth().testTag("menu_zoo")) {
                    Icon(Icons.Filled.Build, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.menu_zoo))
                }
            }
        }
    }
}

@Composable
private fun MenuButton(icon: ImageVector, label: Int, onClick: () -> Unit, tag: String) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp).testTag(tag)) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(label))
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ProfileCard(state: MenuUiState, onCats: () -> Unit, onAddCat: () -> Unit) {
    val cat = state.cat
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .clickable(role = Role.Button) { if (state.hasCats) onCats() else onAddCat() }
            .testTag("menu_profile"),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CatAvatar(cat, 64.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                if (!state.loaded) {
                    Spacer(Modifier.height(48.dp))
                } else if (cat == null) {
                    Text(stringResource(R.string.menu_no_cat), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.menu_add_cat), color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(cat.name, style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(state.rank.nameRes), color = MaterialTheme.colorScheme.secondary)
                    val next = Rank.next(state.stats.totalCatches)
                    Text(
                        if (next != null) stringResource(R.string.menu_rank_next, next.second, stringResource(next.first.nameRes))
                        else stringResource(R.string.menu_rank_top),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (cat != null) StarRow(state.stars)
            else if (state.loaded) Icon(Icons.Filled.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
