package app.purrhunt.ui.cats

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.purrhunt.R
import app.purrhunt.data.CatRepository
import app.purrhunt.domain.CatProfile
import app.purrhunt.ui.components.CatAvatar
import app.purrhunt.ui.components.PurrScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class CatEditUiState(
    val profile: CatProfile = CatProfile(name = "", colorArgb = CatProfile.COLORS.first()),
    val nameError: Boolean = false,
    val loaded: Boolean = false,
)

@HiltViewModel
class CatEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val cats: CatRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val id: Long = savedState["id"] ?: 0L
    private val _state = MutableStateFlow(CatEditUiState(loaded = id == 0L))
    val state: StateFlow<CatEditUiState> = _state

    init {
        if (id != 0L) viewModelScope.launch {
            cats.get(id)?.let { p -> _state.update { it.copy(profile = p, loaded = true) } }
        }
    }

    fun setName(name: String) = _state.update { it.copy(profile = it.profile.copy(name = name.take(30)), nameError = false) }
    fun setColor(argb: Int) = _state.update { it.copy(profile = it.profile.copy(colorArgb = argb)) }
    fun removePhoto() = _state.update { it.copy(profile = it.profile.copy(photoPath = null)) }

    /** Copies the picked image into private storage: Photo Picker grants are temporary. */
    fun setPhoto(uri: Uri) = viewModelScope.launch {
        val path = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "avatars").apply { mkdirs() }
                val file = File(dir, "cat_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                file.absolutePath
            }.getOrNull()
        }
        if (path != null) _state.update { it.copy(profile = it.profile.copy(photoPath = path)) }
    }

    fun save(done: () -> Unit) {
        val p = _state.value.profile
        if (p.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        viewModelScope.launch {
            cats.save(p)
            done()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatEditScreen(onBack: () -> Unit, viewModel: CatEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::setPhoto)
    }
    val title = stringResource(if (state.profile.id == 0L) R.string.cat_new else R.string.cat_edit)
    PurrScaffold(title = title, onBack = onBack) { padding ->
        if (!state.loaded) return@PurrScaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CatAvatar(state.profile, 120.dp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.cat_photo_pick)) }
                if (state.profile.photoPath != null) {
                    TextButton(onClick = viewModel::removePhoto) { Text(stringResource(R.string.cat_photo_remove)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.profile.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.cat_name)) },
                isError = state.nameError,
                supportingText = { if (state.nameError) Text(stringResource(R.string.cat_name_required)) },
                singleLine = true,
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().testTag("cat_name"),
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.cat_color), style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            ) {
                CatProfile.COLORS.forEachIndexed { i, argb ->
                    val selected = argb == state.profile.colorArgb
                    val description = stringResource(R.string.cd_color, i + 1)
                    Box(
                        Modifier
                            .size(48.dp)
                            .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                            .padding(5.dp)
                            .background(Color(argb), CircleShape)
                            .clickable(role = Role.RadioButton) { viewModel.setColor(argb) }
                            .semantics {
                                contentDescription = description
                                this.selected = selected
                            },
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { viewModel.save(onBack) },
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().height(52.dp).testTag("cat_save"),
            ) { Text(stringResource(R.string.common_save)) }
        }
    }
}
