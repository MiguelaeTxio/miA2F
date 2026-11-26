package com.miguelaetxio.mia2f.ui.listening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelaetxio.mia2f.data.backup.ProgressBackupRepository
import com.miguelaetxio.mia2f.data.course.CourseRepository
import com.miguelaetxio.mia2f.data.course.CourseUnit
import com.miguelaetxio.mia2f.data.gemini.AnswerCorrection
import com.miguelaetxio.mia2f.data.gemini.GeminiRepository
import com.miguelaetxio.mia2f.data.gemini.ListeningPractice
import com.miguelaetxio.mia2f.data.gemini.TeacherResult
import com.miguelaetxio.mia2f.data.speech.DialogueLine
import com.miguelaetxio.mia2f.data.speech.FrenchVoice
import com.miguelaetxio.mia2f.data.speech.VoiceStatus
import com.miguelaetxio.mia2f.data.study.ConceptError
import com.miguelaetxio.mia2f.data.study.ConceptErrorDao
import com.miguelaetxio.mia2f.data.study.Exercise
import com.miguelaetxio.mia2f.data.study.ExerciseDao
import com.miguelaetxio.mia2f.data.study.StudyPlanner
import com.miguelaetxio.mia2f.data.study.StudySession
import com.miguelaetxio.mia2f.data.study.StudySessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the listening screen shows. The dialogue text is only part of
 * Finished: while answering, the student only hears it.
 * ---
 * Lo que muestra la pantalla de escucha. El texto del diálogo solo va
 * en Finished: mientras contesta, el alumno solo lo oye.
 */
sealed class ListeningState {
    data class Loading(val message: String) : ListeningState()

    data class Failed(
        val message: String,
        val isOffline: Boolean,
    ) : ListeningState()

    data class Working(
        val unitTitle: String,
        val number: Int,
        val total: Int,
        val question: String,
        val correction: AnswerCorrection?,
        val isCorrecting: Boolean,
        val message: String?,
        val isOffline: Boolean,
    ) : ListeningState()

    data class Finished(
        val unitTitle: String,
        val correct: Int,
        val total: Int,
        val errors: List<String>,
        val lines: List<DialogueLine>,
    ) : ListeningState()
}

/**
 * Runs one listening practice: a dialogue of the planned unit read by
 * the phone's voice, and comprehension questions corrected by Gemini.
 * Each correction is saved in Room as soon as it arrives, with the
 * transcript in the exercise so the history can show it.
 * ---
 * Lleva una práctica de escucha: un diálogo de la unidad planificada
 * leído por la voz del móvil y preguntas de comprensión corregidas por
 * Gemini. Cada corrección se guarda en Room en cuanto llega, con la
 * transcripción en el ejercicio para que el historial la muestre.
 */
