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
    }
}

rootProject.name = "Santa-Game"

include(":core", ":android", ":lwjgl3")

if (System.getProperty("os.name").lowercase().contains("mac") || providers.gradleProperty("includeIos").isPresent) {
    include(":ios")
}
