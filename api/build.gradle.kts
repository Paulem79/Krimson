plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.paperweight.userdev)
}

val targetJavaVersion = libs.versions.java.get().toInt()

dependencies {
    // Paper NMS
    paperweight.paperDevBundle(libs.versions.paper)

    // Dépendances de l'API exportées
    api(libs.universal.scheduler)
    api(libs.custom.block.data)
    api(libs.packed.core)

    api(libs.mcbrawls.inject.spigot)
    api(libs.mcbrawls.inject.api)
    api(libs.mcbrawls.inject.http)
    api(libs.mcbrawls.inject.jetty)
    api(libs.mcbrawls.inject.javalin) {
        isTransitive = false
    }
    api(libs.javalin)

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