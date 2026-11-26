package com.miguelaetxio.mia2f.data.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
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
 * One line of a dialogue; speaker is 0 or 1.
 * ---
 * Una réplica de un diálogo; speaker es 0 o 1.
 */
data class DialogueLine(val speaker: Int, val text: String)

/**
 * The phone's French voice (fr-FR) for model sentences and dialogues.
 * One instance per screen: start() when it opens, shutdown() when it
 * closes.
 * ---
 * La voz francesa del móvil (fr-FR) para frases modelo y diálogos.
 * Una instancia por pantalla: start() al abrirla, shutdown() al
 * cerrarla.
 */
class FrenchVoice @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var engine: TextToSpeech? = null

    private val _status = MutableStateFlow(VoiceStatus.LOADING)
    val status: StateFlow<VoiceStatus> = _status.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)

    /**
     * True while something is being said. Updated from the engine's
     * callbacks, which may come from several threads.
     * ---
     * True mientras se está diciendo algo. Lo actualizan los avisos
     * del motor, que pueden llegar desde varios hilos.
     */
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // Voces francesas instaladas y sin red, una por interlocutor si
    // hay dos; si solo hay una, el segundo se distingue por el tono.
    // ---
    // Installed offline French voices, one per speaker if there are
    // two; with only one, the second speaker differs by pitch.
    private var speakerVoices: List<Voice> = emptyList()

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
                    else -> {
                        speakerVoices = frenchVoices(tts)
                        tts.setOnUtteranceProgressListener(progress)
                        VoiceStatus.READY
                    }
                }
            }
        }
        engine = created
    }

    private val progress = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            _isSpeaking.value = true
        }

        override fun onDone(utteranceId: String?) {
            if (utteranceId == LAST_ID) _isSpeaking.value = false
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            _isSpeaking.value = false
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            _isSpeaking.value = false
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            _isSpeaking.value = false
        }
    }

    private fun frenchVoices(tts: TextToSpeech): List<Voice> =
        runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
            .filter {
                it.locale.language == Locale.FRENCH.language &&
                    !it.isNetworkConnectionRequired &&
                    NOT_INSTALLED !in it.features.orEmpty()
            }
            .sortedWith(
                compareByDescending<Voice> {
                    it.locale.country == Locale.FRANCE.country
                }.thenBy { it.name }
            )
            .take(2)

    /**
     * Reads a dialogue line by line, each speaker with their own voice
     * (or pitch), interrupting anything still being said.
     * ---
     * Lee un diálogo réplica a réplica, cada interlocutor con su voz
     * (o su tono), cortando lo que se estuviera diciendo.
     */
    fun speakDialogue(lines: List<DialogueLine>, slow: Boolean) {
        val tts = engine ?: return
        if (_status.value != VoiceStatus.READY || lines.isEmpty()) return
        tts.stop()
        tts.setSpeechRate(if (slow) SLOW_RATE else NORMAL_RATE)
        lines.forEachIndexed { index, line ->
            val voice = speakerVoices.getOrNull(line.speaker)
                ?: speakerVoices.firstOrNull()
            if (voice != null) tts.setVoice(voice)
            tts.setPitch(
                if (line.speaker == 1 && speakerVoices.size < 2) {
                    SECOND_SPEAKER_PITCH
                } else {
                    NORMAL_PITCH
                }
            )
            val id = if (index == lines.lastIndex) LAST_ID else "line-$index"
            tts.speak(line.text, TextToSpeech.QUEUE_ADD, null, id)
        }
    }

    /**
     * Stops whatever is being said.
     * ---
     * Para lo que se esté diciendo.
     */
    fun stop() {
        engine?.stop()
        _isSpeaking.value = false
    }

    /**
     * Says the sentence, interrupting anything still being said.
     * ---
     * Dice la frase, cortando lo que se estuviera diciendo.
     */
    fun speak(text: String, slow: Boolean) {
        val tts = engine ?: return
        if (_status.value != VoiceStatus.READY) return
        speakerVoices.firstOrNull()?.let { tts.setVoice(it) }
        tts.setPitch(NORMAL_PITCH)
        tts.setSpeechRate(if (slow) SLOW_RATE else NORMAL_RATE)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, LAST_ID)
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
        _isSpeaking.value = false
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
        private const val NORMAL_PITCH = 1.0f
        private const val SECOND_SPEAKER_PITCH = 0.75f
        private const val LAST_ID = "mia2f-last"
        private const val NOT_INSTALLED =
            TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED
    }
}
