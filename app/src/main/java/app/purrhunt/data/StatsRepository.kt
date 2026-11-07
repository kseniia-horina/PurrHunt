package app.purrhunt.data

import app.purrhunt.data.db.AchievementDao
import app.purrhunt.data.db.AchievementEntity
import app.purrhunt.data.db.CatchEntity
import app.purrhunt.data.db.CatchRow
import app.purrhunt.data.db.InteractionEntity
import app.purrhunt.data.db.SessionEntity
import app.purrhunt.data.db.StatsDao
import app.purrhunt.domain.Achievement
import app.purrhunt.domain.CatStats
import app.purrhunt.domain.FavoritePrey
import app.purrhunt.domain.GameMode
import app.purrhunt.domain.HeatmapGrid
import app.purrhunt.domain.Interaction
import app.purrhunt.domain.Location
import app.purrhunt.domain.SessionResult
import app.purrhunt.domain.SessionSummary
import app.purrhunt.domain.Species
import app.purrhunt.domain.Stars
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class BestiaryEntry(val species: Species, val caught: Int, val bestSession: Int)

data class DailyPoint(val epochDay: Long, val catches: Int, val playMs: Long)

data class Journal(
    val stats: CatStats,
    val bestiary: List<BestiaryEntry>,
    val daily: List<DailyPoint>,
    val lastHeatmap: IntArray,
    val allHeatmap: IntArray,
    val favorite: Species?,
    val favoriteRate: Float,
)

