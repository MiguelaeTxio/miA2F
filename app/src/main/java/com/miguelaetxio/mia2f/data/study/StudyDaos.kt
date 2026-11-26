package com.miguelaetxio.mia2f.data.study

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Access to classes.
 * ---
 * Acceso a las clases.
 */
@Dao
interface StudySessionDao {

    @Insert
    suspend fun insert(session: StudySession): Long

    @Query("UPDATE study_session SET ended_at = :endedAt WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Long)

    @Query("SELECT * FROM study_session WHERE id = :id")
    suspend fun getById(id: Long): StudySession?

    @Query("SELECT * FROM study_session ORDER BY started_at DESC")
    fun observeAll(): Flow<List<StudySession>>

    @Query(
        "SELECT s.id, s.unit_id AS unitId, s.started_at AS startedAt, " +
            "s.ended_at AS endedAt, s.kind, " +
            "(SELECT COUNT(*) FROM exercise e WHERE e.session_id = s.id " +
            "AND e.is_correct IS NOT NULL) AS answered, " +
            "(SELECT COUNT(*) FROM concept_error c " +
            "WHERE c.session_id = s.id) AS errors " +
            "FROM study_session s ORDER BY s.started_at DESC"
    )
    fun observeSummaries(): Flow<List<SessionSummary>>

    @Query(
        "SELECT COUNT(*) FROM study_session WHERE kind = 'lesson' " +
            "AND ended_at IS NOT NULL AND id > (SELECT COALESCE(MAX(id), " +
            "0) FROM study_session WHERE kind = 'review' " +
            "AND ended_at IS NOT NULL)"
    )
    fun observeLessonsSinceReview(): Flow<Int>
}

/**
 * One row of the class history: answered exercises and errors.
 * ---
 * Una fila del historial de clases: ejercicios respondidos y errores.
 */
data class SessionSummary(
    val id: Long,
    val unitId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val kind: String,
    val answered: Int,
    val errors: Int,
)

/**
 * Access to exercises.
 * ---
 * Acceso a los ejercicios.
 */
@Dao
interface ExerciseDao {

    @Insert
    suspend fun insertAll(exercises: List<Exercise>): List<Long>

    @Update
    suspend fun update(exercise: Exercise)

    @Query(
        "SELECT * FROM exercise WHERE session_id = :sessionId " +
            "ORDER BY position"
    )
    fun observeBySession(sessionId: Long): Flow<List<Exercise>>

    @Query(
        "SELECT e.concept_id AS conceptId, e.session_id AS sessionId, " +
            "s.unit_id AS unitId, s.kind, e.is_correct AS isCorrect " +
            "FROM exercise e JOIN study_session s ON s.id = e.session_id"
    )
    suspend fun outcomes(): List<ExerciseOutcome>
}

/**
 * One exercise with the class it belongs to, for the concept states.
 * ---
 * Un ejercicio con la clase a la que pertenece, para los estados de
 * los conceptos.
 */
data class ExerciseOutcome(
    val conceptId: String,
    val sessionId: Long,
    val unitId: String,
    val kind: String,
    val isCorrect: Boolean?,
)

/**
 * Access to errors by concept.
 * ---
 * Acceso a los errores por concepto.
 */
@Dao
interface ConceptErrorDao {

    @Insert
    suspend fun insertAll(errors: List<ConceptError>)

    @Query("SELECT * FROM concept_error WHERE session_id = :sessionId")
    fun observeBySession(sessionId: Long): Flow<List<ConceptError>>

    @Query("SELECT * FROM concept_error ORDER BY id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ConceptError>

    @Query("SELECT * FROM concept_error ORDER BY id")
    suspend fun all(): List<ConceptError>
}
