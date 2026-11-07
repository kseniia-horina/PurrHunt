package app.purrhunt.ui.game

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import app.purrhunt.ui.game.camera.CatCamera
import app.purrhunt.ui.game.camera.ScreenRecordService
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.purrhunt.R
import app.purrhunt.domain.GameMode

@Composable
fun GameScreen(
    onFinished: (Long) -> Unit,
    onChangeLocation: (GameMode) -> Unit,
    onChangeMode: () -> Unit,
    onExitToMenu: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    ImmersiveGameWindow()

    // The cat must not be able to leave: back is swallowed while playing and resumes from the pause screen.
    BackHandler { if (ui.paused) viewModel.resume() }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.onBackground()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    DisposableEffect(activity) {
        onDispose { activity?.let { ScreenPinning.stop(it) } }
    }
    LaunchedEffect(ui.finishedSessionId) {
        ui.finishedSessionId?.let { id ->
            activity?.let { ScreenPinning.stop(it) }
            onFinished(id)
        }
    }

    fun leave(action: () -> Unit) {
        activity?.let { ScreenPinning.stop(it) }
        viewModel.finish(showSummary = false, then = action)
    }

    val context = LocalContext.current
    val cameraAllowed = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    // The recording consent is a system dialog: ask for it before the screen gets pinned.
    var recordingAnswered by remember { mutableStateOf(false) }
    val recordLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) ScreenRecordService.start(context, result.resultCode, data)
        recordingAnswered = true
    }
    LaunchedEffect(ui.pinRequested, ui.videoRecording, recordingAnswered) {
        if (ui.pinRequested && (!ui.videoRecording || recordingAnswered) && activity != null) ScreenPinning.start(activity)
    }
    LaunchedEffect(ui.videoRecording, ui.pinRequested) {
        if (ui.videoRecording && ui.pinRequested) {
            recordLauncher.launch(context.getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())
        }
    }
    DisposableEffect(Unit) { onDispose { ScreenRecordService.stop(context) } }

    val engine = ui.engine
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(if (engine != null && !ui.paused) Modifier.gameInput(engine) else Modifier),
    ) {
        val renderer = ui.renderer
        if (engine != null && renderer != null) {
            GameSurface(engine, renderer, viewModel::onFrame, interactive = false)
            ExitCornerHint(visible = !ui.paused && !ui.showLockHint, modifier = Modifier.align(Alignment.TopEnd))
            if (cameraAllowed && (ui.photoOnCatch || ui.videoRecording)) {
                CatCamera(
                    requests = viewModel.photoRequests,
                    showPreview = ui.videoRecording,
                    onSaved = viewModel::photoSaved,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                )
            }
        }
        if (ui.paused) {
            PauseOverlay(
                caught = ui.caught,
                timeLeftSeconds = ui.timeLeftSeconds,
                canExtend = ui.canExtend,
                onExtend = viewModel::extendSession,
                onResume = viewModel::resume,
                onChangeLocation = { leave { onChangeLocation(viewModel.mode) } },
                onChangeMode = { leave(onChangeMode) },
                onFinish = { viewModel.finish(showSummary = true) },
                onMenu = { leave(onExitToMenu) },
            )
        }
    }

    if (ui.showLockHint) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.lock_hint_title)) },
            text = { Text(stringResource(R.string.lock_hint_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::lockHintAccepted, modifier = Modifier.testTag("lock_hint_ok")) {
                    Text(stringResource(R.string.lock_hint_ok))
                }
            },
        )
    }
}

@Composable
private fun PauseOverlay(
    caught: Int,
    timeLeftSeconds: Int?,
    canExtend: Boolean,
    onExtend: () -> Unit,
    onResume: () -> Unit,
    onChangeLocation: () -> Unit,
    onChangeMode: () -> Unit,
    onFinish: () -> Unit,
    onMenu: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE6101520))
            .testTag("pause_overlay"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.pause_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.pause_caught, caught), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (timeLeftSeconds != null) {
                Text(
                    stringResource(R.string.pause_time_left, "%d:%02d".format(timeLeftSeconds / 60, timeLeftSeconds % 60)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("pause_time_left"),
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("pause_resume")) {
                Text(stringResource(R.string.pause_resume))
            }
            if (canExtend) {
                FilledTonalButton(onClick = onExtend, modifier = Modifier.fillMaxWidth().testTag("pause_extend")) {
                    Text(stringResource(R.string.pause_extend))
                }
            }
            FilledTonalButton(onClick = onChangeLocation, modifier = Modifier.fillMaxWidth().testTag("pause_location")) {
                Text(stringResource(R.string.pause_change_location))
            }
            FilledTonalButton(onClick = onChangeMode, modifier = Modifier.fillMaxWidth().testTag("pause_mode")) {
                Text(stringResource(R.string.pause_change_mode))
            }
            FilledTonalButton(onClick = onFinish, modifier = Modifier.fillMaxWidth().testTag("pause_finish")) {
                Text(stringResource(R.string.pause_finish))
            }
            OutlinedButton(onClick = onMenu, modifier = Modifier.fillMaxWidth().testTag("pause_menu")) {
                Text(stringResource(R.string.pause_to_menu))
            }
        }
    }
}

/**
 * Owner hint next to the exit dial, shown for a few seconds when play starts or resumes. Text only, no
 * touch target: touches still go to the game, and a cat can't read it.
 */
@Composable
private fun ExitCornerHint(visible: Boolean, modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        show = visible
        if (visible) {
            delay(HINT_MS)
            show = false
        }
    }
    AnimatedVisibility(visible = show, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Text(
            stringResource(R.string.game_exit_hint),
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(start = 16.dp, top = 22.dp, end = 76.dp)
                .background(Color(0x99000000), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("exit_hint"),
        )
    }
}

private const val HINT_MS = 6_000L
