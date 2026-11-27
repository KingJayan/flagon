import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    `kotlin-dsl`
    `maven-publish`
}

// a separate build reads no root gradle.properties. a provider keeps it tracked
val shared = providers.fileContents(
    layout.projectDirectory.dir("../..").file("gradle.properties"),
).asText.map { text -> Properties().apply { load(text.reader()) } }

group = shared.map { it.getProperty("group") }.get()
version = shared.map { it.getProperty("version") }.get()
description = "Gradle plugin that compiles a module to a Flagon bundle"

// gradle 9 runs on java 17, and so do the teavm plugin and this one
java {
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // the consuming build gets the teavm plugin through this; this repository loads it at the root
    implementation("org.teavm:teavm-gradle-plugin:${libs.versions.teavm.get()}")
}

val versions = tasks.register<WriteProperties>("writeVersions") {
    destinationFile = layout.buildDirectory.file("versions/dev/flagon/build/versions.properties")
    property("flagon", version)
    property("teavm", libs.versions.teavm.get())
}

sourceSets.main {
    resources.srcDir(files(layout.buildDirectory.dir("versions")).builtBy(versions))
}

publishing {
    repositories {
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/KingJayan/flagon")
            credentials {
                username = providers.gradleProperty("githubUser")
                    .orElse(providers.environmentVariable("MAVEN_USERNAME")).orNull
                password = providers.gradleProperty("githubToken")
                    .orElse(providers.environmentVariable("MAVEN_TOKEN")).orNull
            }
        }
    }
}