@Singleton
class StatsRepository @Inject constructor(
    private val stats: StatsDao,
    private val achievements: AchievementDao,
    private val zoneProvider: ZoneProvider,
) {
    private fun zone(): ZoneId = zoneProvider.zone()

    fun observeStats(catId: Long): Flow<CatStats> =
        combine(stats.observeSessions(catId), stats.observeCatches(catId)) { s, c -> StatsCalculator.compute(s, c, zone()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeStatsOrEmpty(catId: Flow<Long?>): Flow<CatStats> =
        catId.flatMapLatest { id -> if (id == null) flowOf(CatStats()) else observeStats(id) }

    suspend fun stats(catId: Long): CatStats = StatsCalculator.compute(stats.sessions(catId), stats.catches(catId), zone())

    fun observeUnlocked(catId: Long): Flow<Map<Achievement, Long>> = achievements.observe(catId).map { list ->
        list.mapNotNull { e -> Achievement.entries.firstOrNull { it.name == e.achievement }?.let { it to e.unlockedAt } }.toMap()
    }

    fun observeJournal(catId: Long): Flow<Journal> = combine(
        stats.observeSessions(catId),
        stats.observeCatches(catId),
        stats.observeBestPerSpecies(catId),
        stats.observeInteractions(catId),
    ) { sessions, catches, best, interactions ->
        val z = zone()
        val s = StatsCalculator.compute(sessions, catches, z)
        val bestMap = best.associate { it.species to it.best }
        val bestiary = Species.bestiary.map { BestiaryEntry(it, s.caught(it), bestMap[it.name] ?: 0) }
        val inter = interactions.mapNotNull { i -> Species.byName(i.species)?.let { Interaction(it, i.exposureMs, i.nearTouches) } }
        val fav = FavoritePrey.find(inter)
        val favRate = fav?.let { f ->
            val list = inter.filter { it.species == f }
            FavoritePrey.touchesPerMinute(Interaction(f, list.sumOf { it.exposureMs }, list.sumOf { it.nearTouches }))
        } ?: 0f
        Journal(
            stats = s,
            bestiary = bestiary,
            daily = StatsCalculator.daily(sessions, catches, z),
            lastHeatmap = sessions.lastOrNull()?.let { HeatmapGrid.decode(it.heatmap) } ?: IntArray(HeatmapGrid.COLS * HeatmapGrid.ROWS),
            allHeatmap = HeatmapGrid.sum(sessions.map { HeatmapGrid.decode(it.heatmap) }),
            favorite = fav,
            favoriteRate = favRate,
        )
    }

    suspend fun favoritePrey(catId: Long): Species? = FavoritePrey.find(
        stats.interactions(catId).mapNotNull { i -> Species.byName(i.species)?.let { Interaction(it, i.exposureMs, i.nearTouches) } },
    )

    /** Persists a finished session and returns its summary with stars and freshly unlocked achievements. */
    suspend fun saveSession(result: SessionResult): SessionSummary {
        val before = stats(result.catId)
        val rareCount = result.catches.count { it.species?.rare == true }
        val stars = Stars.earned(before.totalCatches, before.rareCatches, result.catches.size, rareCount)
        val endedAt = result.startedAt + result.durationMs
        val session = SessionEntity(
            catId = result.catId,
            startedAt = result.startedAt,
            durationMs = result.durationMs,
            location = result.location.name,
            night = result.night,
            mode = result.mode.name,
            catches = result.catches.size,
            misses = result.misses,
            bestStreak = result.bestStreak,
            stars = stars,
            heatmap = HeatmapGrid.encode(result.heatmap),
        )
        val catchRows = result.catches.map {
            CatchEntity(
                sessionId = 0, catId = result.catId, species = it.species?.name ?: result.mode.name, timestamp = it.timestamp,
                rare = it.species?.rare == true, night = result.night, mode = result.mode.name,
            )
        }
        val interactionRows = result.interactions.map {
            InteractionEntity(sessionId = 0, catId = result.catId, species = it.species.name, exposureMs = it.exposureMs, nearTouches = it.nearTouches)
        }
        val id = stats.saveSession(session, catchRows, interactionRows)

        val after = stats(result.catId)
        val stored = achievements.all(result.catId).map { it.achievement }.toSet()
        val fresh = Achievement.unlocked(after).filter { it.name !in stored }
        achievements.insert(fresh.map { AchievementEntity(result.catId, it.name, endedAt) })
        return summaryOf(id) ?: error("session $id not found after insert")
    }

    suspend fun summaryOf(sessionId: Long): SessionSummary? {
        val s = stats.session(sessionId) ?: return null
        val perSpecies = stats.sessionSpecies(sessionId)
            .mapNotNull { row -> Species.byName(row.species)?.let { it to row.count } }
            .toMap()
        val endedAt = s.startedAt + s.durationMs
        val fresh = achievements.all(s.catId).filter { it.unlockedAt == endedAt }
            .mapNotNull { e -> Achievement.entries.firstOrNull { it.name == e.achievement } }
        return SessionSummary(
            sessionId = s.id,
            durationMs = s.durationMs,
            location = Location.byName(s.location),
            night = s.night,
            mode = GameMode.entries.firstOrNull { it.name == s.mode } ?: GameMode.HUNT,
            catches = s.catches,
            misses = s.misses,
            perSpecies = perSpecies,
            stars = s.stars,
            rare = perSpecies.keys.filter { it.rare },
            newAchievements = fresh,
        )
    }

    suspend fun resetAll() = stats.resetAll()
}

fun interface ZoneProvider {
    fun zone(): ZoneId
}

object StatsCalculator {
    fun compute(sessions: List<SessionEntity>, catches: List<CatchRow>, zone: ZoneId): CatStats {
        val perSpecies = HashMap<Species, Int>()
        var rare = 0
        var night = 0
        var feather = 0
        var laser = 0
        for (c in catches) {
            val sp = Species.byName(c.species) ?: continue
            perSpecies[sp] = (perSpecies[sp] ?: 0) + 1
            if (c.rare) rare++
            if (c.night) night++
            when (c.mode) {
                GameMode.FEATHER.name -> feather++
                GameMode.LASER.name -> laser++
            }
        }
        return CatStats(
            totalCatches = catches.size,
            totalMisses = sessions.sumOf { it.misses },
            perSpecies = perSpecies,
            rareCatches = rare,
            bestStreak = sessions.maxOfOrNull { it.bestStreak } ?: 0,
            bestSessionCatches = sessions.maxOfOrNull { it.catches } ?: 0,
            nightCatches = night,
            featherCatches = feather,
            laserCatches = laser,
            sessions = sessions.size,
            totalPlayMs = sessions.sumOf { it.durationMs },
            playDays = sessions.mapTo(HashSet()) { epochDay(it.startedAt, zone) },
            locationsPlayed = sessions.filter { it.mode == GameMode.HUNT.name }.mapTo(HashSet()) { Location.byName(it.location) },
        )
    }

    fun daily(sessions: List<SessionEntity>, catches: List<CatchRow>, zone: ZoneId): List<DailyPoint> {
        val catchesByDay = catches.groupingBy { epochDay(it.timestamp, zone) }.eachCount()
        val playByDay = sessions.groupBy { epochDay(it.startedAt, zone) }.mapValues { (_, v) -> v.sumOf { it.durationMs } }
        return (catchesByDay.keys + playByDay.keys).toSortedSet().map {
            DailyPoint(it, catchesByDay[it] ?: 0, playByDay[it] ?: 0L)
        }
    }

    fun epochDay(millis: Long, zone: ZoneId): Long = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()
}
