package com.miguelaetxio.mia2f.data.log

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostic log files, one per area, in the app's private storage.
 * They only leave the phone when the student presses "Subir logs a
 * Drive" in Settings. Never log the Gemini key or any token.
 * ---
 * Archivos de log de diagnóstico, uno por área, en el almacenamiento
 * privado de la app. Solo salen del móvil cuando el alumno pulsa
 * «Subir logs a Drive» en Ajustes. Nunca se registra la clave de
 * Gemini ni ningún token.
 */
enum class LogArea(val fileName: String) {
    /** Uncaught exceptions. / Excepciones no capturadas. */
    CRASH("crash_log.txt"),

    /** Start, course and updates. / Arranque, temario y actualizaciones. */
    APP("app_debug.txt"),

    /** Every Gemini call. / Cada llamada a Gemini. */
    GEMINI("gemini_debug.txt"),

    /** Classes, tests, reviews, practices. / Clases, tests, repasos. */
    STUDY("study_debug.txt"),

    /** Voice and recording. / Voz y grabación. */
    SPEECH("speech_debug.txt"),

    /** Drive copy, restore and log upload. / Copia, restauración, logs. */
    DRIVE("drive_debug.txt"),
}

/**
 * Writes timestamped lines to the area's file. A file over MAX_BYTES
 * keeps only its last KEEP_LINES lines (same as MiMoo). A failure
 * writing the log never affects the app.
 * ---
 * Escribe líneas con fecha y hora en el archivo del área. Un archivo
 * de más de MAX_BYTES se queda con sus últimas KEEP_LINES líneas
 * (como en MiMoo). Un fallo escribiendo el log nunca afecta a la app.
 */
object AppLog {

    private const val DIRECTORY = "logs"
    private const val LOGCAT_TAG = "miA2F"
    private const val MAX_BYTES = 200_000L
    private const val KEEP_LINES = 400
    private val lock = Any()

    @Volatile
    private var directory: File? = null

    /**
     * Called once from Application.onCreate: sets the folder and
     * records uncaught exceptions before the app closes.
     * ---
     * Se llama una vez desde Application.onCreate: fija la carpeta y
     * registra las excepciones no capturadas antes de que la app se
     * cierre.
     */
    fun init(context: Context) {
        directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            log(
                LogArea.CRASH,
                "Cierre inesperado en el hilo ${thread.name}:\n" +
                    stackTrace(error),
            )
            previous?.uncaughtException(thread, error)
        }
    }

    /**
     * Folder holding the log files, or null before init().
     * ---
     * Carpeta de los archivos de log, o null antes de init().
     */
    fun directory(): File? = directory

    /**
     * Appends a line to the area's file.
     * ---
     * Añade una línea al archivo del área.
     */
    fun log(area: LogArea, line: String) {
        Log.d(LOGCAT_TAG, "[${area.name}] $line")
        val folder = directory ?: return
        try {
            synchronized(lock) {
                val file = File(folder, area.fileName)
                if (file.exists() && file.length() > MAX_BYTES) {
                    file.writeText(
                        file.readLines().takeLast(KEEP_LINES)
                            .joinToString("\n", postfix = "\n")
                    )
                }
                val stamp = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault(),
                ).format(Date())
                file.appendText("$stamp  $line\n")
            }
        } catch (e: Exception) {
            Log.w(LOGCAT_TAG, "No se pudo escribir el log", e)
        }
    }

    /**
     * Line plus the exception's class, message and stack trace.
     * ---
     * Línea más la clase, el mensaje y la traza de la excepción.
     */
    fun error(area: LogArea, line: String, error: Throwable) {
        log(area, "$line\n${stackTrace(error)}")
    }

    private fun stackTrace(error: Throwable): String {
        val writer = StringWriter()
        error.printStackTrace(PrintWriter(writer))
        return writer.toString().trimEnd()
    }
}
