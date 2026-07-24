rootProject.name = "Krimson"

pluginManagement {
    plugins {
        id("com.gradleup.shadow") version "9.3.1"
        kotlin("plugin.lombok") version "2.4.10"
    }
}

plugins {
    // add toolchain resolver
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("api")