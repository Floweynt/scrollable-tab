plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
    id("maven-publish")
}

val version: String by project
val group: String by project
val archives_base_name: String by project
val minecraft_version: String by project
val loader_version: String by project
val minecraft_version_dev: String by project

base {
    archivesName = archives_base_name
}

repositories {
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://maven.parchmentmc.org")
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version_dev}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.19.4:2023.06.26@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${loader_version}")

    modApi("me.shedaniel.cloth:cloth-config-fabric:10.1.117") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    modApi("com.terraformersmc:modmenu:6.2.1")
}

tasks {
    processResources {
        inputs.property("version", version)
        inputs.property("minecraft_version", minecraft_version)
        inputs.property("loader_version", loader_version)
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to version,
                    "minecraft_version" to minecraft_version,
                    "loader_version" to loader_version
                )
            )
        }
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${archives_base_name}"}
        }
    }
}

val targetJavaVersion = 17
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
    withSourcesJar()
}