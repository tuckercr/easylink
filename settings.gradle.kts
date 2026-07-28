pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "EasyLink"

// ── Applications ──────────────────────────────────────────────────────────
include(":app") // EasyLink Launcher — runs on the elder's phone
include(":care") // EasyLink Care — the caregiver companion app

// ── Libraries ─────────────────────────────────────────────────────────────
include(":shared") // Firestore contract shared by both apps
include(":weather") // Standalone weather widget, reusable across projects
