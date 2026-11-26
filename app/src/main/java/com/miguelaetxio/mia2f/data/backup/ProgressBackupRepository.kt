package com.miguelaetxio.mia2f.data.backup

import android.content.Context
import androidx.activity.result.IntentSenderRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a Drive operation, with a ready-to-show message on error.
 * ---
 * Resultado de una operación con Drive, con el mensaje listo para
 * mostrar si hay error.
 */
sealed class DriveOutcome<out T> {
    data class Success<T>(val value: T) : DriveOutcome<T>()

    data class NeedsConsent(
        val request: IntentSenderRequest,
    ) : DriveOutcome<Nothing>()

    data object Offline : DriveOutcome<Nothing>()

    /**
     * Drive already holds a copy this phone never restored or wrote:
     * overwriting it could wipe the progress made on another phone.
     * ---
     * Drive ya tiene una copia que este móvil nunca restauró ni
     * escribió: sobrescribirla podría borrar el progreso de otro móvil.
     */
    data object ForeignCopy : DriveOutcome<Nothing>()

    data class Failed(val message: String) : DriveOutcome<Nothing>()
}

/**
 * A copy found in Drive, already read and checked.
 * ---
 * Una copia encontrada en Drive, ya leída y comprobada.
 */
data class DriveCopy(val backup: ProgressBackup)

/**
 * The student's single copy in their own Drive, mia2f_progreso.json:
 * created the first time and overwritten afterwards. With drive.file
 * the app only sees the files it created itself.
 * ---
 * La única copia del alumno en su propio Drive, mia2f_progreso.json:
 * se crea la primera vez y después se sobrescribe. Con drive.file la
 * app solo ve los archivos que creó ella.
 */
@Singleton
class ProgressBackupRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val authorizationHelper: DriveAuthorizationHelper,
    private val driveApiService: DriveApiService,
    private val driveUploadApiService: DriveUploadApiService,
    private val serializer: ProgressBackupSerializer,
) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _lastBackupAt = MutableStateFlow(
        prefs.getLong(KEY_LAST_BACKUP_AT, 0L).takeIf { it > 0L }
    )

    /**
     * Time of the last successful copy, or null if there is none.
     * ---
     * Hora de la última copia buena, o null si no hay ninguna.
     */
    val lastBackupAt: StateFlow<Long?> = _lastBackupAt.asStateFlow()

    private val _isPending =
        MutableStateFlow(prefs.getBoolean(KEY_PENDING, false))

    /**
     * True while there is progress not yet copied to Drive.
     * ---
     * True mientras haya progreso sin copiar en Drive.
     */
    val isPending: StateFlow<Boolean> = _isPending.asStateFlow()

    /**
     * Records that the progress changed and needs a new copy.
     * ---
     * Anota que el progreso ha cambiado y necesita una copia nueva.
     */
    fun markPending() {
        prefs.edit().putBoolean(KEY_PENDING, true).apply()
        _isPending.value = true
    }

    /**
     * Uploads the current progress, overwriting the previous copy.
     * Without force, a copy this phone never restored or wrote is not
     * overwritten (ForeignCopy).
     * ---
     * Sube el progreso actual, sobrescribiendo la copia anterior. Sin
     * force, no se sobrescribe una copia que este móvil nunca restauró
     * ni escribió (ForeignCopy).
     */
    suspend fun upload(force: Boolean = false): DriveOutcome<Long> =
        withToken { bearer ->
            val existing = findCopy(bearer)
            if (existing != null && !force &&
                !prefs.getBoolean(KEY_LINKED, false)
            ) {
                return@withToken DriveOutcome.ForeignCopy
            }
            val json = serializer.export()
            val fileId = existing?.id ?: driveApiService.createFile(
                bearer, DriveFileCreate(name = FILE_NAME, mimeType = JSON),
            ).id
            driveUploadApiService.uploadContent(
                bearer, fileId, json.toRequestBody(JSON.toMediaType()),
            )
            val now = System.currentTimeMillis()
            prefs.edit()
                .putLong(KEY_LAST_BACKUP_AT, now)
                .putBoolean(KEY_PENDING, false)
                .putBoolean(KEY_LINKED, true)
                .apply()
            _lastBackupAt.value = now
            _isPending.value = false
            DriveOutcome.Success(now)
        }

    /**
     * Downloads and checks the copy; Success(null) if there is none.
     * ---
     * Descarga y comprueba la copia; Success(null) si no hay ninguna.
     */
    suspend fun download(): DriveOutcome<DriveCopy?> = withToken { bearer ->
        val file = findCopy(bearer)
            ?: return@withToken DriveOutcome.Success(null)
        val json = driveApiService.download(bearer, file.id).string()
        when (val read = serializer.read(json)) {
            is BackupReadResult.Valid ->
                DriveOutcome.Success(DriveCopy(read.backup))
            is BackupReadResult.Invalid -> DriveOutcome.Failed(read.message)
        }
    }

    /**
     * Replaces the local progress with a copy already downloaded.
     * ---
     * Sustituye el progreso local por una copia ya descargada.
     */
    suspend fun restore(copy: DriveCopy) {
        serializer.restore(copy.backup)
        prefs.edit()
            .putBoolean(KEY_LINKED, true)
            .putBoolean(KEY_PENDING, false)
            .apply()
        _isPending.value = false
    }

    private suspend fun findCopy(bearer: String): DriveFile? =
        driveApiService.listFiles(
            bearer, "name = '$FILE_NAME' and trashed = false",
        ).files?.firstOrNull()

    private suspend fun <T> withToken(
        block: suspend (String) -> DriveOutcome<T>,
    ): DriveOutcome<T> =
        when (val auth = authorizationHelper.authorize()) {
            is DriveAuthorization.NeedsUserConsent ->
                DriveOutcome.NeedsConsent(auth.request)
            is DriveAuthorization.Failed -> DriveOutcome.Failed(auth.message)
            is DriveAuthorization.Authorized -> try {
                block("Bearer ${auth.accessToken}")
            } catch (e: IOException) {
                DriveOutcome.Offline
            } catch (e: HttpException) {
                DriveOutcome.Failed(
                    "Drive ha respondido con error ${e.code()}."
                )
            }
        }

    private companion object {
        const val FILE_NAME = "mia2f_progreso.json"
        const val JSON = "application/json"
        const val PREFS_NAME = "drive_backup"
        const val KEY_LAST_BACKUP_AT = "last_backup_at"
        const val KEY_PENDING = "pending"

        // Este móvil ya escribió o restauró la copia de Drive.
        // ---
        // This phone already wrote or restored the Drive copy.
        const val KEY_LINKED = "linked"
    }
}
