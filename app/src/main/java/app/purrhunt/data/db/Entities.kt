package app.purrhunt.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cats")
data class CatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val photoPath: String? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(CatEntity::class, ["id"], ["catId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("catId")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catId: Long,
    val startedAt: Long,
    val durationMs: Long,
    val location: String,
    val night: Boolean,
    val mode: String,
    val catches: Int,
    val misses: Int,
    val bestStreak: Int,
    val stars: Int,
    /** Touch heatmap encoded by [app.purrhunt.domain.HeatmapGrid]. */
    val heatmap: String,
)

@Entity(
    tableName = "catches",
    foreignKeys = [
        ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CatEntity::class, ["id"], ["catId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionId"), Index("catId")],
)
data class CatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val catId: Long,
    val species: String,
    val timestamp: Long,
    val rare: Boolean,
    val night: Boolean,
    val mode: String,
)

@Entity(
    tableName = "interactions",
    foreignKeys = [
        ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CatEntity::class, ["id"], ["catId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionId"), Index("catId")],
)
data class InteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val catId: Long,
    val species: String,
    val exposureMs: Long,
    val nearTouches: Int,
)

@Entity(
    tableName = "achievements",
    primaryKeys = ["catId", "achievement"],
    foreignKeys = [ForeignKey(CatEntity::class, ["id"], ["catId"], onDelete = ForeignKey.CASCADE)],
)
data class AchievementEntity(
    val catId: Long,
    val achievement: String,
    val unlockedAt: Long,
)

@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(CatEntity::class, ["id"], ["catId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("catId")],
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catId: Long,
    val path: String,
    val takenAt: Long,
    val species: String,
)

/** Per-species aggregate for the bestiary. */
data class SpeciesCount(val species: String, val count: Int)

/** Catches per calendar day for charts. */
data class DayCount(val day: Long, val count: Int)

data class DayDuration(val day: Long, val durationMs: Long)
