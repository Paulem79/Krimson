rootProject.name = "Krimson"

pluginManagement {
    plugins {
        id("com.gradleup.shadow") version "9.3.1"
    }
}

plugins {
    // add toolchain resolver
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("paper")
include("spigot")
include("common")