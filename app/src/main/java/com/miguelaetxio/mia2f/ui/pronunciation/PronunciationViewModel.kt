package com.miguelaetxio.mia2f.ui.pronunciation

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelaetxio.mia2f.data.backup.ProgressBackupRepository
import com.miguelaetxio.mia2f.data.course.CourseRepository
import com.miguelaetxio.mia2f.data.course.CourseUnit
import com.miguelaetxio.mia2f.data.gemini.GeminiRepository
import com.miguelaetxio.mia2f.data.gemini.GeneratedExercise
import com.miguelaetxio.mia2f.data.gemini.PronunciationFeedback
import com.miguelaetxio.mia2f.data.gemini.TeacherResult
import com.miguelaetxio.mia2f.data.speech.FrenchVoice
import com.miguelaetxio.mia2f.data.speech.Recording
import com.miguelaetxio.mia2f.data.speech.VoiceRecorder
import com.miguelaetxio.mia2f.data.speech.VoiceStatus
import com.miguelaetxio.mia2f.data.study.ConceptError
import com.miguelaetxio.mia2f.data.study.ConceptErrorDao
import com.miguelaetxio.mia2f.data.study.Exercise
import com.miguelaetxio.mia2f.data.study.ExerciseDao
import com.miguelaetxio.mia2f.data.study.StudyPlanner
import com.miguelaetxio.mia2f.data.study.StudySession
import com.miguelaetxio.mia2f.data.study.StudySessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * What the student is doing with the current sentence.
 * ---
 * Lo que está haciendo el alumno con la frase actual.
 */
enum class RecordPhase { IDLE, RECORDING, SENDING }

/**
 * What the pronunciation screen shows.
 * ---
 * Lo que muestra la pantalla de pronunciación.
 */
sealed class PronunciationState {
    data class Loading(val message: String) : PronunciationState()

    data class Failed(
        val message: String,
        val isOffline: Boolean,
    ) : PronunciationState()

    data class Working(
        val unitTitle: String,
        val number: Int,
        val total: Int,
        val conceptTitle: String,
        val sentence: String,
        val phase: RecordPhase,
        val feedback: PronunciationFeedback?,
        val canResend: Boolean,
        val message: String?,
    ) : PronunciationState()

    data class Finished(
        val unitTitle: String,
        val correct: Int,
        val total: Int,
        val errors: List<String>,
    ) : PronunciationState()
}

/**
 * Runs one pronunciation practice: model sentences of the planned
 * unit, the phone's French voice, the recording, and Gemini's comment
 * on the real audio. Each comment is saved in Room as soon as it
 * arrives; the recording is deleted once Gemini has answered.
 * ---
 * Lleva una práctica de pronunciación: frases modelo de la unidad
 * planificada, la voz francesa del móvil, la grabación y el
 * comentario de Gemini sobre el audio real. Cada comentario se guarda
 * en Room en cuanto llega; la grabación se borra cuando Gemini
 * responde.
 */
