package com.miguelaetxio.mia2f.data.remote

import com.miguelaetxio.mia2f.data.remote.dto.UpdateManifest
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Reads the latest manifest from miA2FReleases through GitHub's
 * `releases/latest/download/{asset}` redirect (public repo, no API,
 * no auth), and downloads the APK from the manifest's absolute URL.
 * ---
 * Lee el manifiesto más reciente de miA2FReleases mediante la
 * redirección `releases/latest/download/{asset}` de GitHub (repo
 * público, sin API ni autenticación), y descarga el APK desde la URL
 * absoluta del manifiesto.
 */
interface AppUpdateApiService {

    @GET("MiguelaeTxio/miA2FReleases/releases/latest/download/manifest.json")
    suspend fun getLatestManifest(): UpdateManifest

    @Streaming
    @GET
    suspend fun downloadApk(@Url apkUrl: String): ResponseBody
}
