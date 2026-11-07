package app.purrhunt.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.purrhunt.data.db.CatEntity
import app.purrhunt.data.db.PurrDatabase
import app.purrhunt.domain.Achievement
import app.purrhunt.domain.CaughtPrey
import app.purrhunt.domain.GameMode
import app.purrhunt.domain.HeatmapGrid
import app.purrhunt.domain.Interaction
import app.purrhunt.domain.Location
import app.purrhunt.domain.SessionResult
import app.purrhunt.domain.Species
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class StatsRepositoryTest {
    private lateinit var db: PurrDatabase
    private lateinit var repo: StatsRepository
    private var catId = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PurrDatabase::class.java).allowMainThreadQueries().build()
        repo = StatsRepository(db.statsDao(), db.achievementDao()) { ZoneOffset.UTC }
        catId = db.catDao().insert(CatEntity(name = "Murka", colorArgb = 0, createdAt = 0))
    }

    @After
    fun tearDown() = db.close()

    private fun result(startedAt: Long, catches: List<Species>, misses: Int = 0, mode: GameMode = GameMode.HUNT) = SessionResult(
        catId = catId,
        startedAt = startedAt,
        durationMs = 60_000,
        location = Location.BASEMENT,
        night = false,
        mode = mode,
        catches = catches.mapIndexed { i, s -> CaughtPrey(s, startedAt + i) },
        misses = misses,
        bestStreak = catches.size,
        heatmap = IntArray(HeatmapGrid.COLS * HeatmapGrid.ROWS).also { it[0] = catches.size + misses },
        interactions = listOf(Interaction(Species.GRAY_MOUSE, 60_000, 12)),
    )

    @Test
    fun `saving a session aggregates statistics`() = runTest {
        val summary = repo.saveSession(result(0, List(12) { Species.GRAY_MOUSE } + Species.WHITE_MOUSE, misses = 3))
        assertEquals(13, summary.catches)
        assertEquals(3, summary.misses)
        assertEquals(1 + 3, summary.stars) // 13 catches -> 1 star, rare white mouse -> +3
        assertEquals(listOf(Species.WHITE_MOUSE), summary.rare)
        assertEquals(12, summary.perSpecies[Species.GRAY_MOUSE])
        assertTrue(Achievement.FIRST_CATCH in summary.newAchievements)
        assertTrue(Achievement.RARE_FIRST in summary.newAchievements)

        val stats = repo.stats(catId)
        assertEquals(13, stats.totalCatches)
        assertEquals(1, stats.rareCatches)
        assertEquals(setOf(Location.BASEMENT), stats.locationsPlayed)
    }

    @Test
    fun `achievements are reported only once`() = runTest {
        repo.saveSession(result(0, listOf(Species.RAT)))
        val second = repo.saveSession(result(86_400_000, listOf(Species.RAT)))
        assertTrue(Achievement.FIRST_CATCH !in second.newAchievements)
        assertEquals(2, repo.stats(catId).playDays.size)
    }

    @Test
    fun `journal contains bestiary, heatmap and favourite`() = runTest {
        repo.saveSession(result(0, listOf(Species.GRAY_MOUSE, Species.GRAY_MOUSE)))
        repo.saveSession(result(1000, listOf(Species.GRAY_MOUSE)))
        val journal = repo.observeJournal(catId).first()
        val mouse = journal.bestiary.first { it.species == Species.GRAY_MOUSE }
        assertEquals(3, mouse.caught)
        assertEquals(2, mouse.bestSession)
        assertEquals(Species.GRAY_MOUSE, journal.favorite)
        assertEquals(1, journal.lastHeatmap[0])
        assertEquals(3, journal.allHeatmap[0])
        assertEquals(1, journal.daily.size)
    }

    @Test
    fun `reset removes sessions but keeps cats`() = runTest {
        repo.saveSession(result(0, listOf(Species.FLY), mode = GameMode.LASER))
        assertEquals(1, repo.stats(catId).laserCatches)
        repo.resetAll()
        assertEquals(0, repo.stats(catId).totalCatches)
        assertEquals(1, db.catDao().count())
    }
}
