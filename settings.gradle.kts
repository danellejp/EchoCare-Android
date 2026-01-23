pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Required for MPAndroidChart library
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "EchoCare"
include(":app")