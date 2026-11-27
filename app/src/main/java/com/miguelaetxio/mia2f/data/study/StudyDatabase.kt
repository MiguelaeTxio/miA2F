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
    entities = [
        StudySession::class,
        Exercise::class,
        ConceptError::class,
        WritingSlip::class,
    ],
    version = StudyDatabase.VERSION,
    exportSchema = false,
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun studySessionDao(): StudySessionDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun conceptErrorDao(): ConceptErrorDao
    abstract fun writingSlipDao(): WritingSlipDao

    companion object {

        /**
         * Schema version; also written in every Drive backup.
         * ---
         * Versión del esquema; también va en cada copia de Drive.
         */
        const val VERSION = 3

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

        /**
         * v3 adds writing slips, to count repeated ones (S3). The SQL
         * matches what Room generates for WritingSlip.
         * ---
         * La v3 añade los despistes de escritura, para contar los
         * reiterados (S3). El SQL coincide con el que genera Room
         * para WritingSlip.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `writing_slip` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`session_id` INTEGER NOT NULL, " +
                        "`exercise_id` INTEGER NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`is_serious` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`session_id`) REFERENCES " +
                        "`study_session`(`id`) ON UPDATE NO ACTION " +
                        "ON DELETE CASCADE , " +
                        "FOREIGN KEY(`exercise_id`) REFERENCES " +
                        "`exercise`(`id`) ON UPDATE NO ACTION " +
                        "ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "`index_writing_slip_session_id` " +
                        "ON `writing_slip` (`session_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "`index_writing_slip_exercise_id` " +
                        "ON `writing_slip` (`exercise_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_writing_slip_type` " +
                        "ON `writing_slip` (`type`)"
                )
            }
        }
    }
}
