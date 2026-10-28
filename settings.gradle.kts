pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}
rootProject.name = "trace"
includeBuild("build-logic")
include("agent", "collector", "store", "graph", "jdi-driver", "mcp-server", "eval", "demo-app")
