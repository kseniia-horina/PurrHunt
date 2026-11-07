package app.purrhunt.ui.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.purrhunt.BuildConfig
import app.purrhunt.R
import app.purrhunt.data.CatRepository
import app.purrhunt.data.PhotoRepository
import app.purrhunt.data.SettingsRepository
import app.purrhunt.data.StatsRepository
import app.purrhunt.domain.CatProfile
import app.purrhunt.domain.GameMode
import app.purrhunt.domain.GameSettings
import app.purrhunt.domain.Location
import app.purrhunt.domain.NightClock
import app.purrhunt.domain.SceneSpec
import app.purrhunt.domain.Season
import app.purrhunt.domain.SessionResult
import app.purrhunt.domain.Species
import app.purrhunt.game.audio.SoundManager
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.engine.EngineSpec
import app.purrhunt.game.engine.GameEngine
import app.purrhunt.game.engine.GameEvents
import app.purrhunt.game.engine.GameRandom
import app.purrhunt.game.engine.SoundCue
import app.purrhunt.game.render.SceneRenderer
import app.purrhunt.game.scenes.SceneLook
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class GameUiState(
    val engine: GameEngine? = null,
    val renderer: SceneRenderer? = null,
    val paused: Boolean = false,
    val showLockHint: Boolean = false,
    val pinRequested: Boolean = false,
    val caught: Int = 0,
    /** Seconds left in a timed session at the moment of pausing; null for unlimited sessions. */
    val timeLeftSeconds: Int? = null,
    /** "+5 min" is offered in the pause menu. */
    val canExtend: Boolean = false,
    val finishedSessionId: Long? = null,
    val saving: Boolean = false,
    val photoOnCatch: Boolean = false,
    val videoRecording: Boolean = false,
)

