package com.miguelaetxio.mia2f.di

import android.content.Context
import androidx.room.Room
import com.miguelaetxio.mia2f.data.study.ConceptErrorDao
import com.miguelaetxio.mia2f.data.study.ExerciseDao
import com.miguelaetxio.mia2f.data.study.StudyDatabase
import com.miguelaetxio.mia2f.data.study.StudySessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room database and DAOs.
 * ---
 * Base de datos de Room y sus DAO.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStudyDatabase(
        @ApplicationContext context: Context,
    ): StudyDatabase =
        Room.databaseBuilder(context, StudyDatabase::class.java, "study.db")
            .addMigrations(StudyDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideStudySessionDao(db: StudyDatabase): StudySessionDao =
        db.studySessionDao()

    @Provides
    fun provideExerciseDao(db: StudyDatabase): ExerciseDao =
        db.exerciseDao()

    @Provides
    fun provideConceptErrorDao(db: StudyDatabase): ConceptErrorDao =
        db.conceptErrorDao()
}
