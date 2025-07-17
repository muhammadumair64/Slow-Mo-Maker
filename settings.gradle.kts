pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        jcenter()
        maven(url = "https://jitpack.io")
        maven ( url = "https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea" )
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven(url = "https://jitpack.io")
        maven ( url = "https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea" )
    }
}

rootProject.name = "PhotoToVideoSlidesMaker"
include(":app")
include(":library")