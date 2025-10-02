import java.net.URI

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
        maven {
            url = uri("https://jfrog.amani.ai/artifactory/amani-biomatch-sdk/")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jfrog.amani.ai/artifactory/amani-biomatch-sdk/")
        }
        maven {
            url = URI("https://jfrog.amani.ai/artifactory/amani-sdk")
        }
    }
}

rootProject.name = "BioMatchDemo"
include(":app")
