package com.miguelaetxio.mia2f.data.study

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Local record of the student's classes. The schema is not exported:
 * CI cannot commit the generated JSON back to the repository.
 * ---
 * Registro local de las clases del alumno. El esquema no se exporta:
 * CI no puede devolver al repositorio el JSON que genera.
 */
@Database(
    entities = [StudySession::class, Exercise::class, ConceptError::class],
    version = 2,
    exportSchema = false,
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun studySessionDao(): StudySessionDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun conceptErrorDao(): ConceptErrorDao

    companion object {

        /**
         * v2 adds the class kind (lesson or unit test); existing rows
         * are lessons.
         * ---
         * La v2 añade el tipo de clase (clase o test de unidad); las
         * filas existentes son clases.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE study_session ADD COLUMN kind TEXT " +
                        "NOT NULL DEFAULT 'lesson'"
                )
            }
        }
    }
}
