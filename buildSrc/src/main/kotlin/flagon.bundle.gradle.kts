import dev.flagon.build.BundleExtension
import dev.flagon.build.CompilerLock
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.process.CommandLineArgumentProvider
import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.OptimizationLevel
import org.teavm.gradle.tasks.TeaVMTask

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
        outputDir.set(bundlesDir)
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
        outputDir.set(bundlesDir)
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
}

val writeCatalog = tasks.register<JavaExec>("writeCatalog") {
    description = "list the programs the bundle carries"
    val out = bundlesDir.file(bundle.bundleName.map { "$it.catalog.json" })
    classpath = mainSources.runtimeClasspath
    mainClass.set(bundle.catalogClass)
    outputs.file(out)
    outputs.cacheIf { true }
    argumentProviders.add(
        CommandLineArgumentProvider { listOf(out.get().asFile.absolutePath) },
    )
}

val writeResourcePack = tasks.register<JavaExec>("writeResourcePack") {
    description = "pack the files the bundle serves beside its classes"
    val out = bundlesDir.file(bundle.bundleName.map { "$it.pack" })
    classpath = mainSources.runtimeClasspath + bundleTools
    mainClass.set("dev.flagon.pack.WriteResourcePack")
    inputs.files(bundle.resourcesFrom).withPropertyName("resources").optional()
    outputs.file(out)
    outputs.cacheIf { true }
    val from = bundle.resourcesFrom
    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf(from.get().asFile.absolutePath, out.get().asFile.absolutePath)
        },
    )
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
