package app.purrhunt.ui.locations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.purrhunt.R
import app.purrhunt.data.SettingsRepository
import app.purrhunt.data.StatsRepository
import app.purrhunt.domain.GameSettings
import app.purrhunt.domain.Location
import app.purrhunt.domain.NightClock
import app.purrhunt.domain.NightMode
import app.purrhunt.domain.Stars
import app.purrhunt.ui.components.PurrScaffold
import app.purrhunt.ui.components.StarRow
import app.purrhunt.ui.components.locationDescRes
import app.purrhunt.ui.components.locationNameRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import javax.inject.Inject

data class LocationCard(val location: Location, val night: Boolean, val unlocked: Boolean, val cost: Int)

data class LocationsUiState(val cards: List<LocationCard> = emptyList(), val stars: Int = 0, val settings: GameSettings = GameSettings())

@HiltViewModel
class LocationsViewModel @Inject constructor(settings: SettingsRepository, stats: StatsRepository) : ViewModel() {
    val state: StateFlow<LocationsUiState> = combine(
        settings.settings,
        stats.observeStatsOrEmpty(settings.appState.map { it.currentCatId }),
    ) { s, st ->
        val stars = Stars.total(st.totalCatches, st.rareCatches)
        val nightNow = NightClock.isNight(LocalTime.now().hour)
        val cards = buildList {
            for (loc in Location.entries) {
                val autoNight = loc.supportsNight && s.nightMode == NightMode.AUTO && nightNow
                add(card(loc, autoNight, stars, s.starUnlock))
                if (loc.supportsNight && s.nightMode != NightMode.NEVER && !autoNight) add(card(loc, true, stars, s.starUnlock))
            }
        }
        LocationsUiState(cards, stars, s)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocationsUiState())

    private fun card(loc: Location, night: Boolean, stars: Int, starUnlock: Boolean) = LocationCard(
        location = loc,
        night = night,
        unlocked = Stars.isUnlocked(loc, night, stars, starUnlock),
        cost = loc.starCost + if (night) Location.NIGHT_STAR_COST else 0,
    )
}

@Composable
fun LocationsScreen(
    onBack: () -> Unit,
    onLocation: (Location, Boolean) -> Unit,
    viewModel: LocationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PurrScaffold(
        title = stringResource(R.string.locations_title),
        onBack = onBack,
        actions = { if (state.settings.starUnlock) StarRow(state.stars, Modifier.padding(end = 16.dp)) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.cards, key = { "${it.location}-${it.night}" }) { card ->
                LocationCardView(card, state.settings, onClick = { if (card.unlocked) onLocation(card.location, card.night) })
            }
        }
    }
}

@Composable
private fun LocationCardView(card: LocationCard, settings: GameSettings, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = card.unlocked, role = Role.Button, onClick = onClick)
            .testTag("location_${card.location.name}_${card.night}"),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            ) {
                ScenePreview(card.location, card.night, settings, Modifier.fillMaxSize().alpha(if (card.unlocked) 1f else 0.35f))
                if (card.night) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.location_night_badge)) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    )
                }
                if (!card.unlocked) {
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.location_locked, card.cost), color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text(stringResource(locationNameRes(card.location, card.night)), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(locationDescRes(card.location, card.night)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
