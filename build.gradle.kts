plugins {
    id("java")
    kotlin("jvm") version "2.+"

    id("idea")

    id("com.gradleup.shadow")

    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "ovh.paulem"
version = "1.0"

val targetJavaVersion = 21

allprojects {
    plugins.apply("java")
    plugins.apply("com.gradleup.shadow")

    repositories {
        mavenCentral()
        maven {
            name = "spigotmc-repo"
            url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        }
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/content/groups/public/")
        }
        maven { url = uri("https://jitpack.io") }
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }

        maven("https://maven.radsteve.net/public")
        maven("https://maven.andante.dev/releases/")
    }

    dependencies {
        if (project.name == "paper") compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
        else compileOnly("org.spigotmc:spigot-api:1.21.8-R0.1-SNAPSHOT")

        compileOnly("org.jetbrains:annotations:26.0.2")

        compileOnly("org.projectlombok:lombok:1.18.38")
        annotationProcessor("org.projectlombok:lombok:1.18.38")
    }

    artifacts.archives(tasks.shadowJar)
    tasks.shadowJar {
        archiveClassifier.set("")
        exclude("META-INF/**")

        relocate("com.github.Anon8281.universalScheduler", "ovh.paulem.krimson.libs.universalScheduler")
        relocate("com.jeff_media.customblockdata", "ovh.paulem.krimson.libs.customblockdata")
    }

    tasks.build {
        dependsOn(tasks.shadowJar)
    }

    java {
        sourceCompatibility = JavaVersion.toVersion(targetJavaVersion)
        targetCompatibility = JavaVersion.toVersion(targetJavaVersion)
        if (JavaVersion.current() < JavaVersion.toVersion(targetJavaVersion)) {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"

        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }
}

dependencies {
    implementation(project(":paper")) {
        isTransitive = false
    }
    implementation(project(":spigot")) {
        isTransitive = false
    }
    implementation(project(":common")) {
        isTransitive = false
    }

    implementation("com.github.Anon8281:UniversalScheduler:0.+")

    implementation("com.jeff-media:custom-block-data:2.2.4")

    implementation("net.radstevee.packed:packed-core:1.+")

    implementation("net.mcbrawls.inject:spigot:3.+")
    implementation("net.mcbrawls.inject:api:3.+")
    implementation("net.mcbrawls.inject:http:3.+")
    implementation("net.mcbrawls.inject:jetty:3.+")
    implementation("net.mcbrawls.inject:javalin:3.+") {
        isTransitive = false
    }
    implementation("io.javalin:javalin:6.7.0")

    compileOnly("io.netty:netty-all:4.1.97.Final")
}

var alreadyMappedCommon = false

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")

    group = rootProject.group
    version = rootProject.version

    if (project.name != "common") {
        dependencies {
            implementation(project(":common")) {
                isTransitive = false
            }
        }

        tasks.compileJava {
            dependsOn(project(":common").tasks.build)
        }

        alreadyMappedCommon = true

        tasks.shadowJar {
            if (alreadyMappedCommon) exclude("**/common/**")
        }
    } else {
        dependencies {
            implementation("com.github.Anon8281:UniversalScheduler:0.+")
            implementation("com.jeff-media:custom-block-data:2.2.4")
        }
    }
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    minimize()
}

tasks.compileJava {
    subprojects.forEach {
        dependsOn(it.tasks.build)
    }
}

tasks {
    runServer {
        minecraftVersion("1.21.8")
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}