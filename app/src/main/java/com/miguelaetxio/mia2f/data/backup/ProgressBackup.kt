package com.miguelaetxio.mia2f.data.backup

import androidx.room.withTransaction
import com.google.gson.Gson
import com.miguelaetxio.mia2f.data.profile.ProfileRepository
import com.miguelaetxio.mia2f.data.study.ConceptError
import com.miguelaetxio.mia2f.data.study.ConceptErrorDao
import com.miguelaetxio.mia2f.data.study.Exercise
import com.miguelaetxio.mia2f.data.study.ExerciseDao
import com.miguelaetxio.mia2f.data.study.StudyDatabase
import com.miguelaetxio.mia2f.data.study.StudySession
import com.miguelaetxio.mia2f.data.study.StudySessionDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything the Drive copy holds: the student name and every row of
 * the class record, with their ids. Never the Gemini key.
 * ---
 * Todo lo que guarda la copia de Drive: el nombre del alumno y todas
 * las filas del registro de clases, con sus id. Nunca la clave de
 * Gemini.
 */
data class ProgressBackup(
    val formatVersion: Int,
    val databaseVersion: Int,
    val exportedAt: Long,
    val studentName: String?,
    val sessions: List<StudySession>,
    val exercises: List<Exercise>,
    val errors: List<ConceptError>,
)

/**
 * Result of reading a copy downloaded from Drive.
 * ---
 * Resultado de leer una copia descargada de Drive.
 */
sealed class BackupReadResult {
    data class Valid(val backup: ProgressBackup) : BackupReadResult()
    data class Invalid(val message: String) : BackupReadResult()
}

/**
 * Turns the local progress into a copy and back. Restoring replaces
 * the whole record in one transaction and keeps the ids, because the
 * H04 criterion orders classes by id.
 * ---
 * Convierte el progreso local en una copia y al revés. Restaurar
 * sustituye todo el registro en una transacción y conserva los id,
 * porque el criterio de H04 ordena las clases por id.
 */
@Singleton
class ProgressBackupSerializer @Inject constructor(
    private val database: StudyDatabase,
    private val studySessionDao: StudySessionDao,
    private val exerciseDao: ExerciseDao,
    private val conceptErrorDao: ConceptErrorDao,
    private val profileRepository: ProfileRepository,
) {

    private val gson = Gson()

    /**
     * Current progress as backup JSON.
     * ---
     * El progreso actual como JSON de copia.
     */
    suspend fun export(): String {
        val backup = database.withTransaction {
            ProgressBackup(
                formatVersion = FORMAT_VERSION,
                databaseVersion = StudyDatabase.VERSION,
                exportedAt = System.currentTimeMillis(),
                studentName = profileRepository.studentName.value,
                sessions = studySessionDao.all(),
                exercises = exerciseDao.all(),
                errors = conceptErrorDao.all(),
            )
        }
        return gson.toJson(backup)
    }

    /**
     * Reads and checks a backup JSON. A copy made by a newer app is
     * rejected rather than half-read.
     * ---
     * Lee y comprueba un JSON de copia. Una copia hecha por una app
     * más nueva se rechaza en vez de leerla a medias.
     */
    fun read(json: String): BackupReadResult {
        val backup = runCatching {
            gson.fromJson(json, ProgressBackup::class.java)
        }.getOrNull()
        // Gson no respeta la nulabilidad de Kotlin: se comprueba aquí.
        // ---
        // Gson ignores Kotlin nullability: it is checked here.
        @Suppress("SENSELESS_COMPARISON")
        val complete = backup != null && backup.sessions != null &&
            backup.exercises != null && backup.errors != null &&
            backup.sessions.none { it.unitId == null || it.kind == null } &&
            backup.exercises.none {
                it.conceptId == null || it.prompt == null
            } &&
            backup.errors.none {
                it.conceptId == null || it.description == null
            }
        if (backup == null || !complete) {
            return BackupReadResult.Invalid(
                "La copia de Drive no se puede leer."
            )
        }
        if (backup.formatVersion > FORMAT_VERSION ||
            backup.databaseVersion > StudyDatabase.VERSION
        ) {
            return BackupReadResult.Invalid(
                "La copia la hizo una versión más nueva de miA2F. " +
                    "Actualiza la app y vuelve a intentarlo."
            )
        }
        return BackupReadResult.Valid(backup)
    }

    /**
     * Replaces the local progress with the backup.
     * ---
     * Sustituye el progreso local por el de la copia.
     */
    suspend fun restore(backup: ProgressBackup) {
        database.withTransaction {
            conceptErrorDao.deleteAll()
            exerciseDao.deleteAll()
            studySessionDao.deleteAll()
            studySessionDao.insertAll(backup.sessions)
            exerciseDao.insertAll(backup.exercises)
            conceptErrorDao.insertAll(backup.errors)
        }
        backup.studentName?.let { profileRepository.saveStudentName(it) }
    }

    private companion object {
        const val FORMAT_VERSION = 1
    }
}
