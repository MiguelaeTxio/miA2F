package com.miguelaetxio.mia2f.data.study

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One class (study session) of the student. endedAt is null while the
 * class is still open. Times are epoch milliseconds.
 * ---
 * Una clase (sesión de estudio) del alumno. endedAt es null mientras
 * la clase sigue abierta. Las horas van en milisegundos desde epoch.
 */
@Entity(tableName = "study_session")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "unit_id") val unitId: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
    @ColumnInfo(defaultValue = KIND_LESSON) val kind: String = KIND_LESSON,
) {
    companion object {
        const val KIND_LESSON = "lesson"
        const val KIND_TEST = "test"
        const val KIND_REVIEW = "review"

        // Una prueba de repaso mezcla conceptos de varias unidades.
        // ---
        // A review mixes concepts from several units.
        const val REVIEW_UNIT_ID = "review"
    }
}

/**
 * One written exercise of a class. Answer, correction and result stay
 * null until the student answers and Gemini corrects it.
 * ---
 * Un ejercicio escrito de una clase. Respuesta, corrección y resultado
 * quedan a null hasta que el alumno responde y Gemini corrige.
 */
@Entity(
    tableName = "exercise",
    foreignKeys = [
        ForeignKey(
            entity = StudySession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("session_id")],
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    val position: Int,
    @ColumnInfo(name = "concept_id") val conceptId: String,
    val prompt: String,
    @ColumnInfo(name = "student_answer") val studentAnswer: String? = null,
    val correction: String? = null,
    @ColumnInfo(name = "is_correct") val isCorrect: Boolean? = null,
)

/**
 * One error found in an exercise, tied to a course concept.
 * ---
 * Un error detectado en un ejercicio, ligado a un concepto del curso.
 */
@Entity(
    tableName = "concept_error",
    foreignKeys = [
        ForeignKey(
            entity = StudySession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("session_id"),
        Index("exercise_id"),
        Index("concept_id"),
    ],
)
data class ConceptError(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "concept_id") val conceptId: String,
    val description: String,
)
