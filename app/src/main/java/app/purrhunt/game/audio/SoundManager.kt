package app.purrhunt.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.SystemClock
import app.purrhunt.R
import app.purrhunt.domain.Location
import app.purrhunt.domain.Species
import app.purrhunt.game.engine.SoundCue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Low-latency effects on SoundPool (preloaded per scene, at most [MAX_STREAMS] at once) with equal-power
 * stereo panning, plus one looping ambience on MediaPlayer.
 */
class SoundManager(
    private val context: Context,
    private var effectsVolume: Float,
    private var ambientVolume: Float,
    private val stereo: Boolean,
) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    /** Loaded sample ids per cue (variants). */
    private val samples = HashMap<SoundCue, IntArray>()
    private val lastPlayed = LongArray(SoundCue.entries.size)
    private var variantCounter = 0
    private val loopStreams = HashMap<Int, Int>()
    private val loopCue = HashMap<Int, SoundCue>()
    private var ambient: MediaPlayer? = null
    private var ambientFade = 1f
    private var muted = false

    fun preload(cues: Collection<SoundCue>) {
        for (cue in cues) {
            if (samples.containsKey(cue)) continue
            samples[cue] = resources(cue).map { pool.load(context, it, 1) }.toIntArray()
        }
    }

    fun play(cue: SoundCue, pan: Float, volume: Float) {
        if (muted || effectsVolume <= 0f) return
        val ids = samples[cue] ?: return
        val now = SystemClock.uptimeMillis()
        // The same cue twice within 60 ms just sounds like a glitch.
        if (now - lastPlayed[cue.ordinal] < MIN_GAP_MS) return
        lastPlayed[cue.ordinal] = now
        val id = ids[(variantCounter++) % ids.size]
        val v = volume * effectsVolume
        pool.play(id, left(pan) * v, right(pan) * v, 1, 0, 1f)
    }

    /** Starts or updates a looping sound bound to [key] (a creature id). */
    fun loop(key: Int, cue: SoundCue, pan: Float, volume: Float) {
        if (muted) return
        val stream = loopStreams[key]
        val v = volume * effectsVolume
        if (stream != null && loopCue[key] == cue) {
            pool.setVolume(stream, left(pan) * v, right(pan) * v)
            return
        }
        if (stream != null) pool.stop(stream)
        val ids = samples[cue] ?: return
        val id = pool.play(ids[0], left(pan) * v, right(pan) * v, 0, -1, 1f)
        if (id != 0) {
            loopStreams[key] = id
            loopCue[key] = cue
        }
    }

    fun stopLoop(key: Int) {
        loopStreams.remove(key)?.let { pool.stop(it) }
        loopCue.remove(key)
    }

    fun stopAllLoops() {
        for (s in loopStreams.values) pool.stop(s)
        loopStreams.clear()
        loopCue.clear()
    }

    fun startAmbient(location: Location, night: Boolean) {
        ambient?.release()
        ambient = MediaPlayer.create(context, ambientResource(location, night))?.apply {
            isLooping = true
            setVolume(ambientVolume, ambientVolume)
            if (!muted) start()
        }
        ambientFade = 1f
    }

    /** Fades the ambience to [target] (0..1 of its volume): used when prey "goes to sleep". */
    fun setAmbientFade(target: Float) {
        ambientFade = target.coerceIn(0f, 1f)
        val v = ambientVolume * ambientFade
        ambient?.setVolume(v, v)
    }

    fun setVolumes(effects: Float, ambience: Float) {
        effectsVolume = effects
        ambientVolume = ambience
        setAmbientFade(ambientFade)
    }

    /** Silences everything when the app loses focus; [resume] restores. */
    fun pause() {
        muted = true
        stopAllLoops()
        pool.autoPause()
        ambient?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        muted = false
        pool.autoResume()
        ambient?.start()
    }

    fun release() {
        stopAllLoops()
        ambient?.release()
        ambient = null
        pool.release()
    }

    private fun left(pan: Float): Float = if (stereo) cos(pan.coerceIn(0f, 1f) * HALF_PI) else CENTRE
    private fun right(pan: Float): Float = if (stereo) sin(pan.coerceIn(0f, 1f) * HALF_PI) else CENTRE

    companion object {
        const val MAX_STREAMS = 8
        private const val MIN_GAP_MS = 60L
        private const val HALF_PI = (PI / 2).toFloat()
        private const val CENTRE = 0.7071f

        fun resources(cue: SoundCue): List<Int> = when (cue) {
            SoundCue.MOUSE_SQUEAK -> listOf(R.raw.mouse_squeak_1, R.raw.mouse_squeak_2, R.raw.mouse_squeak_3, R.raw.mouse_squeak_4)
            SoundCue.MOUSE_RUSTLE -> listOf(R.raw.mouse_rustle_1, R.raw.mouse_rustle_2)
            SoundCue.MOUSE_CAUGHT -> listOf(R.raw.mouse_caught)
            SoundCue.RAT_SQUEAK -> listOf(R.raw.rat_squeak_1, R.raw.rat_squeak_2)
            SoundCue.RAT_CAUGHT -> listOf(R.raw.rat_caught)
            SoundCue.SPARROW_CHIRP -> listOf(R.raw.sparrow_chirp_1, R.raw.sparrow_chirp_2)
            SoundCue.TIT_TRILL -> listOf(R.raw.tit_trill_1, R.raw.tit_trill_2)
            SoundCue.BULLFINCH_CALL -> listOf(R.raw.bullfinch_call)
            SoundCue.WING_FLAP -> listOf(R.raw.wing_flap)
            SoundCue.FLY_BUZZ -> listOf(R.raw.fly_buzz)
            SoundCue.CRICKET_CHIRP -> listOf(R.raw.cricket_chirp)
            SoundCue.INSECT_RUSTLE -> listOf(R.raw.insect_rustle)
            SoundCue.BEETLE_HUM -> listOf(R.raw.beetle_hum)
            SoundCue.WATER_BLOOP -> listOf(R.raw.water_bloop)
            SoundCue.WATER_SPLASH -> listOf(R.raw.water_splash)
            SoundCue.BUBBLES -> listOf(R.raw.bubbles)
            SoundCue.FIREFLY_TWINKLE -> listOf(R.raw.firefly_twinkle)
            SoundCue.FEATHER_SWISH -> listOf(R.raw.feather_swish)
            SoundCue.DOT_CHIME -> listOf(R.raw.dot_chime)
        }

        fun ambientResource(location: Location, night: Boolean): Int = when {
            night -> R.raw.amb_night
            location == Location.BASEMENT -> R.raw.amb_basement
            location == Location.GARDEN -> R.raw.amb_garden
            location == Location.MEADOW -> R.raw.amb_meadow
            location == Location.POND -> R.raw.amb_pond
            else -> R.raw.amb_pond
        }

        /** Cues a set of species can produce, so only those are loaded for a scene. */
        fun cuesFor(species: Collection<Species>): Set<SoundCue> {
            val set = HashSet<SoundCue>()
            set += SoundCue.FEATHER_SWISH
            set += SoundCue.DOT_CHIME
            for (s in species) set += when (s) {
                Species.GRAY_MOUSE, Species.MOUSELET, Species.WHITE_MOUSE -> listOf(SoundCue.MOUSE_SQUEAK, SoundCue.MOUSE_RUSTLE, SoundCue.MOUSE_CAUGHT)
                Species.RAT -> listOf(SoundCue.RAT_SQUEAK, SoundCue.RAT_CAUGHT, SoundCue.MOUSE_RUSTLE)
                Species.SPARROW -> listOf(SoundCue.SPARROW_CHIRP, SoundCue.WING_FLAP)
                Species.TIT -> listOf(SoundCue.TIT_TRILL, SoundCue.WING_FLAP)
                Species.BULLFINCH -> listOf(SoundCue.BULLFINCH_CALL, SoundCue.WING_FLAP)
                Species.FLY -> listOf(SoundCue.FLY_BUZZ, SoundCue.INSECT_RUSTLE)
                Species.CRICKET -> listOf(SoundCue.CRICKET_CHIRP, SoundCue.INSECT_RUSTLE)
                Species.BEETLE -> listOf(SoundCue.BEETLE_HUM, SoundCue.INSECT_RUSTLE)
                Species.BUTTERFLY, Species.SWALLOWTAIL, Species.LADYBUG -> listOf(SoundCue.INSECT_RUSTLE)
                Species.CRUCIAN, Species.GOLDFISH, Species.TADPOLE -> listOf(SoundCue.WATER_BLOOP, SoundCue.WATER_SPLASH, SoundCue.BUBBLES)
                Species.WATER_STRIDER -> listOf(SoundCue.WATER_BLOOP, SoundCue.WATER_SPLASH)
                Species.FIREFLY, Species.GIANT_FIREFLY -> listOf(SoundCue.FIREFLY_TWINKLE)
            }
            return set
        }
    }
}
