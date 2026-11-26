package com.miguelaetxio.mia2f.data.backup

import android.content.Context
import androidx.activity.result.IntentSenderRequest
import com.miguelaetxio.mia2f.data.log.AppLog
import com.miguelaetxio.mia2f.data.log.DebugLogFile
import com.miguelaetxio.mia2f.data.log.LogArea
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

    private val _isRestoreOfferDismissed = MutableStateFlow(
        prefs.getBoolean(KEY_LINKED, false) ||
            prefs.getBoolean(KEY_OFFER_DISMISSED, false)
    )

    /**
     * True once the "already used miA2F on another phone?" offer is no
     * longer needed: answered, or this phone already linked to Drive.
     * ---
     * True cuando ya no hace falta la oferta «¿ya usabas miA2F en otro
     * móvil?»: contestada, o este móvil ya enlazado con Drive.
     */
    val isRestoreOfferDismissed: StateFlow<Boolean> =
        _isRestoreOfferDismissed.asStateFlow()

    /**
     * The student says this phone starts from scratch.
     * ---
     * El alumno dice que este móvil empieza de cero.
     */
    fun dismissRestoreOffer() {
        prefs.edit().putBoolean(KEY_OFFER_DISMISSED, true).apply()
        _isRestoreOfferDismissed.value = true
    }

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
                AppLog.log(
                    LogArea.DRIVE,
                    "Copia no subida: Drive ya tiene una de otro móvil " +
                        "(${existing.id}, ${existing.modifiedTime})",
                )
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
            _isRestoreOfferDismissed.value = true
            AppLog.log(
                LogArea.DRIVE,
                "Copia subida a $fileId (${json.length} car., " +
                    "force=$force, existía=${existing != null})",
            )
            DriveOutcome.Success(now)
        }

    /**
     * Downloads and checks the copy; Success(null) if there is none.
     * ---
     * Descarga y comprueba la copia; Success(null) si no hay ninguna.
     */
    suspend fun download(): DriveOutcome<DriveCopy?> = withToken { bearer ->
        val file = findCopy(bearer)
        if (file == null) {
            AppLog.log(LogArea.DRIVE, "Restaurar: no hay copia en Drive")
            return@withToken DriveOutcome.Success(null)
        }
        val json = driveApiService.download(bearer, file.id).string()
        AppLog.log(
            LogArea.DRIVE,
            "Restaurar: copia ${file.id} (${file.modifiedTime}) " +
                "descargada, ${json.length} car.",
        )
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
        AppLog.log(
            LogArea.DRIVE,
            "Progreso restaurado: ${copy.backup.sessions.size} clases, " +
                "${copy.backup.exercises.size} ejercicios, " +
                "${copy.backup.errors.size} errores",
        )
        prefs.edit()
            .putBoolean(KEY_LINKED, true)
            .putBoolean(KEY_PENDING, false)
            .apply()
        _isPending.value = false
        _isRestoreOfferDismissed.value = true
    }

    /**
     * Uploads the diagnostic logs to the "miA2F - Intercambio Claude"
     * folder, overwriting each file in place (the folder always holds
     * just the last upload). Files go in the given order: the manifest
     * last. Returns how many files were uploaded.
     * ---
     * Sube los logs de diagnóstico a la carpeta «miA2F - Intercambio
     * Claude», sobrescribiendo cada archivo en su sitio (la carpeta
     * guarda siempre solo la última subida). Los archivos van en el
     * orden dado: el manifiesto el último. Devuelve cuántos subió.
     */
    suspend fun uploadDebugLogs(
        files: List<DebugLogFile>,
    ): DriveOutcome<Int> = withToken { bearer ->
        val folderId = exchangeFolder(bearer)
        files.forEach { file ->
            val fileId = driveApiService.listFiles(
                bearer,
                "name = '${file.name}' and '$folderId' in parents " +
                    "and trashed = false",
            ).files?.firstOrNull()?.id ?: driveApiService.createFile(
                bearer,
                DriveFileCreate(
                    name = file.name,
                    mimeType = TEXT,
                    parents = listOf(folderId),
                ),
            ).id
            driveUploadApiService.uploadContent(
                bearer, fileId, file.content.toRequestBody(TEXT.toMediaType()),
            )
        }
        AppLog.log(
            LogArea.DRIVE,
            "Logs subidos a $EXCHANGE_FOLDER: ${files.size} archivos",
        )
        DriveOutcome.Success(files.size)
    }

    private suspend fun exchangeFolder(bearer: String): String =
        driveApiService.listFiles(
            bearer,
            "mimeType = '$FOLDER' and name = '$EXCHANGE_FOLDER' " +
                "and trashed = false",
        ).files?.firstOrNull()?.id ?: driveApiService.createFile(
            bearer, DriveFileCreate(name = EXCHANGE_FOLDER, mimeType = FOLDER),
        ).id

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
                AppLog.log(
                    LogArea.DRIVE,
                    "Sin conexión con Drive — " +
                        "${e::class.java.simpleName}: ${e.message}",
                )
                DriveOutcome.Offline
            } catch (e: HttpException) {
                val body = runCatching {
                    e.response()?.errorBody()?.string()
                }.getOrNull()
                AppLog.log(
                    LogArea.DRIVE,
                    "Drive HTTP ${e.code()} — ${body?.take(1000)}",
                )
                DriveOutcome.Failed(
                    "Drive ha respondido con error ${e.code()}."
                )
            }
        }

    private companion object {
        const val FILE_NAME = "mia2f_progreso.json"
        const val JSON = "application/json"
        const val TEXT = "text/plain"
        const val FOLDER = "application/vnd.google-apps.folder"

        // Misma convención que MiMoo (com-actions-relay §7).
        // ---
        // Same convention as MiMoo (com-actions-relay §7).
        const val EXCHANGE_FOLDER = "miA2F - Intercambio Claude"
        const val PREFS_NAME = "drive_backup"
        const val KEY_LAST_BACKUP_AT = "last_backup_at"
        const val KEY_PENDING = "pending"

        // Este móvil ya escribió o restauró la copia de Drive.
        // ---
        // This phone already wrote or restored the Drive copy.
        const val KEY_LINKED = "linked"
        const val KEY_OFFER_DISMISSED = "restore_offer_dismissed"
    }
}
