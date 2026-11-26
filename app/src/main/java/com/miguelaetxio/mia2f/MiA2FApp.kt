package com.miguelaetxio.mia2f

import android.app.Application
import android.os.Build
import com.miguelaetxio.mia2f.data.log.AppLog
import com.miguelaetxio.mia2f.data.log.LogArea
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class: bootstraps Hilt and the diagnostic logs.
 * ---
 * Clase Application: arranca Hilt y los logs de diagnóstico.
 */
@HiltAndroidApp
class MiA2FApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AppLog.log(
            LogArea.APP,
            "Arranque: miA2F ${BuildConfig.VERSION_NAME} " +
                "(${BuildConfig.VERSION_CODE}), ${Build.MANUFACTURER} " +
                "${Build.MODEL}, Android ${Build.VERSION.RELEASE}",
        )
    }
}
