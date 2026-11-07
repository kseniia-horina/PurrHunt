package app.purrhunt.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.purrhunt.R
import app.purrhunt.data.SettingsRepository
import app.purrhunt.data.StatsRepository
import app.purrhunt.data.reminders.ReminderScheduler
import app.purrhunt.domain.AppLanguage
import app.purrhunt.domain.GameSettings
import app.purrhunt.domain.NightMode
import app.purrhunt.ui.components.PurrScaffold
import app.purrhunt.ui.components.SectionTitle
import app.purrhunt.ui.components.dayRes
import app.purrhunt.ui.components.nameRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val stats: StatsRepository,
    private val reminders: ReminderScheduler,
) : ViewModel() {
    val settings: StateFlow<GameSettings> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, GameSettings())

    fun update(transform: (GameSettings) -> GameSettings) = viewModelScope.launch {
        val before = repo.current()
        repo.update(transform)
        val after = repo.current()
        if (before.remindersEnabled != after.remindersEnabled || before.reminderDays != after.reminderDays ||
            before.reminderHour != after.reminderHour || before.reminderMinute != after.reminderMinute
        ) {
            reminders.schedule(after)
        }
        if (before.language != after.language) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(after.language.tag))
        }
    }

    fun resetStats(done: () -> Unit) = viewModelScope.launch {
        stats.resetAll()
        done()
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deniedText = stringResource(R.string.permission_denied)
    val resetDoneText = stringResource(R.string.settings_reset_done)
    var showReset by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.update { it.copy(remindersEnabled = true) }
        else scope.launch { snackbar.showSnackbar(deniedText) }
    }
    var pendingCamera by remember { mutableStateOf<((GameSettings) -> GameSettings)?>(null) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val change = pendingCamera
        pendingCamera = null
        if (granted && change != null) viewModel.update(change)
        else if (!granted) scope.launch { snackbar.showSnackbar(deniedText) }
    }

    fun withCamera(change: (GameSettings) -> GameSettings) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            viewModel.update(change)
        } else {
            pendingCamera = change
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    PurrScaffold(title = stringResource(R.string.settings_title), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_section_game))
                StepSlider(
                    label = stringResource(R.string.settings_speed),
                    value = s.speedLevel,
                    range = 1..5,
                    valueText = stringResource(R.string.settings_speed_value, s.speedLevel),
                    tag = "settings_speed",
                ) { v -> viewModel.update { it.copy(speedLevel = v) } }
                StepSlider(
                    label = stringResource(R.string.settings_count),
                    value = s.creatureCount,
                    range = 1..5,
                    valueText = s.creatureCount.toString(),
                    tag = "settings_count",
                ) { v -> viewModel.update { it.copy(creatureCount = v) } }
                SwitchRow(stringResource(R.string.settings_adaptive), stringResource(R.string.settings_adaptive_desc), s.adaptiveDifficulty, "settings_adaptive") { v ->
                    viewModel.update { it.copy(adaptiveDifficulty = v) }
                }
                SwitchRow(stringResource(R.string.settings_contrast), stringResource(R.string.settings_contrast_desc), s.catContrast, "settings_contrast") { v ->
                    viewModel.update { it.copy(catContrast = v) }
                }
                SwitchRow(stringResource(R.string.settings_favorite), stringResource(R.string.settings_favorite_desc), s.favoritePreyMoreOften, "settings_favorite") { v ->
                    viewModel.update { it.copy(favoritePreyMoreOften = v) }
                }

                SectionTitle(stringResource(R.string.settings_section_sound))
                VolumeSlider(stringResource(R.string.settings_fx_volume), s.effectsVolume) { v -> viewModel.update { it.copy(effectsVolume = v) } }
                VolumeSlider(stringResource(R.string.settings_ambient_volume), s.ambientVolume) { v -> viewModel.update { it.copy(ambientVolume = v) } }
                SwitchRow(stringResource(R.string.settings_stereo), stringResource(R.string.settings_stereo_desc), s.stereoPanning, "settings_stereo") { v ->
                    viewModel.update { it.copy(stereoPanning = v) }
                }
                SwitchRow(stringResource(R.string.settings_vibration), null, s.vibration, "settings_vibration") { v ->
                    viewModel.update { it.copy(vibration = v) }
                }

                SectionTitle(stringResource(R.string.settings_section_session))
                Text(stringResource(R.string.settings_session_timer), style = MaterialTheme.typography.bodyLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameSettings.SESSION_OPTIONS.forEach { m ->
                        FilterChip(
                            selected = s.sessionMinutes == m,
                            onClick = { viewModel.update { it.copy(sessionMinutes = m) } },
                            label = { Text(if (m == 0) stringResource(R.string.settings_unlimited) else stringResource(R.string.settings_minutes, m)) },
                            modifier = Modifier.testTag("session_$m"),
                        )
                    }
                }
                SwitchRow(stringResource(R.string.settings_treat), null, s.treatReminder, "settings_treat") { v ->
                    viewModel.update { it.copy(treatReminder = v) }
                }

                SectionTitle(stringResource(R.string.settings_section_scene))
                Text(stringResource(R.string.settings_night), style = MaterialTheme.typography.bodyLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NightMode.entries.forEach { mode ->
                        FilterChip(
                            selected = s.nightMode == mode,
                            onClick = { viewModel.update { it.copy(nightMode = mode) } },
                            label = { Text(stringResource(mode.nameRes)) },
                        )
                    }
                }
                SwitchRow(stringResource(R.string.settings_seasons), stringResource(R.string.settings_seasons_desc), s.seasons, "settings_seasons") { v ->
                    viewModel.update { it.copy(seasons = v) }
                }
                SwitchRow(stringResource(R.string.settings_star_unlock), null, s.starUnlock, "settings_star_unlock") { v ->
                    viewModel.update { it.copy(starUnlock = v) }
                }

                SectionTitle(stringResource(R.string.settings_section_reminders))
                SwitchRow(stringResource(R.string.settings_reminders), null, s.remindersEnabled, "settings_reminders") { v ->
                    if (v && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.update { it.copy(remindersEnabled = v) }
                    }
                }
                if (s.remindersEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.settings_reminder_time), modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { showTime = true }) { Text("%02d:%02d".format(s.reminderHour, s.reminderMinute)) }
                    }
                    Text(stringResource(R.string.settings_reminder_days), style = MaterialTheme.typography.bodyLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..7).forEach { d ->
                            FilterChip(
                                selected = d in s.reminderDays,
                                onClick = {
                                    viewModel.update { it.copy(reminderDays = if (d in it.reminderDays) it.reminderDays - d else it.reminderDays + d) }
                                },
                                label = { Text(stringResource(dayRes(d))) },
                            )
                        }
                    }
                }

                SectionTitle(stringResource(R.string.settings_section_camera))
                SwitchRow(stringResource(R.string.settings_photo), stringResource(R.string.settings_photo_desc), s.photoOnCatch, "settings_photo") { v ->
                    if (v) withCamera { it.copy(photoOnCatch = true) } else viewModel.update { it.copy(photoOnCatch = false) }
                }
                SwitchRow(stringResource(R.string.settings_video), stringResource(R.string.settings_video_desc), s.videoRecording, "settings_video") { v ->
                    if (v) withCamera { it.copy(videoRecording = true) } else viewModel.update { it.copy(videoRecording = false) }
                }

                SectionTitle(stringResource(R.string.settings_section_other))
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = s.language == lang,
                            onClick = { viewModel.update { it.copy(language = lang) } },
                            label = { Text(stringResource(lang.labelRes())) },
                            modifier = Modifier.testTag("lang_${lang.name}"),
                        )
                    }
                }
                Spacer(Modifier.padding(8.dp))
                OutlinedButton(onClick = { showReset = true }, modifier = Modifier.fillMaxWidth().testTag("settings_reset")) {
                    Text(stringResource(R.string.settings_reset), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        SnackbarHost(snackbar)
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.settings_reset_title)) },
            text = { Text(stringResource(R.string.settings_reset_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReset = false
                        viewModel.resetStats { scope.launch { snackbar.showSnackbar(resetDoneText) } }
                    },
                    modifier = Modifier.testTag("settings_reset_confirm"),
                ) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
    if (showTime) {
        val picker = rememberTimePickerState(s.reminderHour, s.reminderMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            text = { TimePicker(picker) },
            confirmButton = {
                TextButton(onClick = {
                    showTime = false
                    viewModel.update { it.copy(reminderHour = picker.hour, reminderMinute = picker.minute) }
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.lang_system
    AppLanguage.UK -> R.string.lang_uk
    AppLanguage.EN -> R.string.lang_en
}

@Composable
private fun SwitchRow(title: String, description: String?, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun StepSlider(label: String, value: Int, range: IntRange, valueText: String, tag: String, onChange: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(valueText, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            modifier = Modifier.testTag(tag).semantics { contentDescription = label; stateDescription = valueText },
        )
    }
}

@Composable
private fun VolumeSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    val percent = stringResource(R.string.percent_format, (value * 100).roundToInt())
    Column(Modifier.padding(vertical = 4.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(percent, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.semantics { contentDescription = label; stateDescription = percent },
        )
    }
}
