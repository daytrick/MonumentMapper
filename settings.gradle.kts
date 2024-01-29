pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
//dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
//    repositories {
//        google()
//
//        // MAVEN BELONGS HERE!!!
//        mavenLocal()
//        // From: https://stackoverflow.com/a/72960966
//        mavenCentral()
//        maven { url = uri("https://jitpack.io") }
//    }
//}

rootProject.name = "Monument Mapper"
include(":app")
