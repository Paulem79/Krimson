plugins {
    id("java")
    kotlin("jvm") version "2.+"

    id("idea")

    id("com.gradleup.shadow")

    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "net.paulem"
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

        maven {
            name = "radRepoPublic"
            url = uri("https://maven.rad.vg/public")
        }
        maven("https://maven.mcbrawls.net/releases/")
    }

    dependencies {
        if (project.name == "paper") compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
        else compileOnly("org.spigotmc:spigot-api:1.21.8-R0.1-SNAPSHOT")

        compileOnly("org.jetbrains:annotations:26.0.2-1")

        compileOnly("org.projectlombok:lombok:1.18.42")
        annotationProcessor("org.projectlombok:lombok:1.18.42")
    }

    artifacts.archives(tasks.shadowJar)
    tasks.shadowJar {
        archiveClassifier.set("")
        exclude("META-INF/**")

        relocate("com.github.Anon8281.universalScheduler", "net.paulem.krimson.libs.universalScheduler")
        relocate("com.jeff_media.customblockdata", "net.paulem.krimson.libs.customblockdata")
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

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests = false
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
    implementation("com.jeff-media:custom-block-data:2.2.5")

    implementation("net.radstevee.packed:packed-core:1.+")

    implementation("org.apache.commons:commons-lang3:3.20.0")

    implementation("net.mcbrawls.inject:spigot:3.+")
    implementation("net.mcbrawls.inject:api:3.+")
    implementation("net.mcbrawls.inject:http:3.+")
    implementation("net.mcbrawls.inject:jetty:3.+")
    implementation("net.mcbrawls.inject:javalin:3.+") {
        isTransitive = false
    }
    implementation("io.javalin:javalin:6.7.0")

    compileOnly("io.netty:netty-all:4.2.9.Final")
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
            implementation("com.jeff-media:custom-block-data:2.2.5")
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

val buildRust by tasks.registering(Exec::class) {
    workingDir = file("native")
    val cargoExe = if (System.getProperty("os.name").lowercase().contains("win")) "cargo.exe" else "cargo"
    val userHome = System.getProperty("user.home")
    val cargoPath = file("$userHome/.cargo/bin/$cargoExe")
    val skipRustBuild = project.hasProperty("skipRustBuild")
    if (cargoPath.exists()) {
        commandLine(cargoPath, "build", "--release")
    } else {
        commandLine("cargo", "build", "--release")
    }
    onlyIf { !skipRustBuild }
    inputs.files(fileTree("native/src"))
    inputs.file("native/Cargo.toml")
    outputs.dir("native/target/release")
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

tasks.processResources {
    val skipRustBuild = project.hasProperty("skipRustBuild")
    if (!skipRustBuild) {
        dependsOn(buildRust)
    }

    from("native/target/release") {
        include("*.dll", "*.so", "*.dylib")
        into("native")
    }

    from("native/libs") {
        include("*.dll", "*.so", "*.dylib")
        into("native")
    }

    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}