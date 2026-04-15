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
}

rootProject.name = "PharmaNet"
include(":app")
include(":core")
include(":core:common")
include(":core:network")
include(":designsystem")
include(":feature:auth")
include(":feature:home")
include(":feature:request")
include(":feature:orders")
include(":feature:profile")
include(":feature:warehouses")
include(":feature:notifications")
include(":feature:tracking")
