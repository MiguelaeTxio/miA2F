package com.miguelaetxio.mia2f.data.study

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Local record of the student's classes. The schema is not exported:
 * CI cannot commit the generated JSON back to the repository.
 * ---
 * Registro local de las clases del alumno. El esquema no se exporta:
 * CI no puede devolver al repositorio el JSON que genera.
 */
@Database(
    entities = [StudySession::class, Exercise::class, ConceptError::class],
    version = 1,
    exportSchema = false,
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun studySessionDao(): StudySessionDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun conceptErrorDao(): ConceptErrorDao
}
