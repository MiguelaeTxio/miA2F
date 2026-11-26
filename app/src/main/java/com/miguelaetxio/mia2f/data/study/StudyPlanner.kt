package com.miguelaetxio.mia2f.data.study

import com.miguelaetxio.mia2f.data.course.Course
import com.miguelaetxio.mia2f.data.course.CourseUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the next class works on: the unit and the progress summary
 * sent to Gemini.
 * ---
 * Lo que trabaja la siguiente clase: la unidad y el resumen de
 * progreso que se envía a Gemini.
 */
data class StudyPlan(
    val unit: CourseUnit,
    val progressSummary: String,
)

/**
 * Chooses the unit of the next class from the state of each concept:
 * the first unit with concepts not worked yet or under watch; if
 * there is none, the unit with most recent errors (ties go to the
 * earliest unit).
 * ---
 * Elige la unidad de la siguiente clase según el estado de cada
 * concepto: la primera unidad con conceptos sin trabajar o en
 * observación; si no queda ninguna, la de más errores recientes (a
 * igualdad, la primera).
 */
@Singleton
class StudyPlanner @Inject constructor(
    private val conceptProgressCalculator: ConceptProgressCalculator,
    private val conceptErrorDao: ConceptErrorDao,
) {

    suspend fun plan(course: Course): StudyPlan? {
        val units = course.units.sortedBy { it.number }
        if (units.isEmpty()) return null
        val progress = conceptProgressCalculator.calculate(course)
        val recentErrors = conceptErrorDao.recent(RECENT_ERRORS_LIMIT)
        val unitOfConcept = units
            .flatMap { unit -> unit.concepts.map { it.id to unit } }
            .toMap()
        val unit = units.firstOrNull { unit ->
            unit.concepts.any { progress[it.id]?.status in PENDING }
        } ?: units.maxBy { unit ->
            recentErrors.count { unitOfConcept[it.conceptId] == unit }
        }
        return StudyPlan(
            unit = unit,
            progressSummary = summarize(course, progress),
        )
    }

    private fun summarize(
        course: Course,
        progress: Map<String, ConceptProgress>,
    ): String {
        val concepts = course.units
            .sortedBy { it.number }
            .flatMap { it.concepts }
        fun titlesWith(status: ConceptStatus): String = concepts
            .filter { progress[it.id]?.status == status }
            .joinToString("; ") { it.title }
            .ifEmpty { "ninguno" }
        val watchedLines = concepts
            .filter { progress[it.id]?.status == ConceptStatus.WATCHED }
            .joinToString("\n") { concept ->
                "- ${concept.title} (${concept.id}): " +
                    progress.getValue(concept.id).recentErrors
                        .joinToString(" | ")
            }
            .ifEmpty { "- ninguno" }
        return "Conceptos en observación (errores aún sin superar), " +
            "con sus errores más recientes:\n$watchedLines\n" +
            "Conceptos superados: ${titlesWith(ConceptStatus.PASSED)}\n" +
            "Conceptos acertados sin errores: " +
            titlesWith(ConceptStatus.CORRECT)
    }

    private companion object {
        const val RECENT_ERRORS_LIMIT = 30
        val PENDING = setOf(ConceptStatus.NOT_WORKED, ConceptStatus.WATCHED)
    }
}
