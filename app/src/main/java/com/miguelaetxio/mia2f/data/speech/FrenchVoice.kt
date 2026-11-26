package com.miguelaetxio.mia2f.data.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject

/**
 * State of the French voice of the phone.
 * ---
 * Estado de la voz francesa del móvil.
 */
enum class VoiceStatus {
    /** Starting the speech engine. / Arrancando el motor de voz. */
    LOADING,

    /** French ready to speak. / Francés listo para hablar. */
    READY,

    /** French exists but its voice is not installed. / Falta la voz. */
    MISSING_DATA,

    /** No French or no speech engine. / Sin francés o sin motor. */
    UNAVAILABLE,
}

/**
 * The phone's French voice (fr-FR) for model sentences. One instance
 * per screen: start() when it opens, shutdown() when it closes.
 * ---
 * La voz francesa del móvil (fr-FR) para las frases modelo. Una
 * instancia por pantalla: start() al abrirla, shutdown() al cerrarla.
 */
class FrenchVoice @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var engine: TextToSpeech? = null

    private val _status = MutableStateFlow(VoiceStatus.LOADING)
    val status: StateFlow<VoiceStatus> = _status.asStateFlow()

    /**
     * Starts the engine and checks French. Calling it again re-checks,
     * e.g. after the student installs the voice.
     * ---
     * Arranca el motor y comprueba el francés. Volver a llamarlo
     * comprueba de nuevo, p. ej. tras instalar la voz.
     */
    fun start() {
        shutdown()
        _status.value = VoiceStatus.LOADING
        var created: TextToSpeech? = null
        created = TextToSpeech(context) { initStatus ->
            val tts = created
            _status.value = if (initStatus != TextToSpeech.SUCCESS ||
                tts == null
            ) {
                VoiceStatus.UNAVAILABLE
            } else {
                when (tts.setLanguage(Locale.FRANCE)) {
                    TextToSpeech.LANG_MISSING_DATA -> VoiceStatus.MISSING_DATA
                    TextToSpeech.LANG_NOT_SUPPORTED -> VoiceStatus.UNAVAILABLE
                    else -> VoiceStatus.READY
                }
            }
        }
        engine = created
    }

    /**
     * Says the sentence, interrupting anything still being said.
     * ---
     * Dice la frase, cortando lo que se estuviera diciendo.
     */
    fun speak(text: String, slow: Boolean) {
        val tts = engine ?: return
        if (_status.value != VoiceStatus.READY) return
        tts.setSpeechRate(if (slow) SLOW_RATE else NORMAL_RATE)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    /**
     * Releases the engine.
     * ---
     * Libera el motor.
     */
    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
    }

    companion object {

        /**
         * Opens the system screen to install voice data.
         * ---
         * Abre la pantalla del sistema para instalar voces.
         */
        const val ACTION_INSTALL_VOICE =
            TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA

        private const val NORMAL_RATE = 1.0f
        private const val SLOW_RATE = 0.7f
        private const val UTTERANCE_ID = "mia2f-model-sentence"
    }
}
