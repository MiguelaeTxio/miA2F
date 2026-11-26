package com.miguelaetxio.mia2f.data.update

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.miguelaetxio.mia2f.data.remote.AppUpdateApiService
import com.miguelaetxio.mia2f.data.remote.dto.UpdateManifest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks miA2FReleases for a newer version, downloads the APK to
 * `cacheDir/apk_updates/` and returns the `content://` Uri the system
 * installer needs. Same pattern as MiMoo (H07): the app is not on
 * Google Play, so it installs through the "unknown sources" flow.
 * ---
 * Comprueba en miA2FReleases si hay una versión más reciente,
 * descarga el APK a `cacheDir/apk_updates/` y devuelve el Uri
 * `content://` que necesita el instalador del sistema. Mismo patrón
 * que MiMoo (H07): la app no está en Google Play, así que se instala
 * por el flujo de «fuentes desconocidas».
 */
@Singleton
class AppUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUpdateApiService: AppUpdateApiService,
) {

    /**
     * Compares the installed versionCode against the published
     * manifest. Never throws.
     * ---
     * Compara el versionCode instalado con el manifiesto publicado.
     * Nunca lanza excepciones.
     */
    suspend fun checkForUpdate(currentVersionCode: Int): UpdateCheckResult =
        try {
            val manifest = appUpdateApiService.getLatestManifest()
            if (manifest.versionCode > currentVersionCode) {
                UpdateCheckResult.UpdateAvailable(manifest)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                e.message ?: "No se pudo comprobar si hay actualizaciones."
            )
        }

    /**
     * Returns the Uri of an already downloaded APK for this manifest,
     * or null if it is missing, unreadable or of another version.
     * ---
     * Devuelve el Uri de un APK ya descargado para este manifiesto, o
     * null si no existe, no se puede leer o es de otra versión.
     */
    suspend fun downloadedApkUri(manifest: UpdateManifest): Uri? =
        withContext(Dispatchers.IO) {
            val apkFile = apkFileFor(manifest)
            if (!apkFile.isFile || apkFile.length() == 0L) {
                return@withContext null
            }
            val info = try {
                context.packageManager
                    .getPackageArchiveInfo(apkFile.absolutePath, 0)
            } catch (e: Exception) {
                null
            }
            val code = info?.let {
                if (Build.VERSION.SDK_INT >= 28) {
                    it.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    it.versionCode.toLong()
                }
            }
            if (code != manifest.versionCode.toLong()) {
                apkFile.delete()
                return@withContext null
            }
            uriFor(apkFile)
        }

    /**
     * Downloads the APK to a `.part` file and renames it only when
     * complete, so a cut download is never mistaken for a full one.
     * `onProgress` receives (downloaded, total); total is -1 when the
     * server sends no Content-Length.
     * ---
     * Descarga el APK a un archivo `.part` y solo lo renombra al
     * terminar, para que una descarga cortada nunca pase por completa.
     * `onProgress` recibe (descargados, total); total es -1 si el
     * servidor no envía Content-Length.
     */
    suspend fun downloadApk(
        manifest: UpdateManifest,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Uri = withContext(Dispatchers.IO) {
        downloadedApkUri(manifest)?.let { return@withContext it }
        val apkFile = apkFileFor(manifest)
        val partFile = File(apkFile.parentFile, apkFile.name + ".part")
        apkFile.parentFile?.listFiles()?.forEach {
            if (it != apkFile && it != partFile) it.delete()
        }

        val body = appUpdateApiService.downloadApk(manifest.apkUrl)
        val totalBytes = body.contentLength()
        var bytesDownloaded = 0L

        body.byteStream().use { input ->
            partFile.outputStream().use { output ->
                val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead
                    onProgress(bytesDownloaded, totalBytes)
                }
            }
        }

        if (totalBytes > 0 && bytesDownloaded != totalBytes) {
            partFile.delete()
            throw IOException(
                "Descarga incompleta ($bytesDownloaded de $totalBytes bytes)."
            )
        }
        apkFile.delete()
        if (!partFile.renameTo(apkFile)) {
            throw IOException("No se pudo guardar la actualización.")
        }
        uriFor(apkFile)
    }

    private fun apkFileFor(manifest: UpdateManifest): File =
        File(
            File(context.cacheDir, "apk_updates").apply { mkdirs() },
            "miA2F-update-${manifest.versionCode}.apk",
        )

    private fun uriFor(apkFile: File): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
}

private const val DOWNLOAD_BUFFER_SIZE = 8 * 1024

/**
 * Result of an update check.
 * ---
 * Resultado de una comprobación de actualización.
 */
sealed class UpdateCheckResult {
    object UpToDate : UpdateCheckResult()
    data class UpdateAvailable(val manifest: UpdateManifest) :
        UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}
