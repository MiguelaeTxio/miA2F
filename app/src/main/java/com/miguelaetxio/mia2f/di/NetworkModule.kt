package com.miguelaetxio.mia2f.di

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

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
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
}
