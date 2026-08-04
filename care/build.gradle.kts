import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.google.services) // reads care/google-services.json
    alias(libs.plugins.crashlytics) // uploads R8 mapping files for readable release stack traces
}

// Release signing credentials live in local.properties (git-ignored) on dev
// machines, or in environment variables in CI. Same keystore as :app — one
// signing identity for both EasyLink apps.
val localProps = Properties().also { props ->
    rootProject
        .file("local.properties")
        .takeIf { it.exists() }
        ?.inputStream()
        ?.use { props.load(it) }
}

fun signingProp(name: String): String? = localProps[name] as String? ?: System.getenv(name)

android {
    namespace = "com.fangjet.care"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fangjet.care"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val path = signingProp("KEYSTORE_PATH")
            if (path != null) {
                storeFile = rootProject.file(path)
                storePassword = signingProp("KEYSTORE_PASSWORD") ?: ""
                keyAlias = signingProp("KEY_ALIAS") ?: ""
                keyPassword = signingProp("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Falls back to unsigned when no keystore is configured (CI without
            // secrets, a fresh clone) rather than failing the build.
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // Shared Firestore contract — the whole reason these apps live in one repo
    implementation(project(":shared"))

    // Core
    implementation(libs.androidx.core.ktx)
    coreLibraryDesugaring(libs.android.desugar.jdk)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation + lifecycle
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Firebase — Auth (caregiver sign-in + elder anonymous), Firestore (the
    // shared config/status/events contract), and FCM (alert push).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics) // crash reporting — no code needed, auto-initialises
    implementation(libs.kotlinx.coroutines.play.services) // Task.await()

    // DataStore — persists the redeemed linkId
    implementation(libs.datastore.preferences)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
