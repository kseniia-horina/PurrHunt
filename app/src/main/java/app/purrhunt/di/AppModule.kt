package app.purrhunt.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import app.purrhunt.data.Clock
import app.purrhunt.data.ZoneProvider
import app.purrhunt.data.db.AchievementDao
import app.purrhunt.data.db.CatDao
import app.purrhunt.data.db.PhotoDao
import app.purrhunt.data.db.PurrDatabase
import app.purrhunt.data.db.StatsDao
import app.purrhunt.game.engine.GameRandom
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): PurrDatabase =
        Room.databaseBuilder(context, PurrDatabase::class.java, PurrDatabase.NAME).build()

    @Provides fun catDao(db: PurrDatabase): CatDao = db.catDao()
    @Provides fun statsDao(db: PurrDatabase): StatsDao = db.statsDao()
    @Provides fun achievementDao(db: PurrDatabase): AchievementDao = db.achievementDao()
    @Provides fun photoDao(db: PurrDatabase): PhotoDao = db.photoDao()

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides fun clock(): Clock = Clock { System.currentTimeMillis() }
    @Provides fun zone(): ZoneProvider = ZoneProvider { ZoneId.systemDefault() }

    /** A fresh, time-seeded random source per game; tests construct their own seeded one. */
    @Provides fun random(): GameRandom = GameRandom(System.nanoTime())
}
