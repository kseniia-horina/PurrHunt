package app.purrhunt.ui.cats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.purrhunt.domain.CatProfile
import app.purrhunt.ui.components.CatAvatar
import app.purrhunt.ui.components.PurrScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CatsUiState(val cats: List<CatProfile> = emptyList(), val currentId: Long? = null)

@HiltViewModel
class CatsViewModel @Inject constructor(
    private val cats: CatRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<CatsUiState> = combine(cats.cats, settings.appState) { list, app ->
        CatsUiState(list, app.currentCatId ?: list.firstOrNull()?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatsUiState())

    fun select(cat: CatProfile) = viewModelScope.launch { settings.setCurrentCat(cat.id) }

    fun delete(cat: CatProfile) = viewModelScope.launch {
        cat.photoPath?.let { File(it).delete() }
        cats.delete(cat)
        if (state.value.currentId == cat.id) {
            settings.setCurrentCat(state.value.cats.firstOrNull { it.id != cat.id }?.id)
        }
    }
}

@Composable
fun CatsScreen(onBack: () -> Unit, onEdit: (Long) -> Unit, viewModel: CatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<CatProfile?>(null) }

    PurrScaffold(title = stringResource(R.string.cats_title), onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.cats, key = { it.id }) { cat ->
                val current = cat.id == state.currentId
                Surface(
                    color = if (current) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { viewModel.select(cat) },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CatAvatar(cat, 56.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cat.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(if (current) R.string.cat_current else R.string.cat_select),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (current) Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.cat_current), tint = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { onEdit(cat.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cat_edit))
                        }
                        IconButton(onClick = { toDelete = cat }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete))
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
            ExtendedFloatingActionButton(
                onClick = { onEdit(0) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.cat_add)) },
                modifier = Modifier.testTag("cats_add"),
            )
        }
    }

    toDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.cat_delete_title, cat.name)) },
            text = { Text(stringResource(R.string.cat_delete_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(cat); toDelete = null }) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}
