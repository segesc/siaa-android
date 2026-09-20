pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SIAA"
include(
    ":app",
    ":core:model",
    ":core:algorithm",
    ":core:runtime",
    ":core:data",
    ":core:audio",
    ":core:content"
)
