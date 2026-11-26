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
}

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

    @Query("SELECT DISTINCT concept_id FROM exercise " +
        "WHERE is_correct IS NOT NULL")
    suspend fun practisedConceptIds(): List<String>
}

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
}
