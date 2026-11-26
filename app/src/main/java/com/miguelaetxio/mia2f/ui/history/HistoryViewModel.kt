package com.miguelaetxio.mia2f.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelaetxio.mia2f.data.course.CourseRepository
import com.miguelaetxio.mia2f.data.study.ConceptError
import com.miguelaetxio.mia2f.data.study.ConceptErrorDao
import com.miguelaetxio.mia2f.data.study.Exercise
import com.miguelaetxio.mia2f.data.study.ExerciseDao
import com.miguelaetxio.mia2f.data.study.SessionSummary
import com.miguelaetxio.mia2f.data.study.StudySession
import com.miguelaetxio.mia2f.data.study.StudySessionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Class history: the list and the detail of one class.
 * ---
 * Historial de clases: la lista y el detalle de una clase.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    courseRepository: CourseRepository,
    studySessionDao: StudySessionDao,
    private val exerciseDao: ExerciseDao,
    private val conceptErrorDao: ConceptErrorDao,
) : ViewModel() {

    private val course = courseRepository.course

    val sessions: StateFlow<List<SessionSummary>> = studySessionDao
        .observeSummaries()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * "Unit N: title" for a unit ID, or the ID if the course lacks it.
     * ---
     * «Unidad N: título» de un identificador de unidad, o el
     * identificador si el curso no lo tiene.
     */
    private fun unitTitle(unitId: String): String =
        course.value?.units?.firstOrNull { it.id == unitId }
            ?.let { "Unidad ${it.number}: ${it.title}" } ?: unitId

    /**
     * Heading of a class: "Repaso" for a review; "Test · ",
     * "Pronunciación · " or "Escucha · " plus the unit; the unit for a
     * lesson.
     * ---
     * Título de una clase: «Repaso» para una prueba de repaso;
     * «Test · », «Pronunciación · » o «Escucha · » y la unidad; la
     * unidad para una clase.
     */
    fun sessionTitle(kind: String, unitId: String): String = when (kind) {
        StudySession.KIND_REVIEW -> "Repaso"
        StudySession.KIND_TEST -> "Test · " + unitTitle(unitId)
        StudySession.KIND_PRONUNCIATION ->
            "Pronunciación · " + unitTitle(unitId)
        StudySession.KIND_LISTENING -> "Escucha · " + unitTitle(unitId)
        else -> unitTitle(unitId)
    }

    /**
     * Concept title for an ID, or the ID if the course lacks it.
     * ---
     * Título del concepto de un identificador, o el identificador si
     * el curso no lo tiene.
     */
    fun conceptTitle(conceptId: String): String =
        course.value?.units?.flatMap { it.concepts }
            ?.firstOrNull { it.id == conceptId }?.title ?: conceptId

    /**
     * Exercises of a class, each with its errors.
     * ---
     * Ejercicios de una clase, cada uno con sus errores.
     */
    fun detail(
        sessionId: Long,
    ): Flow<List<Pair<Exercise, List<ConceptError>>>> =
        combine(
            exerciseDao.observeBySession(sessionId),
            conceptErrorDao.observeBySession(sessionId),
        ) { exercises, errors ->
            exercises.map { exercise ->
                exercise to errors.filter { it.exerciseId == exercise.id }
            }
        }
}
