import org.gradle.process.CommandLineArgumentProvider

plugins {
    id("flagon.bundle")
}

val vendorDir = rootProject.layout.projectDirectory.dir("vendor")

// merge vendor jars
val vendorJar = tasks.register<Zip>("vendorJar") {
    description = "fold the jars in vendor/ into one"
    from(
        provider {
            vendorDir.asFileTree.matching { include("*.jar") }.files.sorted().map { zipTree(it) }
        },
    )
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.EC")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(layout.buildDirectory.dir("vendor"))
    archiveFileName.set("app.jar")
}

val appEntry = configurations.create("appEntry")

dependencies {
    implementation(project(":runtime"))
    implementation(project(":platform-browser"))

    implementation(project(":lwjgl2-compat"))
    implementation(project(":lwjgl3-compat"))
    implementation(project(":toolchain:socket-classlib"))
    implementation(project(":toolchain:jdk-classlib"))
    implementation(project(":toolchain:nio-classlib"))
    implementation(project(":toolchain:awt-classlib"))
    implementation(project(":toolchain:sound-classlib"))
    implementation(project(":toolchain:crypto-classlib"))
    implementation(project(":toolchain:concurrent-classlib"))
    implementation(project(":toolchain:reflect-policy"))
    implementation(project(":toolchain:jdk-shims"))
    implementation(files(vendorJar))
    appEntry(project(":toolchain:app-entry"))
    compileOnly(project(":toolchain:app-entry"))
}

val generatedSources = layout.buildDirectory.dir("generated/app")
val constructed = layout.buildDirectory.file("app-classes.txt")

val writeAppEntry = tasks.register<JavaExec>("writeAppEntry") {
    description = "write the entry point from what the jars in vendor/ say about themselves"
    classpath = appEntry
    mainClass.set("dev.flagon.appentry.WriteAppEntry")
    inputs.files(vendorDir.asFileTree.matching { include("*.jar") }).withPropertyName("vendor")
    outputs.dir(generatedSources)
    outputs.file(constructed)
    outputs.cacheIf { true }

    val vendor = vendorDir.asFile.absolutePath
    val generated = generatedSources
    val classList = constructed
    val reflect = providers.gradleProperty("flagon.app.reflect").orElse("")
    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                vendor,
                generated.get().asFile.absolutePath,
                classList.get().asFile.absolutePath,
                reflect.get(),
            )
        },
    )
}

sourceSets["main"].java.srcDir(writeAppEntry.map { generatedSources })

bundle {
    mainClass.set("dev.flagon.app.AppMain")
    catalogClass.set("dev.flagon.app.AppCatalog")
    resourcesFrom.set(vendorDir)
    reflectConstructed.set(constructed)
    reflectConverted.set(
        "java.lang.Boolean, java.lang.Byte, java.lang.Short, java.lang.Integer, "
            + "java.lang.Long, java.lang.Float, java.lang.Double, java.lang.String, java.io.File",
    )
}
