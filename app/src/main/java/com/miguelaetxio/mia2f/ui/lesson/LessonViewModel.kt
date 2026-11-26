package com.miguelaetxio.mia2f.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelaetxio.mia2f.data.course.CourseRepository
import com.miguelaetxio.mia2f.data.course.CourseUnit
import com.miguelaetxio.mia2f.data.gemini.AnswerCorrection
import com.miguelaetxio.mia2f.data.gemini.GeminiRepository
import com.miguelaetxio.mia2f.data.gemini.GeneratedExercise
import com.miguelaetxio.mia2f.data.gemini.TeacherResult
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
 * What the class screen shows.
 * ---
 * Lo que muestra la pantalla de clase.
 */
sealed class LessonState {
    data class Loading(val message: String) : LessonState()

    data class Failed(
        val message: String,
        val isOffline: Boolean,
    ) : LessonState()

    data class Working(
        val unitTitle: String,
        val number: Int,
        val total: Int,
        val conceptTitle: String,
        val prompt: String,
        val correction: AnswerCorrection?,
        val isCorrecting: Boolean,
        val message: String?,
        val isOffline: Boolean,
    ) : LessonState()

    data class Finished(
        val unitTitle: String,
        val isTest: Boolean,
        val correct: Int,
        val total: Int,
        val errors: List<String>,
    ) : LessonState()
}

/**
 * Runs one class: plans the unit, gets the exercises from Gemini,
 * saves each correction in Room as soon as it arrives, and closes the
 * class at the end.
 * ---
 * Lleva una clase: planifica la unidad, pide los ejercicios a Gemini,
 * guarda cada corrección en Room en cuanto llega y cierra la clase al
 * final.
 */
