import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.google.services) // reads app/google-services.json
    alias(libs.plugins.crashlytics) // uploads R8 mapping files for readable release stack traces
}

val localProps = Properties().also { props ->
    rootProject
        .file("local.properties")
        .takeIf { it.exists() }
        ?.inputStream()
        ?.use { props.load(it) }
}

// Signing credentials come from local.properties on dev machines, or from
// environment variables in CI (.github/workflows/release.yml exports them).
fun signingProp(name: String): String? = localProps[name] as String? ?: System.getenv(name)

android {
    namespace = "com.fangjet.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fangjet.launcher"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── Distribution flavors ──────────────────────────────────────────────────
    // standard — Play Store build. No SOS / fall detection / voice, so the
    //            manifest carries no SMS, fine-location, microphone, or
    //            health-foreground-service permissions (all Play-restricted
    //            or scrutinized). See src/safety/AndroidManifest.xml.
    // safety   — everything enabled; sideload / future release once the
    //            restricted-permission declarations are approved.
    // Both share one applicationId (and Firebase registration); only one can
    // be installed at a time.
    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {
            dimension = "distribution"
            isDefault = true
            buildConfigField("boolean", "SAFETY_FEATURES", "false")
        }
        create("safety") {
            dimension = "distribution"
            versionNameSuffix = "-safety"
            buildConfigField("boolean", "SAFETY_FEATURES", "true")
        }
    }

    signingConfigs {
        create("release") {
            val path = signingProp("KEYSTORE_PATH")
            if (path != null) {
                // rootProject.file resolves CI's repo-relative path the same way
                // from every module (plain file() would resolve against app/).
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

    packaging {
        jniLibs {
            useLegacyPackaging = true
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
        checkDependencies = true
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
    // Project modules
    implementation(project(":shared")) // Firestore contract shared with :care
    implementation(project(":weather")) // Weather widget, also reusable elsewhere

    // Firebase — Remote Config supplies server-tunable setting defaults.
    // The SDK compiles and the app runs without google-services.json; Firebase
    // simply stays uninitialised until it's added, and the provider falls back
    // to hardcoded defaults. See README → "Remote Config".
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.firebase.auth) // anonymous identity for pairing
    implementation(libs.firebase.firestore) // links/pairingCodes documents
    implementation(libs.firebase.crashlytics) // crash reporting — no code needed, auto-initialises
    implementation(libs.kotlinx.coroutines.play.services) // Task.await()

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    coreLibraryDesugaring(libs.android.desugar.jdk)

    // Compose BOM — pins all compose artifact versions together
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation + Hilt navigation
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Location
    implementation(libs.play.services.location)

    // Jetpack Startup
    implementation(libs.startup.runtime)

    // DataStore
    implementation(libs.datastore.preferences)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Coil (Compose integration)
    implementation(libs.coil.compose)

    // Tests
    testImplementation(libs.room.testing)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
