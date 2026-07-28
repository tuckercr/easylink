plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.fangjet.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    coreLibraryDesugaring(libs.android.desugar.jdk)

    // Deliberately dependency-free beyond the stdlib.
    //
    // The types here are plain data classes with default values, which is exactly
    // what Firestore's POJO mapper needs — so neither app is forced to agree on a
    // Firebase version through this module, and these stay unit-testable on the JVM.

    testImplementation(libs.junit)
}
