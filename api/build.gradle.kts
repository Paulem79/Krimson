plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.paperweight.userdev)
    kotlin("plugin.lombok")
}

val targetJavaVersion = libs.versions.java.get().toInt()

dependencies {
    // Paper NMS
    paperweight.paperDevBundle(libs.versions.paper)

    // Dépendances de l'API exportées
    implementation(libs.universal.scheduler)
    implementation(libs.custom.block.data)
    implementation(libs.packed.core)

    implementation(libs.mcbrawls.inject.spigot)
    implementation(libs.mcbrawls.inject.api)
    implementation(libs.mcbrawls.inject.http)
    implementation(libs.mcbrawls.inject.jetty)
    implementation(libs.mcbrawls.inject.javalin) {
        isTransitive = false
    }
    implementation(libs.javalin)

    implementation(libs.arcana)

    implementation(libs.commons.lang3)

    compileOnly(libs.viaversion.api)
    compileOnly(libs.jetbrains.annotations)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

java {
    sourceCompatibility = JavaVersion.toVersion(targetJavaVersion)
    targetCompatibility = JavaVersion.toVersion(targetJavaVersion)
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
repositories {
    mavenCentral()
}