@HiltViewModel
class LessonViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val studyPlanner: StudyPlanner,
    private val geminiRepository: GeminiRepository,
    private val studySessionDao: StudySessionDao,
    private val exerciseDao: ExerciseDao,
    private val conceptErrorDao: ConceptErrorDao,
) : ViewModel() {

    private val _state =
        MutableStateFlow<LessonState>(LessonState.Loading(PLANNING))
    val state: StateFlow<LessonState> = _state.asStateFlow()

    private var unit: CourseUnit? = null
    private var sessionId: Long? = null
    private var exercises: List<Exercise> = emptyList()
    private var current = 0
    private val corrections = mutableMapOf<Int, AnswerCorrection>()
    private var lastAnswer: String? = null
    private var started = false
    private var isTest = false

    /**
     * Starts the class once; test = true for a unit test (one exercise
     * per concept, passed only with all of them right).
     * ---
     * Empieza la clase una sola vez; test = true para un test de
     * unidad (un ejercicio por concepto, aprobado solo con todos bien).
     */
    fun start(test: Boolean) {
        if (started) return
        started = true
        isTest = test
        load()
    }

    /**
     * Retries what failed: starting the class or the last correction.
     * ---
     * Reintenta lo que falló: empezar la clase o la última corrección.
     */
    fun retry() {
        val answer = lastAnswer
        if (sessionId == null) load() else if (answer != null) {
            submit(answer)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = LessonState.Loading(PLANNING)
            val course = courseRepository.course.value
            val plan = course?.let { studyPlanner.plan(it) }
            if (plan == null) {
                _state.value = LessonState.Failed(
                    "No hay temario guardado todavía.", isOffline = false,
                )
                return@launch
            }
            _state.value = LessonState.Loading(
                (if (isTest) "Preparando el test de la unidad " else
                    "Preparando los ejercicios de la unidad ") +
                    "${plan.unit.number}: ${plan.unit.title}…"
            )
            val result = if (isTest) {
                geminiRepository.generateTest(plan.unit)
            } else {
                geminiRepository.generateExercises(
                    plan.unit, plan.progressSummary,
                )
            }
            when (result) {
                is TeacherResult.Success -> begin(plan.unit, result.value)
                is TeacherResult.Error -> _state.value =
                    LessonState.Failed(result.message, isOffline = false)
                TeacherResult.Offline -> _state.value = LessonState.Failed(
                    "Sin conexión: la clase necesita Internet para " +
                        "hablar con Gemini.",
                    isOffline = true,
                )
            }
        }
    }

    private suspend fun begin(
        courseUnit: CourseUnit,
        generated: List<GeneratedExercise>,
    ) {
        val id = studySessionDao.insert(
            StudySession(
                unitId = courseUnit.id,
                startedAt = System.currentTimeMillis(),
                kind = if (isTest) {
                    StudySession.KIND_TEST
                } else {
                    StudySession.KIND_LESSON
                },
            )
        )
        val rows = generated.mapIndexed { index, exercise ->
            Exercise(
                sessionId = id,
                position = index + 1,
                conceptId = exercise.conceptId,
                prompt = exercise.prompt,
            )
        }
        val ids = exerciseDao.insertAll(rows)
        unit = courseUnit
        sessionId = id
        exercises = rows.mapIndexed { index, row -> row.copy(id = ids[index]) }
        current = 0
        showCurrent(isCorrecting = false, message = null, isOffline = false)
    }

    /**
     * Sends the answer to the current exercise for correction.
     * ---
     * Envía a corregir la respuesta al ejercicio actual.
     */
    fun submit(answer: String) {
        val courseUnit = unit ?: return
        val exercise = exercises.getOrNull(current) ?: return
        val text = answer.trim()
        if (text.isEmpty() || current in corrections) return
        lastAnswer = text
        showCurrent(isCorrecting = true, message = null, isOffline = false)
        viewModelScope.launch {
            when (
                val result = geminiRepository.correctAnswer(
                    courseUnit,
                    GeneratedExercise(exercise.conceptId, exercise.prompt),
                    text,
                )
            ) {
                is TeacherResult.Success -> save(exercise, text, result.value)
                is TeacherResult.Error -> showCurrent(
                    isCorrecting = false,
                    message = result.message,
                    isOffline = false,
                )
                TeacherResult.Offline -> showCurrent(
                    isCorrecting = false,
                    message = "Sin conexión: la respuesta se corregirá " +
                        "cuando vuelva Internet. Pulsa «Reintentar».",
                    isOffline = true,
                )
            }
        }
    }

    private suspend fun save(
        exercise: Exercise,
        answer: String,
        correction: AnswerCorrection,
    ) {
        val saved = exercise.copy(
            studentAnswer = answer,
            correction = "**Corrección:** ${correction.correctedAnswer}" +
                "\n\n${correction.explanation}",
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
        showCurrent(isCorrecting = false, message = null, isOffline = false)
    }

    /**
     * Moves to the next exercise, or closes the class after the last.
     * ---
     * Pasa al siguiente ejercicio, o cierra la clase tras el último.
     */
    fun next() {
        if (current !in corrections) return
        if (current + 1 < exercises.size) {
            current += 1
            showCurrent(
                isCorrecting = false, message = null, isOffline = false,
            )
        } else {
            finish()
        }
    }

    private fun finish() {
        val id = sessionId ?: return
        val courseUnit = unit ?: return
        viewModelScope.launch {
            studySessionDao.finish(id, System.currentTimeMillis())
            val titleOf = courseUnit.concepts.associate { it.id to it.title }
            _state.value = LessonState.Finished(
                unitTitle = "Unidad ${courseUnit.number}: ${courseUnit.title}",
                isTest = isTest,
                correct = corrections.values.count { it.isCorrect },
                total = exercises.size,
                errors = corrections.values.flatMap { it.errors }.map {
                    "${titleOf[it.conceptId] ?: it.conceptId}: " +
                        it.description
                },
            )
        }
    }

    private fun showCurrent(
        isCorrecting: Boolean,
        message: String?,
        isOffline: Boolean,
    ) {
        val courseUnit = unit ?: return
        val exercise = exercises.getOrNull(current) ?: return
        _state.value = LessonState.Working(
            unitTitle = "Unidad ${courseUnit.number}: ${courseUnit.title}",
            number = current + 1,
            total = exercises.size,
            conceptTitle = courseUnit.concepts
                .firstOrNull { it.id == exercise.conceptId }?.title
                ?: exercise.conceptId,
            prompt = exercise.prompt,
            correction = corrections[current],
            isCorrecting = isCorrecting,
            message = message,
            isOffline = isOffline,
        )
    }

    private companion object {
        const val PLANNING = "Preparando la clase…"
    }
}
