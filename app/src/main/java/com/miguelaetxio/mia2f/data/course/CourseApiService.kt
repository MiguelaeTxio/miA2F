package com.miguelaetxio.mia2f.data.course

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Reads the public miCursos repository through
 * raw.githubusercontent.com (no API, no auth). The course is fetched
 * as raw text so the exact JSON can be cached on disk.
 * ---
 * Lee el repositorio público miCursos a través de
 * raw.githubusercontent.com (sin API ni autenticación). El curso se
 * baja como texto para guardar en disco el JSON exacto.
 */
interface CourseApiService {

    @GET("MiguelaeTxio/miCursos/main/index.json")
    suspend fun getIndex(): CourseIndex

    @GET("MiguelaeTxio/miCursos/main/{path}")
    suspend fun getCourseFile(
        @Path(value = "path", encoded = true) path: String,
    ): ResponseBody
}