@HiltViewModel
class PronunciationViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val studyPlanner: StudyPlanner,
    private val geminiRepository: GeminiRepository,
    private val studySessionDao: StudySessionDao,
    private val exerciseDao: ExerciseDao,
    private val conceptErrorDao: ConceptErrorDao,
    private val progressBackupRepository: ProgressBackupRepository,
    private val voice: FrenchVoice,
    private val recorder: VoiceRecorder,
) : ViewModel() {

    private val _state = MutableStateFlow<PronunciationState>(
        PronunciationState.Loading(PLANNING)
    )
    val state: StateFlow<PronunciationState> = _state.asStateFlow()

    val voiceStatus: StateFlow<VoiceStatus> = voice.status

    private var unit: CourseUnit? = null
    private var sessionId: Long? = null
    private var exercises: List<Exercise> = emptyList()
    private var current = 0
    private val feedbacks = mutableMapOf<Int, PronunciationFeedback>()
    private var phase = RecordPhase.IDLE
    private var pending: Recording? = null
    private var started = false

    /**
     * Starts once: the voice and the sentences.
     * ---
     * Empieza una sola vez: la voz y las frases.
     */
    fun start() {
        if (started) return
        started = true
        voice.start()
        load()
    }

    /**
     * Checks the French voice again (after installing it).
     * ---
     * Vuelve a comprobar la voz francesa (tras instalarla).
     */
    fun recheckVoice() {
        voice.start()
    }

    /**
     * Retries loading the sentences.
     * ---
     * Reintenta cargar las frases.
     */
    fun retry() {
        if (sessionId == null) load()
    }

    /**
     * Plays the current sentence with the phone's French voice.
     * ---
     * Reproduce la frase actual con la voz francesa del móvil.
     */
    fun listen(slow: Boolean) {
        exercises.getOrNull(current)?.let { voice.speak(it.prompt, slow) }
    }

    /**
     * Starts recording; the microphone permission is already granted.
     * ---
     * Empieza a grabar; el permiso de micrófono ya está concedido.
     */
    fun startRecording() {
        if (phase != RecordPhase.IDLE || current in feedbacks) return
        discardPending()
        if (!recorder.start(onMaxDuration = { stopRecording() })) {
            show("No se ha podido usar el micrófono.")
            return
        }
        phase = RecordPhase.RECORDING
        show(null)
    }

    /**
     * Stops recording and sends the audio to Gemini.
     * ---
     * Para la grabación y envía el audio a Gemini.
     */
    fun stopRecording() {
        if (phase != RecordPhase.RECORDING) return
        phase = RecordPhase.IDLE
        val recording = recorder.stop()
        if (recording == null) {
            show("No se ha grabado nada. Pulsa «Grabar» y di la frase.")
            return
        }
        pending = recording
        send()
    }

    /**
     * Sends the last recording again (after a connection error).
     * ---
     * Vuelve a enviar la última grabación (tras un error de conexión).
     */
    fun resend() {
        if (phase == RecordPhase.IDLE && pending != null) send()
    }

    private fun send() {
        val recording = pending ?: return
        val courseUnit = unit ?: return
        val exercise = exercises.getOrNull(current) ?: return
        phase = RecordPhase.SENDING
        show(null)
        viewModelScope.launch {
            val base64 = withContext(Dispatchers.IO) {
                Base64.encodeToString(
                    recording.file.readBytes(), Base64.NO_WRAP,
                )
            }
            val result = geminiRepository.evaluatePronunciation(
                courseUnit,
                GeneratedExercise(exercise.conceptId, exercise.prompt),
                base64,
                recording.mimeType,
            )
            phase = RecordPhase.IDLE
            when (result) {
                is TeacherResult.Success -> save(exercise, result.value)
                is TeacherResult.Error -> show(result.message)
                TeacherResult.Offline -> show(
                    "Sin conexión: pulsa «Reenviar» cuando vuelva Internet."
                )
            }
        }
    }

    private suspend fun save(
        exercise: Exercise,
        feedback: PronunciationFeedback,
    ) {
        discardPending()
        val saved = exercise.copy(
            studentAnswer = "Se oye: ${feedback.heard}",
            correction = feedback.explanation,
            isCorrect = feedback.isCorrect,
        )
        exerciseDao.update(saved)
        conceptErrorDao.insertAll(
            feedback.errors.map {
                ConceptError(
                    sessionId = exercise.sessionId,
                    exerciseId = exercise.id,
                    conceptId = it.conceptId,
                    description = it.description,
                )
            }
        )
        exercises = exercises.map { if (it.id == saved.id) saved else it }
        feedbacks[current] = feedback
        show(null)
    }

    /**
     * Next sentence, or the end of the practice after the last one.
     * ---
     * Siguiente frase, o el final de la práctica tras la última.
     */
    fun next() {
        if (current !in feedbacks) return
        if (current + 1 < exercises.size) {
            current += 1
            show(null)
        } else {
            finish()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = PronunciationState.Loading(PLANNING)
            val course = courseRepository.course.value
            val plan = course?.let { studyPlanner.plan(it) }
            if (plan == null) {
                _state.value = PronunciationState.Failed(
                    "No hay temario guardado todavía.", isOffline = false,
                )
                return@launch
            }
            _state.value = PronunciationState.Loading(
                "Preparando las frases de la unidad " +
                    "${plan.unit.number}: ${plan.unit.title}…"
            )
            when (
                val result = geminiRepository.generatePronunciationSentences(
                    plan.unit, plan.progressSummary,
                )
            ) {
                is TeacherResult.Success -> begin(plan.unit, result.value)
                is TeacherResult.Error -> _state.value =
                    PronunciationState.Failed(result.message, false)
                TeacherResult.Offline -> _state.value =
                    PronunciationState.Failed(
                        "Sin conexión: la práctica necesita Internet " +
                            "para hablar con Gemini.",
                        isOffline = true,
                    )
            }
        }
    }

    private suspend fun begin(
        courseUnit: CourseUnit,
        sentences: List<GeneratedExercise>,
    ) {
        val id = studySessionDao.insert(
            StudySession(
                unitId = courseUnit.id,
                startedAt = System.currentTimeMillis(),
                kind = StudySession.KIND_PRONUNCIATION,
            )
        )
        val rows = sentences.mapIndexed { index, sentence ->
            Exercise(
                sessionId = id,
                position = index + 1,
                conceptId = sentence.conceptId,
                prompt = sentence.prompt,
            )
        }
        val ids = exerciseDao.insertAll(rows)
        unit = courseUnit
        sessionId = id
        exercises = rows.mapIndexed { index, row -> row.copy(id = ids[index]) }
        current = 0
        show(null)
    }

    private fun finish() {
        val id = sessionId ?: return
        val courseUnit = unit ?: return
        viewModelScope.launch {
            studySessionDao.finish(id, System.currentTimeMillis())
            progressBackupRepository.markPending()
            val titleOf = courseUnit.concepts.associate { it.id to it.title }
            _state.value = PronunciationState.Finished(
                unitTitle = heading(courseUnit),
                correct = feedbacks.values.count { it.isCorrect },
                total = exercises.size,
                errors = feedbacks.values.flatMap { it.errors }.map {
                    "${titleOf[it.conceptId] ?: it.conceptId}: " +
                        it.description
                },
            )
        }
    }

    private fun show(message: String?) {
        val courseUnit = unit ?: return
        val exercise = exercises.getOrNull(current) ?: return
        _state.value = PronunciationState.Working(
            unitTitle = heading(courseUnit),
            number = current + 1,
            total = exercises.size,
            conceptTitle = courseUnit.concepts
                .firstOrNull { it.id == exercise.conceptId }?.title
                ?: exercise.conceptId,
            sentence = exercise.prompt,
            phase = phase,
            feedback = feedbacks[current],
            canResend = pending != null && phase == RecordPhase.IDLE,
            message = message,
        )
    }

    private fun discardPending() {
        pending?.delete()
        pending = null
    }

    private fun heading(courseUnit: CourseUnit): String =
        "Unidad ${courseUnit.number}: ${courseUnit.title}"

    override fun onCleared() {
        recorder.discard()
        discardPending()
        voice.shutdown()
    }

    private companion object {
        const val PLANNING = "Preparando la práctica…"
    }
}
