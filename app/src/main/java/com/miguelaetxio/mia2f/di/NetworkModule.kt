package com.miguelaetxio.mia2f.di

import com.miguelaetxio.mia2f.data.backup.DriveApiService
import com.miguelaetxio.mia2f.data.backup.DriveUploadApiService
import com.miguelaetxio.mia2f.data.course.CourseApiService
import com.miguelaetxio.mia2f.data.gemini.GeminiApiService
import com.miguelaetxio.mia2f.data.remote.AppUpdateApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Network dependencies. The update client points at the github.com
 * root: the manifest and the APK are plain Release asset downloads.
 * ---
 * Dependencias de red. El cliente de actualizaciones apunta a la raíz
 * de github.com: manifiesto y APK son descargas normales de assets de
 * Release.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val GITHUB_BASE_URL = "https://github.com/"

    // miCursos, repositorio público de temarios (H02), servido sin
    // API ni autenticación; comprobado el 2026-11-26 (HTTP 200).
    private const val COURSES_BASE_URL = "https://raw.githubusercontent.com/"

    // Gemini Developer API (REST v1beta), comprobado en línea el
    // 2026-11-26 en ai.google.dev/api/generate-content.
    private const val GEMINI_BASE_URL =
        "https://generativelanguage.googleapis.com/"

    // Drive REST v3 (H05): metadatos y subida van en rutas distintas,
    // comprobado en línea el 2026-11-26 (files.update, manage-uploads).
    private const val DRIVE_BASE_URL = "https://www.googleapis.com/drive/v3/"
    private const val DRIVE_UPLOAD_BASE_URL =
        "https://www.googleapis.com/upload/drive/v3/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideAppUpdateApiService(
        okHttpClient: OkHttpClient,
    ): AppUpdateApiService =
        Retrofit.Builder()
            .baseUrl(GITHUB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AppUpdateApiService::class.java)

    @Provides
    @Singleton
    fun provideGeminiApiService(
        okHttpClient: OkHttpClient,
    ): GeminiApiService =
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)

    @Provides
    @Singleton
    fun provideCourseApiService(
        okHttpClient: OkHttpClient,
    ): CourseApiService =
        Retrofit.Builder()
            .baseUrl(COURSES_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CourseApiService::class.java)

    @Provides
    @Singleton
    fun provideDriveApiService(
        okHttpClient: OkHttpClient,
    ): DriveApiService =
        Retrofit.Builder()
            .baseUrl(DRIVE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DriveApiService::class.java)

    @Provides
    @Singleton
    fun provideDriveUploadApiService(
        okHttpClient: OkHttpClient,
    ): DriveUploadApiService =
        Retrofit.Builder()
            .baseUrl(DRIVE_UPLOAD_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DriveUploadApiService::class.java)
}
