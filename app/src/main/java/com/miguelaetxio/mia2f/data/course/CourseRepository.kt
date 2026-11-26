package com.miguelaetxio.mia2f.data.course

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the student's course on disk and refreshes it from miCursos
 * when the published version is newer. Works offline with the cached
 * copy. For now every student follows the only course (fr-a2).
 * ---
 * Guarda en disco el curso del alumno y lo renueva desde miCursos
 * cuando la versión publicada es más nueva. Funciona sin conexión con
 * la copia guardada. De momento todos siguen el único curso (fr-a2).
 */
@Singleton
class CourseRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val courseApiService: CourseApiService,
) {

    private val gson = Gson()
    private val cacheFile =
        File(File(context.filesDir, "courses").apply { mkdirs() },
            "$DEFAULT_COURSE_ID.json")

    private val _course = MutableStateFlow(readCache())

    /**
     * Cached course, or null if it has never been downloaded.
     * ---
     * Curso guardado, o null si nunca se ha descargado.
     */
    val course: StateFlow<Course?> = _course.asStateFlow()

    /**
     * Downloads the course if the published version is newer than the
     * cached one. Never throws; returns a Spanish message on failure.
     * ---
     * Descarga el curso si la versión publicada es más nueva que la
     * guardada. Nunca lanza; devuelve un mensaje en castellano si falla.
     */
    suspend fun refresh(): RefreshResult = withContext(Dispatchers.IO) {
        try {
            val entry = courseApiService.getIndex().courses
                .firstOrNull { it.id == DEFAULT_COURSE_ID }
                ?: return@withContext RefreshResult.Error(
                    "El curso $DEFAULT_COURSE_ID no está en miCursos."
                )
            val cached = _course.value
            if (cached != null && cached.version >= entry.version) {
                return@withContext RefreshResult.UpToDate
            }
            val json = courseApiService.getCourseFile(entry.path).string()
            val downloaded = gson.fromJson(json, Course::class.java)
            val tmp = File(cacheFile.parentFile, cacheFile.name + ".part")
            tmp.writeText(json)
            if (!tmp.renameTo(cacheFile)) {
                cacheFile.writeText(json)
                tmp.delete()
            }
            _course.value = downloaded
            RefreshResult.Updated(downloaded.version)
        } catch (e: IOException) {
            // Sin red (incluye UnknownHostException): no es un error del
            // temario, se sigue con la copia guardada si la hay.
            // ---
            // No network (includes UnknownHostException): not a course
            // error, the cached copy is used if there is one.
            RefreshResult.Offline(_course.value?.version)
        } catch (e: Exception) {
            RefreshResult.Error(
                "No se pudo comprobar el temario: ${e.message}"
            )
        }
    }

    private fun readCache(): Course? =
        try {
            if (cacheFile.isFile) {
                gson.fromJson(cacheFile.readText(), Course::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

    private companion object {
        const val DEFAULT_COURSE_ID = "fr-a2"
    }
}

/**
 * Result of a course refresh.
 * ---
 * Resultado de renovar el curso.
 */
sealed class RefreshResult {
    object UpToDate : RefreshResult()
    data class Updated(val version: Int) : RefreshResult()

    /**
     * No network; cachedVersion is null if nothing was ever downloaded.
     * ---
     * Sin red; cachedVersion es null si nunca se descargó nada.
     */
    data class Offline(val cachedVersion: Int?) : RefreshResult()
    data class Error(val message: String) : RefreshResult()
}
