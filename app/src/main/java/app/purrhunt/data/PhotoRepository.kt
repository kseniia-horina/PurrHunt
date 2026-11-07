package app.purrhunt.data

import app.purrhunt.data.db.PhotoDao
import app.purrhunt.data.db.PhotoEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** The "best shots" album: photos taken by the front camera at the moment of a catch. */
@Singleton
class PhotoRepository @Inject constructor(private val dao: PhotoDao, private val clock: Clock) {
    fun observe(catId: Long): Flow<List<PhotoEntity>> = dao.observe(catId)

    suspend fun add(catId: Long, path: String, species: String) {
        dao.insert(PhotoEntity(catId = catId, path = path, takenAt = clock.now(), species = species))
    }

    suspend fun delete(photo: PhotoEntity) {
        File(photo.path).delete()
        dao.delete(photo)
    }
}