@HiltViewModel
class ListeningViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val studyPlanner: StudyPlanner,
    private val geminiRepository: GeminiRepository,
    private val studySessionDao: StudySessionDao,
    private val exerciseDao: ExerciseDao,
    private val conceptErrorDao: ConceptErrorDao,
    private val progressBackupRepository: ProgressBackupRepository,
    private val voice: FrenchVoice,
) : ViewModel() {

    private val _state =
        MutableStateFlow<ListeningState>(ListeningState.Loading(PLANNING))
    val state: StateFlow<ListeningState> = _state.asStateFlow()

    val voiceStatus: StateFlow<VoiceStatus> = voice.status
    val isSpeaking: StateFlow<Boolean> = voice.isSpeaking

    private var unit: CourseUnit? = null
    private var practice: ListeningPractice? = null
    private var sessionId: Long? = null
    private var exercises: List<Exercise> = emptyList()
    private var current = 0
    private val corrections = mutableMapOf<Int, AnswerCorrection>()
    private var lastAnswer: String? = null
    private var started = false

    /**
     * Starts once: the voice and the practice.
     * ---
     * Empieza una sola vez: la voz y la práctica.
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
     * Plays the whole dialogue, as many times as wanted.
     * ---
     * Reproduce el diálogo entero, tantas veces como se quiera.
     */
    fun play(slow: Boolean) {
        practice?.let { voice.speakDialogue(it.lines, slow) }
    }

    /**
     * Stops the dialogue.
     * ---
     * Para el diálogo.
     */
    fun stopPlaying() {
        voice.stop()
    }

    /**
     * Retries what failed: loading the practice or the last
     * correction.
     * ---
     * Reintenta lo que falló: cargar la práctica o la última
     * corrección.
     */
    fun retry() {
        val answer = lastAnswer
        if (sessionId == null) load() else if (answer != null) {
            submit(answer)
        }
    }

    /**
     * Sends the answer to the current question for correction.
     * ---
     * Envía a corregir la respuesta a la pregunta actual.
     */
    fun submit(answer: String) {
        val courseUnit = unit ?: return
        val exercise = exercises.getOrNull(current) ?: return
        val question = practice?.questions?.getOrNull(current) ?: return
        val text = answer.trim()
        if (text.isEmpty() || current in corrections) return
        lastAnswer = text
        show(isCorrecting = true, message = null, isOffline = false)
        viewModelScope.launch {
            when (
                val result = geminiRepository.correctListeningAnswer(
                    courseUnit, transcript(), question, text,
                )
            ) {
                is TeacherResult.Success -> save(exercise, text, result.value)
                is TeacherResult.Error -> show(
                    isCorrecting = false,
                    message = result.message,
                    isOffline = false,
                )
                TeacherResult.Offline -> show(
                    isCorrecting = false,
                    message = "Sin conexión: la respuesta se corregirá " +
                        "cuando vuelva Internet. Pulsa «Reintentar».",
                    isOffline = true,
                )
            }
        }
    }

    /**
     * Next question, or the end of the practice after the last one.
     * ---
     * Siguiente pregunta, o el final de la práctica tras la última.
     */
    fun next() {
        if (current !in corrections) return
        if (current + 1 < exercises.size) {
            current += 1
            show(isCorrecting = false, message = null, isOffline = false)
        } else {
            finish()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = ListeningState.Loading(PLANNING)
            val course = courseRepository.course.value
            val plan = course?.let { studyPlanner.plan(it) }
            if (plan == null) {
                _state.value = ListeningState.Failed(
                    "No hay temario guardado todavía.", isOffline = false,
                )
                return@launch
            }
            _state.value = ListeningState.Loading(
                "Preparando un diálogo de la unidad " +
                    "${plan.unit.number}: ${plan.unit.title}…"
            )
            when (
                val result = geminiRepository.generateListening(
                    plan.unit, plan.progressSummary,
                )
            ) {
                is TeacherResult.Success -> begin(plan.unit, result.value)
                is TeacherResult.Error -> _state.value =
                    ListeningState.Failed(result.message, isOffline = false)
                TeacherResult.Offline -> _state.value =
                    ListeningState.Failed(
                        "Sin conexión: la práctica necesita Internet " +
                            "para hablar con Gemini.",
                        isOffline = true,
                    )
            }
        }
    }

    private suspend fun begin(
        courseUnit: CourseUnit,
        generated: ListeningPractice,
    ) {
        val id = studySessionDao.insert(
            StudySession(
                unitId = courseUnit.id,
                startedAt = System.currentTimeMillis(),
                kind = StudySession.KIND_LISTENING,
            )
        )
        unit = courseUnit
        practice = generated
        // El historial muestra el prompt: pregunta y transcripción.
        // ---
        // The history shows the prompt: question and transcript.
        val dialogue = generated.lines.joinToString("\n") {
            "- **${speakerName(it.speaker)}:** ${it.text}"
        }
        val rows = generated.questions.mapIndexed { index, question ->
            Exercise(
                sessionId = id,
                position = index + 1,
                conceptId = question.conceptId,
                prompt = "**Pregunta:** ${question.prompt}\n\n" +
                    "**Diálogo:**\n\n$dialogue",
            )
        }
        val ids = exerciseDao.insertAll(rows)
        sessionId = id
        exercises = rows.mapIndexed { index, row -> row.copy(id = ids[index]) }
        current = 0
        show(isCorrecting = false, message = null, isOffline = false)
    }

    private suspend fun save(
        exercise: Exercise,
        answer: String,
        correction: AnswerCorrection,
    ) {
        val saved = exercise.copy(
            studentAnswer = answer,
            correction = "**Respuesta esperada:** " +
                "${correction.correctedAnswer}\n\n${correction.explanation}",
            isCorrect = correction.isCorrect,
        )
        exerciseDao.update(saved)
        conceptErrorDao.insertAll(
            correction.errors.map {
                ConceptError(
                    sessionId = exercise.sessionId,
                    exerciseId = exercise.id,
                    conceptId = it.conceptId,
                    description = it.description,
                )
            }
        )
        exercises = exercises.map { if (it.id == saved.id) saved else it }
        corrections[current] = correction
        lastAnswer = null
        show(isCorrecting = false, message = null, isOffline = false)
    }

    private fun finish() {
        val id = sessionId ?: return
        val courseUnit = unit ?: return
        val lines = practice?.lines.orEmpty()
        voice.stop()
        viewModelScope.launch {
            studySessionDao.finish(id, System.currentTimeMillis())
            progressBackupRepository.markPending()
            val titleOf = courseUnit.concepts.associate { it.id to it.title }
            _state.value = ListeningState.Finished(
                unitTitle = heading(courseUnit),
                correct = corrections.values.count { it.isCorrect },
                total = exercises.size,
                errors = corrections.values.flatMap { it.errors }.map {
                    "${titleOf[it.conceptId] ?: it.conceptId}: " +
                        it.description
                },
                lines = lines,
            )
        }
    }

    private fun show(
        isCorrecting: Boolean,
        message: String?,
        isOffline: Boolean,
    ) {
        val courseUnit = unit ?: return
        val question = practice?.questions?.getOrNull(current) ?: return
        _state.value = ListeningState.Working(
            unitTitle = heading(courseUnit),
            number = current + 1,
            total = exercises.size,
            question = question.prompt,
            correction = corrections[current],
            isCorrecting = isCorrecting,
            message = message,
            isOffline = isOffline,
        )
    }

    private fun transcript(): String =
        practice?.lines.orEmpty().joinToString("\n") {
            "${speakerName(it.speaker)}: ${it.text}"
        }

    private fun heading(courseUnit: CourseUnit): String =
        "Unidad ${courseUnit.number}: ${courseUnit.title}"

    override fun onCleared() {
        voice.shutdown()
    }

    companion object {

        /**
         * "A" or "B", as Gemini wrote the dialogue.
         * ---
         * «A» o «B», como escribió Gemini el diálogo.
         */
        fun speakerName(speaker: Int): String = if (speaker == 1) "B" else "A"

        private const val PLANNING = "Preparando la práctica…"
    }
}
