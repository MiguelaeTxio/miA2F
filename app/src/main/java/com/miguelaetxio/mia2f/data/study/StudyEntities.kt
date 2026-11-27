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
        const val KIND_PRONUNCIATION = "pronunciation"
        const val KIND_LISTENING = "listening"

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

/**
 * A writing slip (punctuation, hyphen, capital letter...) found in an
 * answer. type is a SlipType name. isSerious is true when it was the
 * third time (or more) of the same type: then it also counted as an
 * error of the exercise.
 * ---
 * Un despiste de escritura (puntuación, guion, mayúscula...) en una
 * respuesta. type es el nombre de un SlipType. isSerious es true
 * cuando era la tercera vez (o más) del mismo tipo: entonces contó
 * también como error del ejercicio.
 */
@Entity(
    tableName = "writing_slip",
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
        Index("type"),
    ],
)
data class WritingSlip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    val type: String,
    val description: String,
    @ColumnInfo(name = "is_serious") val isSerious: Boolean,
)
