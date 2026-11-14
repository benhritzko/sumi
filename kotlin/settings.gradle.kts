rootProject.name = "sumi"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include(":sumi")        // the design system: tokens, theme, components
include(":playground")  // desktop app for trying things out
