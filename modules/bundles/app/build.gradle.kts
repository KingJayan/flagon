import dev.flagon.build.UntrackedArguments

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
    implementation(project(":platform:browser"))

    implementation(project(":lwjgl:lwjgl2"))
    implementation(project(":lwjgl:lwjgl3"))
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
val generatedResources = layout.buildDirectory.dir("generated/appResources")
val constructed = generatedResources.map { it.file("META-INF/flagon/constructed.txt") }

val writeAppEntry = tasks.register<JavaExec>("writeAppEntry") {
    description = "write the entry point from what the jars in vendor/ say about themselves"
    classpath = appEntry
    mainClass.set("dev.flagon.appentry.WriteAppEntry")
    inputs.files(vendorDir.asFileTree.matching { include("*.jar") }).withPropertyName("vendor")
        .withPathSensitivity(PathSensitivity.NAME_ONLY)
    inputs.files(vendorDir.asFileTree.matching { include("assets/indexes/*.json") })
        .withPropertyName("assetIndexes").withPathSensitivity(PathSensitivity.NAME_ONLY)
    outputs.dir(generatedSources)
    outputs.dir(generatedResources)
    outputs.cacheIf { true }

    val reflect = providers.gradleProperty("flagon.app.reflect").orElse("")
    inputs.property("reflect", reflect)
    argumentProviders.add(objects.newInstance<UntrackedArguments>().apply {
        arguments.set(
            generatedSources.zip(constructed) { generated, classList ->
                listOf(
                    vendorDir.asFile.absolutePath,
                    generated.asFile.absolutePath,
                    classList.asFile.absolutePath,
                )
            }.zip(reflect) { paths, narrowing -> paths + narrowing },
        )
    })
}

sourceSets["main"].java.srcDir(writeAppEntry.map { generatedSources })
sourceSets["main"].resources.srcDir(writeAppEntry.map { generatedResources })

bundle {
    mainClass.set("dev.flagon.app.AppMain")
    catalogClass.set("dev.flagon.app.AppCatalog")
    resourcesFrom.set(vendorDir)
    compilerMemory.set(4096)
    shrinkCoroutineWasm.set(false)
    vorbisQuality.set(0)
    apartFrom.set(256 * 1024)
    reflectConverted.set(
        "java.lang.Boolean, java.lang.Byte, java.lang.Short, java.lang.Integer, "
            + "java.lang.Long, java.lang.Float, java.lang.Double, java.lang.String, java.io.File",
    )
}
