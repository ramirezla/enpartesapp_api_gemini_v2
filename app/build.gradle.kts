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
    namespace = "com.ehome.enpartesapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ehome.enpartesapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.98.1-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Lee la API Key desde local.properties y la asigna a BuildConfig
        buildConfigField("String", "GEMINI_API_KEY", localProperties.getProperty("GEMINI_API_KEY") ?: "")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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