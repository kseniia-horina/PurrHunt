package app.purrhunt.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CatEntity::class,
        SessionEntity::class,
        CatchEntity::class,
        InteractionEntity::class,
        AchievementEntity::class,
        PhotoEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PurrDatabase : RoomDatabase() {
    abstract fun catDao(): CatDao
    abstract fun statsDao(): StatsDao
    abstract fun achievementDao(): AchievementDao
    abstract fun photoDao(): PhotoDao

    companion object {
        const val NAME = "purrhunt.db"
    }
}
