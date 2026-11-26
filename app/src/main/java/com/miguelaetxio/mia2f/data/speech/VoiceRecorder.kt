package com.miguelaetxio.mia2f.data.speech

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.miguelaetxio.mia2f.data.log.AppLog
import com.miguelaetxio.mia2f.data.log.LogArea
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * A finished recording, ready to send to Gemini.
 * ---
 * Una grabación terminada, lista para enviar a Gemini.
 */
class Recording(val file: File) {
    val mimeType: String get() = MIME_TYPE

    /**
     * Deletes the audio: it is never kept once Gemini has answered.
     * ---
     * Borra el audio: nunca se guarda una vez que Gemini responde.
     */
    fun delete() {
        file.delete()
    }

    companion object {
        // AAC con cabeceras ADTS: formato admitido por Gemini.
        // ---
        // AAC with ADTS headers: a format Gemini accepts.
        const val MIME_TYPE = "audio/aac"
    }
}

/**
 * Records the student's voice to a temporary file in the app cache,
 * up to MAX_DURATION_MS. The RECORD_AUDIO permission must already be
 * granted. One recording at a time.
 * ---
 * Graba la voz del alumno en un archivo temporal de la caché de la
 * app, hasta MAX_DURATION_MS. El permiso RECORD_AUDIO ya tiene que
 * estar concedido. Una grabación cada vez.
 */
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var recorder: MediaRecorder? = null
    private var output: File? = null

    /**
     * Starts recording. onMaxDuration runs if the limit is reached; the
     * recorder stops by itself, and stop() must still be called.
     * Returns false if the microphone could not be started.
     * ---
     * Empieza a grabar. onMaxDuration se ejecuta si se llega al
     * límite; la grabadora se para sola, y aun así hay que llamar a
     * stop(). Devuelve false si no se pudo arrancar el micrófono.
     */
    fun start(onMaxDuration: () -> Unit): Boolean {
        discard()
        val file =
            File.createTempFile("pronunciation", ".aac", context.cacheDir)
        val created = newRecorder()
        return try {
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setMaxDuration(MAX_DURATION_MS)
            created.setOnInfoListener { _, what, _ ->
                if (what == MAX_DURATION_REACHED) onMaxDuration()
            }
            created.setOutputFile(file.absolutePath)
            created.prepare()
            created.start()
            recorder = created
            output = file
            true
        } catch (e: Exception) {
            AppLog.error(LogArea.SPEECH, "No se pudo empezar a grabar", e)
            created.release()
            file.delete()
            false
        }
    }

    /**
     * Stops and returns the recording, or null if nothing usable was
     * recorded (stop() fails when no audio arrived).
     * ---
     * Para y devuelve la grabación, o null si no se grabó nada útil
     * (stop() falla cuando no llegó audio).
     */
    fun stop(): Recording? {
        val active = recorder ?: return null
        val file = output
        recorder = null
        output = null
        val ok = try {
            active.stop()
            true
        } catch (e: RuntimeException) {
            false
        } finally {
            active.release()
        }
        if (!ok || file == null || file.length() == 0L) {
            AppLog.log(
                LogArea.SPEECH,
                "Grabación vacía o fallida (stop correcto: $ok)",
            )
            file?.delete()
            return null
        }
        AppLog.log(LogArea.SPEECH, "Grabación de ${file.length()} bytes")
        return Recording(file)
    }

    /**
     * Stops without keeping anything.
     * ---
     * Para sin guardar nada.
     */
    fun discard() {
        stop()?.delete()
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

    companion object {
        const val MAX_DURATION_MS = 15_000
        private const val MAX_DURATION_REACHED =
            MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
    }
}
