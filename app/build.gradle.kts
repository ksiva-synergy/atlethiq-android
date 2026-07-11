import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.atlethiq"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.atlethiq"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val localProperties = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) {
                FileInputStream(file).use { load(it) }
            }
        }
        resValue("string", "SUPABASE_URL", localProperties.getProperty("SUPABASE_URL") ?: "https://placeholder-url.supabase.co")
        resValue("string", "SUPABASE_ANON_KEY", localProperties.getProperty("SUPABASE_ANON_KEY") ?: "placeholder-key")
        buildConfigField("String", "MOCK_USER_EMAIL", "\"${localProperties.getProperty("MOCK_USER_EMAIL") ?: ""}\"")
        buildConfigField("String", "MOCK_USER_PASSWORD", "\"${localProperties.getProperty("MOCK_USER_PASSWORD") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
      resValues = true
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.text.googlefonts)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Hilt
  implementation(libs.google.dagger.hilt.android)
  ksp(libs.google.dagger.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)

  // Room
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  implementation(libs.androidx.room.ktx)

  // WorkManager
  implementation(libs.androidx.work.runtime.ktx)

  // Health Connect
  implementation(libs.androidx.health.connect.client)

  // Supabase
  implementation(libs.supabase.core)
  implementation(libs.supabase.postgrest)
  implementation(libs.supabase.auth)
  implementation(libs.supabase.functions)
  implementation(libs.ktor.client.android)

  // Vico Charts
  implementation(libs.vico.compose)
  implementation(libs.vico.compose.m3)
}
