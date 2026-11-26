package com.miguelaetxio.mia2f.data.study

import com.miguelaetxio.mia2f.data.course.Course
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the student stands on one course concept.
 * ---
 * En qué punto está el alumno en un concepto del curso.
 */
enum class ConceptStatus {
    /** Never answered correctly. / Nunca acertado. */
    NOT_WORKED,

    /** Answered correctly, no error ever. / Acertado, sin ningún error. */
    CORRECT,

    /** Has errors, not yet cleared. / Con errores, aún sin superar. */
    WATCHED,

    /** Errors cleared. / Errores superados. */
    PASSED,
}

/**
 * State of one concept, with its most recent errors (newest first).
 * ---
 * Estado de un concepto, con sus errores más recientes (el último
 * primero).
 */
data class ConceptProgress(
    val conceptId: String,
    val status: ConceptStatus,
    val recentErrors: List<String>,
)

/**
 * Gives every course concept its state with the FrancaisA2 criterion:
 * an error is only cleared when, after the last class with an error
 * on that concept, the concept was answered correctly without errors
 * in at least two different classes. A unit test with every exercise
 * right clears all the concepts of its unit from then on, unless they
 * have an error in a later class.
 * ---
 * Da a cada concepto del curso su estado con el criterio de
 * FrancaisA2: un error solo se supera cuando, después de la última
 * clase con error de ese concepto, se acertó sin error en al menos dos
 * clases distintas. Un test de unidad con todos los ejercicios bien da
 * por superados todos los conceptos de su unidad desde entonces, salvo
 * que tengan un error en una clase posterior.
 */
@Singleton
class ConceptProgressCalculator @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val conceptErrorDao: ConceptErrorDao,
) {

    /**
     * State of every concept of the course, keyed by concept id.
     * ---
     * Estado de cada concepto del curso, por identificador.
     */
    suspend fun calculate(course: Course): Map<String, ConceptProgress> =
        compute(course, exerciseDao.outcomes(), conceptErrorDao.all())

    companion object {

        const val CLEAN_SESSIONS_TO_PASS = 2
        const val RECENT_ERRORS_PER_CONCEPT = 3

        /**
         * Pure calculation. Class ids grow with time, so a higher id
         * means a later class.
         * ---
         * Cálculo puro. Los identificadores de clase crecen con el
         * tiempo: un id mayor es una clase posterior.
         */
        fun compute(
            course: Course,
            outcomes: List<ExerciseOutcome>,
            errors: List<ConceptError>,
        ): Map<String, ConceptProgress> {
            val errorsByConcept = errors.groupBy { it.conceptId }
            val correctByConcept = outcomes
                .filter { it.isCorrect == true }
                .groupBy({ it.conceptId }, { it.sessionId })
            val lastPassedTestOfUnit = outcomes
                .filter { it.kind == StudySession.KIND_TEST }
                .groupBy { it.sessionId }
                .filterValues { rows -> rows.all { it.isCorrect == true } }
                .map { (sessionId, rows) -> rows.first().unitId to sessionId }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, ids) -> ids.max() }
            return course.units.flatMap { unit ->
                val passedTest = lastPassedTestOfUnit[unit.id]
                unit.concepts.map { concept ->
                    val conceptErrors = errorsByConcept[concept.id].orEmpty()
                    val correctSessions =
                        correctByConcept[concept.id].orEmpty().toSet()
                    val lastError = conceptErrors.maxOfOrNull { it.sessionId }
                    val status = when {
                        passedTest != null &&
                            (lastError == null || lastError < passedTest) ->
                            ConceptStatus.PASSED
                        lastError == null && correctSessions.isEmpty() ->
                            ConceptStatus.NOT_WORKED
                        lastError == null -> ConceptStatus.CORRECT
                        correctSessions.count { it > lastError } >=
                            CLEAN_SESSIONS_TO_PASS -> ConceptStatus.PASSED
                        else -> ConceptStatus.WATCHED
                    }
                    concept.id to ConceptProgress(
                        conceptId = concept.id,
                        status = status,
                        recentErrors = conceptErrors
                            .sortedByDescending { it.id }
                            .take(RECENT_ERRORS_PER_CONCEPT)
                            .map { it.description },
                    )
                }
            }.toMap()
        }
    }
}
