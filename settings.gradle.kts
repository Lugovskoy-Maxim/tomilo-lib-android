pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // RuStore SDKs: Push, in-app update and review.
        maven { url = uri("https://nexus-external.vkteam.ru/repository/maven/") }
        maven { url = uri("https://artifactory-external.vkpartner.ru/artifactory/maven") }
    }
}

rootProject.name = "tomilo-lib-android"
include(":app")
