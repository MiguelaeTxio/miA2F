package com.miguelaetxio.mia2f.data.backup

import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Drive REST v3 metadata endpoint (`.../drive/v3/`): list, create the
 * empty file and download its content with alt=media. Same pattern as
 * MiMoo H06; the token travels as a per-call header.
 * ---
 * Endpoint de metadatos de Drive REST v3 (`.../drive/v3/`): listar,
 * crear el archivo vacío y descargar su contenido con alt=media. Mismo
 * patrón que MiMoo H06; el token va como cabecera en cada llamada.
 */
interface DriveApiService {

    @GET("files")
    suspend fun listFiles(
        @Header("Authorization") bearerToken: String,
        @Query("q") query: String,
        @Query("fields") fields: String = "files(id,name,modifiedTime)",
        @Query("orderBy") orderBy: String = "modifiedTime desc",
    ): DriveFileList

    @POST("files")
    suspend fun createFile(
        @Header("Authorization") bearerToken: String,
        @Body metadata: DriveFileCreate,
    ): DriveFile

    @GET("files/{fileId}")
    suspend fun download(
        @Header("Authorization") bearerToken: String,
        @Path("fileId") fileId: String,
        @Query("alt") alt: String = "media",
    ): ResponseBody
}

/**
 * Drive REST v3 upload endpoint (`.../upload/drive/v3/`): overwrites
 * the content of an existing file (files.update, PATCH, media).
 * ---
 * Endpoint de subida de Drive REST v3 (`.../upload/drive/v3/`):
 * sobrescribe el contenido de un archivo existente (files.update,
 * PATCH, media).
 */
interface DriveUploadApiService {

    @PATCH("files/{fileId}")
    suspend fun uploadContent(
        @Header("Authorization") bearerToken: String,
        @Path("fileId") fileId: String,
        @Body content: RequestBody,
        @Query("uploadType") uploadType: String = "media",
    ): DriveFile
}

data class DriveFile(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("modifiedTime") val modifiedTime: String? = null,
)

data class DriveFileList(
    @SerializedName("files") val files: List<DriveFile>? = null,
)

data class DriveFileCreate(
    @SerializedName("name") val name: String,
    @SerializedName("mimeType") val mimeType: String,
)
