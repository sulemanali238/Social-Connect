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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }

    // Manual 'versionCatalogs' block yahan se hata diya gaya hai
    // kyunki Gradle 'gradle/libs.versions.toml' ko khud hi detect kar leta hai.
}

rootProject.name = "Social Connect"
include(":app")