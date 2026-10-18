pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "better-sheet"
include(":better-sheet", ":sample", ":findmy")

project(":sample").projectDir = file("samples/sample")
project(":findmy").projectDir = file("samples/findmy")