@HiltViewModel
class GameViewModel @Inject constructor(
    savedState: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val cats: CatRepository,
    private val stats: StatsRepository,
    private val random: GameRandom,
    private val photos: PhotoRepository,
) : ViewModel(), GameEvents {
    val location: Location = Location.byName(savedState["location"] ?: Location.BASEMENT.name)
    private val requestedNight: Boolean = savedState["night"] ?: false
    val mode: GameMode = GameMode.valueOf(savedState["mode"] ?: GameMode.HUNT.name)

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui

    private var sound: SoundManager? = null
    private var settings = GameSettings()
    private var catId: Long? = null
    private var vibrator: Vibrator? = null
    private val startedAt = System.currentTimeMillis()
    private var saved = false
    private var night = false

    private val _photoRequests = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Emits a species tag whenever the front camera should take a picture. */
    val photoRequests: SharedFlow<String> = _photoRequests
    private var lastPhotoAt = 0L

    private var loopTick = 0
    private var fpsFrames = 0
    private var fpsSince = 0L

    init {
        viewModelScope.launch {
            settings = settingsRepo.current()
            val app = settingsRepo.appState.first()
            // Sessions and photos belong to a cat: create a default profile on the very first game.
            catId = app.currentCatId?.takeIf { cats.get(it) != null }
                ?: cats.cats.first().firstOrNull()?.id
                ?: cats.save(CatProfile(name = context.getString(R.string.default_cat_name), colorArgb = CatProfile.COLORS.first()))
            val now = LocalDateTime.now()
            night = NightClock.resolveNight(location, requestedNight, settings.nightMode, now.hour)
            val season = if (settings.seasons) Season.ofMonth(now.monthValue) else Season.SUMMER
            val scene = SceneSpec(location, night, season)
            val favorite = if (settings.favoritePreyMoreOften) catId?.let { stats.favoritePrey(it) } else null
            val spec = EngineSpec(
                scene = scene,
                mode = mode,
                speedLevel = settings.speedLevel,
                adaptive = settings.adaptiveDifficulty,
                creatureCount = settings.creatureCount,
                favorite = favorite?.takeIf { it in scene.common },
                sessionSeconds = settings.sessionMinutes * 60,
            )
            val engine = GameEngine(spec, random, this@GameViewModel, startedAt)
            sound = SoundManager(context, settings.effectsVolume, settings.ambientVolume, settings.stereoPanning).apply {
                preload(SoundManager.cuesFor(scene.common + scene.rare))
                startAmbient(location, night)
            }
            vibrator = if (settings.vibration) vibrator() else null
            _ui.update {
                it.copy(
                    engine = engine,
                    renderer = SceneRenderer(SceneLook(night, season), settings.catContrast, ownerDial = true),
                    showLockHint = !app.lockTaskHintShown,
                    pinRequested = app.lockTaskHintShown,
                    photoOnCatch = settings.photoOnCatch,
                    videoRecording = settings.videoRecording,
                )
            }
        }
    }

    fun lockHintAccepted() {
        viewModelScope.launch { settingsRepo.setLockTaskHintShown() }
        _ui.update { it.copy(showLockHint = false, pinRequested = true) }
    }

    /** Called once per display frame from the game surface. */
    fun onFrame(frameNanos: Long) {
        val engine = _ui.value.engine ?: return
        if (_ui.value.paused) return
        engine.frame(frameNanos)
        if (engine.exitDetector.triggered) {
            engine.exitDetector.reset()
            engine.pointersCancelled()
            pause()
            return
        }
        if (engine.phase == GameEngine.Phase.BEDTIME) sound?.setAmbientFade(1f - engine.dusk)
        if (++loopTick % LOOP_UPDATE_FRAMES == 0) updateLoops(engine)
        if (engine.phase == GameEngine.Phase.FINISHED) finish(showSummary = true)
        if (BuildConfig.DEBUG) countFps(frameNanos)
    }

    private fun updateLoops(engine: GameEngine) {
        val s = sound ?: return
        val w = engine.world
        for (c in w.creatures) {
            val cue = when {
                !c.active || !c.flying || engine.phase != GameEngine.Phase.PLAYING -> null
                c.species == Species.FLY -> SoundCue.FLY_BUZZ
                c.species == Species.BEETLE -> SoundCue.BEETLE_HUM
                else -> null
            }
            if (cue == null) {
                s.stopLoop(c.id)
            } else {
                val speed = (c.speed / (c.config.dartSpeed * w.dp)).coerceIn(0.2f, 1f)
                s.loop(c.id, cue, (c.x / w.width).coerceIn(0f, 1f), 0.25f + 0.45f * speed)
            }
        }
    }

    private fun countFps(frameNanos: Long) {
        fpsFrames++
        if (fpsSince == 0L) fpsSince = frameNanos
        val elapsed = frameNanos - fpsSince
        if (elapsed >= 5_000_000_000L) {
            Log.d(TAG, "fps=%.1f".format(fpsFrames * 1e9 / elapsed))
            fpsFrames = 0
            fpsSince = frameNanos
        }
    }

    fun pause() {
        val engine = _ui.value.engine ?: return
        engine.paused = true
        sound?.pause()
        _ui.update { it.copy(paused = true, caught = engine.tracker.catches, timeLeftSeconds = engine.remainingSeconds?.toInt(), canExtend = engine.canExtend) }
    }

    /** The app went to the background: pause and mute. */
    fun onBackground() {
        if (_ui.value.engine != null && !_ui.value.paused) pause()
    }

    /** Adds five minutes to the hunt; the pause menu shows the new time left. */
    fun extendSession() {
        val engine = _ui.value.engine ?: return
        engine.extend(EXTEND_SECONDS)
        _ui.update { it.copy(timeLeftSeconds = engine.remainingSeconds?.toInt(), canExtend = engine.canExtend) }
    }

    fun resume() {
        val engine = _ui.value.engine ?: return
        engine.resetClock()
        engine.paused = false
        sound?.resume()
        _ui.update { it.copy(paused = false) }
    }

    /** Saves the session (if anything happened) and reports its id when the summary should be shown. */
    fun finish(showSummary: Boolean, then: (() -> Unit)? = null) {
        val engine = _ui.value.engine ?: return
        if (saved || _ui.value.saving) return
        _ui.update { it.copy(saving = true) }
        engine.finishNow()
        sound?.pause()
        viewModelScope.launch {
            val worthSaving = engine.playTime >= MIN_SAVED_SECONDS || engine.tracker.catches > 0
            val id = if (worthSaving || showSummary) saveSession(engine) else null
            saved = true
            if (showSummary && id != null) _ui.update { it.copy(finishedSessionId = id) }
            then?.invoke()
        }
    }

    private suspend fun saveSession(engine: GameEngine): Long {
        val cat = catId ?: error("game started without a cat profile")
        val result = SessionResult(
            catId = cat,
            startedAt = startedAt,
            durationMs = (engine.playTime * 1000).toLong(),
            location = location,
            night = night,
            mode = mode,
            catches = engine.tracker.caughtList(),
            misses = engine.tracker.misses,
            bestStreak = engine.tracker.bestStreak,
            heatmap = engine.tracker.heatmap.copyOf(),
            interactions = engine.tracker.interactions(),
        )
        return stats.saveSession(result).sessionId
    }

    // ---------------------------------------------------------------- GameEvents

    override fun onSound(cue: SoundCue, pan: Float, volume: Float) {
        sound?.play(cue, pan, volume)
    }

    override fun onCatch(creature: Creature) {
        vibrate()
        requestPhoto(creature.species.name)
    }

    /** At most one photo every 10 s, so a streak of catches doesn't fill the album. */
    private fun requestPhoto(tag: String) {
        val now = System.currentTimeMillis()
        if (settings.photoOnCatch && now - lastPhotoAt >= PHOTO_INTERVAL_MS) {
            lastPhotoAt = now
            _photoRequests.tryEmit(tag)
        }
    }

    fun photoSaved(path: String, tag: String) {
        val cat = catId ?: return
        viewModelScope.launch { photos.add(cat, path, tag) }
    }

    override fun onMiss(x: Float, y: Float) = Unit

    override fun onToyCaught(x: Float, y: Float) {
        vibrate()
        requestPhoto(mode.name)
    }

    private fun vibrate() {
        val v = vibrator ?: return
        v.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    override fun onCleared() {
        sound?.release()
        sound = null
    }

    private companion object {
        const val TAG = "PurrHunt"
        const val LOOP_UPDATE_FRAMES = 6
        const val MIN_SAVED_SECONDS = 10.0
        const val PHOTO_INTERVAL_MS = 10_000L
        const val EXTEND_SECONDS = 5 * 60
    }
}
