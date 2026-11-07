package app.purrhunt.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CatDao {
    @Query("SELECT * FROM cats ORDER BY createdAt")
    fun observeAll(): Flow<List<CatEntity>>

    @Query("SELECT * FROM cats WHERE id = :id")
    fun observe(id: Long): Flow<CatEntity?>

    @Query("SELECT * FROM cats WHERE id = :id")
    suspend fun get(id: Long): CatEntity?

    @Query("SELECT COUNT(*) FROM cats")
    suspend fun count(): Int

    @Insert
    suspend fun insert(cat: CatEntity): Long

    @Update
    suspend fun update(cat: CatEntity)

    @Delete
    suspend fun delete(cat: CatEntity)
}

/** A catch row reduced to what statistics need. */
data class CatchRow(val species: String, val timestamp: Long, val rare: Boolean, val night: Boolean, val mode: String)

data class SessionSpeciesBest(val species: String, val best: Int)

@Dao
interface StatsDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Insert
    suspend fun insertCatches(catches: List<CatchEntity>)

    @Insert
    suspend fun insertInteractions(items: List<InteractionEntity>)

    @Transaction
    suspend fun saveSession(session: SessionEntity, catches: List<CatchEntity>, interactions: List<InteractionEntity>): Long {
        val id = insertSession(session)
        insertCatches(catches.map { it.copy(sessionId = id) })
        insertInteractions(interactions.map { it.copy(sessionId = id) })
        return id
    }

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun session(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE catId = :catId ORDER BY startedAt")
    fun observeSessions(catId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE catId = :catId ORDER BY startedAt")
    suspend fun sessions(catId: Long): List<SessionEntity>

    @Query("SELECT species, timestamp, rare, night, mode FROM catches WHERE catId = :catId")
    fun observeCatches(catId: Long): Flow<List<CatchRow>>

    @Query("SELECT species, timestamp, rare, night, mode FROM catches WHERE catId = :catId")
    suspend fun catches(catId: Long): List<CatchRow>

    @Query("SELECT species, COUNT(*) AS count FROM catches WHERE sessionId = :sessionId GROUP BY species")
    suspend fun sessionSpecies(sessionId: Long): List<SpeciesCount>

    @Query(
        "SELECT species, MAX(c) AS best FROM " +
            "(SELECT species, sessionId, COUNT(*) AS c FROM catches WHERE catId = :catId GROUP BY species, sessionId) " +
            "GROUP BY species",
    )
    fun observeBestPerSpecies(catId: Long): Flow<List<SessionSpeciesBest>>

    @Query("SELECT * FROM interactions WHERE catId = :catId")
    fun observeInteractions(catId: Long): Flow<List<InteractionEntity>>

    @Query("SELECT * FROM interactions WHERE catId = :catId")
    suspend fun interactions(catId: Long): List<InteractionEntity>

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM achievements")
    suspend fun clearAchievements()

    @Transaction
    suspend fun resetAll() {
        clearSessions()
        clearAchievements()
    }
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE catId = :catId")
    fun observe(catId: Long): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE catId = :catId")
    suspend fun all(catId: Long): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(items: List<AchievementEntity>)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE catId = :catId ORDER BY takenAt DESC")
    fun observe(catId: Long): Flow<List<PhotoEntity>>

    @Insert
    suspend fun insert(photo: PhotoEntity): Long

    @Delete
    suspend fun delete(photo: PhotoEntity)
}
