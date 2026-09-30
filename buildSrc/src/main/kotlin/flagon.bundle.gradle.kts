import dev.flagon.build.BundleExtension
import dev.flagon.build.CompilerLock
import dev.flagon.build.RunMain
import org.gradle.accessors.dm.LibrariesForLibs
import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.OptimizationLevel
import org.teavm.gradle.tasks.TeaVMTask
import java.io.File

plugins {
    id("flagon.java")
    id("org.teavm")
}

val libs = the<LibrariesForLibs>()
val bundle = extensions.create<BundleExtension>("bundle")
val mainSources = the<SourceSetContainer>()["main"]

bundle.bundleName.convention(project.name.removePrefix("bundle-"))
bundle.resourcesFrom.convention(
    layout.dir(provider { mainSources.output.resourcesDir }),
)
bundle.reflectClasses.convention("")
bundle.reflectProxies.convention("")
bundle.reflectConverted.convention("")

val bundlesDir = rootProject.layout.projectDirectory.dir("web/public/flagon")
val releaseBuild = providers.gradleProperty("flagon.release").isPresent
val preserve = providers.gradleProperty("flagon.preserve").orElse("")

val teavmClasspath = configurations.named("teavmClasspath")
val bundleTools = configurations.create("bundleTools")

val teavmPatched = configurations.create("teavmPatched") {
    isTransitive = false
}

dependencies {
    bundleTools(project(":toolchain:resource-pack"))
    compileOnly(project(":toolchain:resource-pack"))
    compileOnly("dev.flagon:teavm-core-patched:${project.version}")
    compileOnly("dev.flagon:teavm-jso-impl-patched:${project.version}")

    add(teavmClasspath.name, "dev.flagon:teavm-core-patched:${project.version}")
    add(teavmClasspath.name, "dev.flagon:teavm-jso-impl-patched:${project.version}")
    add(teavmClasspath.name, project(":toolchain:resource-pack"))
    teavmPatched("dev.flagon:teavm-core-patched:${project.version}")
    teavmPatched("dev.flagon:teavm-jso-impl-patched:${project.version}")
}

teavmClasspath.configure {
    exclude(group = "org.teavm", module = "teavm-core")
    exclude(group = "org.teavm", module = "teavm-jso-impl")
}

val reflectionProperties = provider {
    mapOf(
        "flagon.reflect.classes" to bundle.reflectClasses.get(),
        "flagon.reflect.proxies" to bundle.reflectProxies.get(),
        "flagon.reflect.constructed" to
            bundle.reflectConstructed.orNull?.asFile?.absolutePath.orEmpty(),
        "flagon.reflect.converted" to bundle.reflectConverted.get(),
        "flagon.preserve" to preserve.get(),
    )
}

teavm {
    js {
        mainClass.set(bundle.mainClass)
        outputDir.set(bundlesDir.dir("js"))
        relativePathInOutputDir.set("")
        targetFileName.set(bundle.bundleName.map { "$it.js" })
        moduleType.set(JSModuleType.ES2015)
        optimization.set(OptimizationLevel.NONE)
        obfuscated.set(releaseBuild)
        strict.set(true)
        sourceMap.set(false)
        debugInformation.set(false)
        properties.putAll(reflectionProperties)
    }
    wasmGC {
        mainClass.set(bundle.mainClass)
        outputDir.set(bundlesDir.dir("wasm"))
        relativePathInOutputDir.set("")
        targetFileName.set(bundle.bundleName.map { "$it.wasm" })
        optimization.set(
            if (releaseBuild) OptimizationLevel.AGGRESSIVE else OptimizationLevel.NONE,
        )
        obfuscated.set(releaseBuild)
        strict.set(true)
        sourceMap.set(false)
        debugInformation.set(false)
        copyRuntime.set(false)
        properties.putAll(reflectionProperties)
    }
}

val compilerLock = gradle.sharedServices.registerIfAbsent("teavmCompiler", CompilerLock::class) {
    maxParallelUsages.set(1)
}

tasks.withType<TeaVMTask>().configureEach {
    usesService(compilerLock)
    val rest = classpath.from.toTypedArray()
    classpath.setFrom(teavmPatched, *rest)
    val listed = classpath
    properties.put(
        "flagon.classpath",
        provider { listed.files.joinToString(File.pathSeparator) { it.absolutePath } },
    )
}

val writeCatalog = tasks.register<RunMain>("writeCatalog") {
    description = "list the programs the bundle carries"
    val out = bundlesDir.file(bundle.bundleName.map { "$it.catalog.json" })
    runtimeClasspath.from(mainSources.runtimeClasspath)
    mainClass.set(bundle.catalogClass)
    outputs.file(out)
    outputs.cacheIf { true }
    arguments.add(out.map { it.asFile.absolutePath })
}

val writeResourcePack = tasks.register<RunMain>("writeResourcePack") {
    description = "pack the files the bundle serves beside its classes"
    val out = bundlesDir.file(bundle.bundleName.map { "$it.pack" })
    runtimeClasspath.from(mainSources.runtimeClasspath, bundleTools)
    mainClass.set("dev.flagon.pack.WriteResourcePack")
    inputs.files(bundle.resourcesFrom).withPropertyName("resources").optional()
    outputs.file(out)
    outputs.cacheIf { true }
    arguments.add(bundle.resourcesFrom.map { it.asFile.absolutePath })
    arguments.add(out.map { it.asFile.absolutePath })
}

tasks.register("bundle") {
    group = "flagon"
    description = "compile this module to webassembly gc and to javascript"
    dependsOn("generateWasmGC", "generateJavaScript", writeCatalog, writeResourcePack)
}

tasks.register("bundleFast") {
    group = "flagon"
    description = "compile this module to webassembly gc only"
    dependsOn("generateWasmGC", writeCatalog, writeResourcePack)
}
