package com.miguelaetxio.mia2f.data.log

import android.os.Build
import com.miguelaetxio.mia2f.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * One log file ready to upload.
 * ---
 * Un archivo de log listo para subir.
 */
data class DebugLogFile(val name: String, val content: String)

/**
 * Gathers the log files of every LogArea (a closed list, never "every
 * file in the folder") and builds the manifest, which goes last so a
 * half-done upload is visible: its "Subida:" time stays old.
 * ---
 * Reúne los archivos de log de cada LogArea (lista cerrada, nunca
 * «todos los archivos de la carpeta») y prepara el manifiesto, que va
 * el último para que una subida a medias se note: su hora «Subida:»
 * se queda vieja.
 */
class DebugLogCollector @Inject constructor() {

    /**
     * Log files found, followed by the manifest.
     * ---
     * Archivos de log encontrados, seguidos del manifiesto.
     */
    suspend fun collect(): List<DebugLogFile> = withContext(Dispatchers.IO) {
        val folder = AppLog.directory()
        val found = mutableListOf<DebugLogFile>()
        val missing = mutableListOf<String>()
        LogArea.entries.forEach { area ->
            val content = folder?.let { read(File(it, area.fileName)) }
            if (content != null) {
                found.add(DebugLogFile(area.fileName, content))
            } else {
                missing.add(area.fileName)
            }
        }
        found + DebugLogFile(MANIFEST_FILE_NAME, manifest(found, missing))
    }

    private fun read(file: File): String? =
        runCatching { if (file.isFile) file.readText() else null }
            .getOrNull()

    private fun manifest(
        found: List<DebugLogFile>,
        missing: List<String>,
    ): String {
        val stamp = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss Z", Locale.getDefault(),
        ).format(Date())
        return buildString {
            appendLine("Subida: $stamp")
            appendLine(
                "Versión: ${BuildConfig.VERSION_NAME} " +
                    "(${BuildConfig.VERSION_CODE})"
            )
            appendLine(
                "Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} " +
                    "(Android ${Build.VERSION.RELEASE})"
            )
            appendLine()
            appendLine("Archivos subidos (${found.size}):")
            found.forEach {
                val bytes = it.content.toByteArray().size
                appendLine("  ${it.name}  $bytes bytes")
            }
            appendLine()
            appendLine("No encontrados en el dispositivo (${missing.size}):")
            missing.forEach { appendLine("  $it") }
        }
    }

    companion object {
        const val MANIFEST_FILE_NAME = "mia2f_logs_manifest.txt"
    }
}
