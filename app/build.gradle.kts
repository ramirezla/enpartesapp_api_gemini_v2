import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.ehome.autoperitajeia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ehome.autoperitajeia"
        minSdk = 24
        targetSdk = 35
        versionCode = 200
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Lee la API Key desde local.properties y la asigna a BuildConfig
        buildConfigField("String", "GEMINI_API_KEY", localProperties.getProperty("GEMINI_API_KEY") ?: "")
        // Porcentaje por defecto de preferencia para REPARAR vs REEMPLAZAR (80% en desarrollo)
        buildConfigField("int", "PORCENTAJE_REPARACION", "80")
    }

    buildTypes {
        debug {
            // 80% de preferencia por reparación en compilaciones de desarrollo
            buildConfigField("int", "PORCENTAJE_REPARACION", "80")
        }
        release {
            isMinifyEnabled = false
            // 70% de preferencia por reparación en compilaciones de producción
            buildConfigField("int", "PORCENTAJE_REPARACION", "70")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.generativeai)
    implementation(libs.androidx.cardview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Implementar el icono para mostrar u ocultar el password

    // Retrofit
    implementation(libs.retrofit) // Or the latest version
    // Gson Converter (if you're using Gson for JSON parsing)
    implementation(libs.converter.gson) // Or thelatest version
    // OkHttp (Retrofit uses OkHttp for networking)
    implementation(libs.okhttp) // Or the latest version
    // OkHttp Logging Interceptor (for debugging network requests)
    implementation(libs.logging.interceptor) // Or the latest version
}