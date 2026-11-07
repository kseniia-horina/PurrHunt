package app.purrhunt.data

import app.purrhunt.data.db.CatDao
import app.purrhunt.data.db.CatEntity
import app.purrhunt.domain.CatProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatRepository @Inject constructor(
    private val dao: CatDao,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    val cats: Flow<List<CatProfile>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observe(id: Long): Flow<CatProfile?> = dao.observe(id).map { it?.toDomain() }

    suspend fun get(id: Long): CatProfile? = dao.get(id)?.toDomain()

    suspend fun save(profile: CatProfile): Long {
        val entity = CatEntity(
            id = profile.id,
            name = profile.name.trim(),
            colorArgb = profile.colorArgb,
            photoPath = profile.photoPath,
            createdAt = if (profile.id == 0L) clock.now() else profile.createdAt,
        )
        return if (profile.id == 0L) {
            val id = dao.insert(entity)
            if (dao.count() == 1) settings.setCurrentCat(id)
            id
        } else {
            dao.update(entity)
            profile.id
        }
    }

    suspend fun delete(profile: CatProfile) {
        dao.get(profile.id)?.let { dao.delete(it) }
    }

    private fun CatEntity.toDomain() = CatProfile(id, name, colorArgb, photoPath, createdAt)
}

/** Time source, injectable for tests. */
fun interface Clock {
    fun now(): Long
}
