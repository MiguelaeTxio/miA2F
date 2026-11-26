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
 * Chooses the unit of the next class from the course and the local
 * record: the first unit with concepts not practised yet; if all are
 * practised, the unit with most recent errors (ties go to the
 * earliest unit).
 * ---
 * Elige la unidad de la siguiente clase a partir del curso y del
 * registro local: la primera unidad con conceptos sin practicar; si
 * todos lo están, la de más errores recientes (a igualdad, la
 * primera).
 */
@Singleton
class StudyPlanner @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val conceptErrorDao: ConceptErrorDao,
) {

    suspend fun plan(course: Course): StudyPlan? {
        val units = course.units.sortedBy { it.number }
        if (units.isEmpty()) return null
        val practised = exerciseDao.practisedConceptIds().toSet()
        val recentErrors = conceptErrorDao.recent(RECENT_ERRORS_LIMIT)
        val unitOfConcept = units
            .flatMap { unit -> unit.concepts.map { it.id to unit } }
            .toMap()
        val unit = units.firstOrNull { unit ->
            unit.concepts.any { it.id !in practised }
        } ?: units.maxBy { unit ->
            recentErrors.count { unitOfConcept[it.conceptId] == unit }
        }
        return StudyPlan(
            unit = unit,
            progressSummary = summarize(course, practised, recentErrors),
        )
    }

    private fun summarize(
        course: Course,
        practised: Set<String>,
        recentErrors: List<ConceptError>,
    ): String {
        val titleOf = course.units
            .flatMap { it.concepts }
            .associate { it.id to it.title }
        val practisedLine = practised
            .mapNotNull { titleOf[it] }
            .sorted()
            .joinToString("; ")
            .ifEmpty { "ninguno todavía" }
        val errorLines = recentErrors
            .filter { it.conceptId in titleOf }
            .joinToString("\n") {
                "- ${titleOf.getValue(it.conceptId)} " +
                    "(${it.conceptId}): ${it.description}"
            }
            .ifEmpty { "- ninguno todavía" }
        return "Conceptos ya practicados: $practisedLine\n" +
            "Errores de las últimas clases:\n$errorLines"
    }

    private companion object {
        const val RECENT_ERRORS_LIMIT = 30
    }
}
