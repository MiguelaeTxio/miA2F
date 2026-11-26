plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace  = "com.miguelaetxio.mia2f"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.miguelaetxio.mia2f"
        minSdk        = 26
        targetSdk     = 36
        // versionCode dinámico: llega como -PversionCode=N desde el
        // workflow (github.run_number, siempre creciente), para que
        // Android acepte cada build como actualización. Fallback a 1
        // fuera de CI.
        versionCode = (project.findProperty("versionCode") as String?)
            ?.toIntOrNull() ?: 1
        versionName = "0.1"
    }

    signingConfigs {
        getByName("debug") {
            // Ruta explícita a la keystore que restaura el workflow
            // (DEBUG_KEYSTORE_BASE64), igual que en MiMoo: sin ella AGP
            // resuelve la keystore de debug de forma implícita y no hay
            // garantía de firmar siempre con la misma clave.
            storeFile = file(
                "${System.getProperty("user.home")}/.android/debug.keystore"
            )
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
}
