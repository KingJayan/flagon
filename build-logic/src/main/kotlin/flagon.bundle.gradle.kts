import dev.flagon.build.BundleExtension
import dev.flagon.build.RunMain
import dev.flagon.build.WASM_FEATURES
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.process.CommandLineArgumentProvider
import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.OptimizationLevel
import org.teavm.gradle.tasks.GenerateWasmGCTask
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
bundle.compilerMemory.convention(1024)
bundle.shrinkCoroutineWasm.convention(true)

val bundlesDir = layout.buildDirectory.dir("flagon")
val releaseBuild = providers.gradleProperty("flagon.release").isPresent
// release wasm goes through wasm-opt before it lands beside the other outputs
fun shrinks(variant: String): Provider<Boolean> =
    if (variant == "wasm") bundle.shrinkCoroutineWasm.map { it && releaseBuild } else provider { releaseBuild }

fun wasmDir(variant: String) = shrinks(variant).flatMap {
    if (it) layout.buildDirectory.dir("teavm/$variant") else bundlesDir.map { dir -> dir.dir(variant) }
}
val preserve = providers.gradleProperty("flagon.preserve").orElse("")

val teavmClasspath = configurations.named("teavmClasspath")
val bundleTools = configurations.create("bundleTools")

val teavmPatched = configurations.create("teavmPatched") {
    isTransitive = false
}

val teavmCompiler = configurations.create("teavmCompiler") {
    exclude(group = "org.teavm", module = "teavm-core")
    exclude(group = "org.teavm", module = "teavm-jso-impl")
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

    teavmCompiler("org.teavm:teavm-tooling:${libs.versions.teavm.get()}")
    teavmCompiler("dev.flagon:teavm-core-patched:${project.version}")
    teavmCompiler("dev.flagon:teavm-jso-impl-patched:${project.version}")
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
        outputDir.set(bundlesDir.map { it.dir("js") })
        relativePathInOutputDir.set("")
        targetFileName.set(bundle.bundleName.map { "$it.js" })
        moduleType.set(JSModuleType.ES2015)
        optimization.set(
            if (releaseBuild) OptimizationLevel.BALANCED else OptimizationLevel.NONE,
        )
        obfuscated.set(releaseBuild)
        strict.set(true)
        sourceMap.set(false)
        debugInformation.set(false)
        properties.putAll(reflectionProperties)
    }
    wasmGC {
        mainClass.set(bundle.mainClass)
        outputDir.set(wasmDir("wasm"))
        relativePathInOutputDir.set("")
        targetFileName.set(bundle.bundleName.map { "$it.wasm" })
        optimization.set(
            if (releaseBuild) OptimizationLevel.BALANCED else OptimizationLevel.NONE,
        )
        obfuscated.set(releaseBuild)
        strict.set(true)
        sourceMap.set(false)
        debugInformation.set(false)
        copyRuntime.set(false)
        properties.putAll(reflectionProperties)
    }
}

val generateWasmGC = tasks.named<GenerateWasmGCTask>("generateWasmGC")

// the same program again, for browsers that park the wasm stack with jspi
val generateWasmJspi = tasks.register<GenerateWasmGCTask>("generateWasmJspi") {
    description = "compile this module to webassembly gc that pauses with jspi"
    classpath.from(generateWasmGC.map { it.classpath })
    mainClass.set(generateWasmGC.flatMap { it.mainClass })
    outputDir.set(wasmDir("jspi").map { it.asFile })
    targetFileName.set(bundle.bundleName.map { "$it.wasm" })
    optimization.set(generateWasmGC.flatMap { it.optimization })
    obfuscated.set(generateWasmGC.flatMap { it.obfuscated })
    strict.set(generateWasmGC.flatMap { it.strict })
    sourceMap.set(false)
    debugInformation.set(false)
    properties.putAll(generateWasmGC.flatMap { it.properties })
    properties.put("flagon.jspi", "true")
}

tasks.withType<TeaVMTask>().configureEach {
    outOfProcess.set(true)
    processMemory.set(bundle.compilerMemory)
    daemonClasspath.setFrom(teavmCompiler)
    inputs.files(bundle.reflectConstructed).withPropertyName("reflectConstructed").optional()
    outputs.cacheIf { true }
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
    val out = layout.buildDirectory.file(bundle.bundleName.map { "flagon/$it.catalog.json" })
    runtimeClasspath.from(mainSources.runtimeClasspath)
    mainClass.set(bundle.catalogClass)
    outputs.file(out)
    outputs.cacheIf { true }
    arguments.add(out.map { it.asFile.absolutePath })
}

val writeResourcePack = tasks.register<RunMain>("writeResourcePack") {
    description = "pack the files the bundle serves beside its classes"
    val out = layout.buildDirectory.file(bundle.bundleName.map { "flagon/$it.pack" })
    runtimeClasspath.from(mainSources.runtimeClasspath, bundleTools)
    mainClass.set("dev.flagon.pack.WriteResourcePack")
    inputs.files(bundle.resourcesFrom).withPropertyName("resources").optional()
    outputs.file(out)
    outputs.cacheIf { true }
    arguments.add(bundle.resourcesFrom.map { it.asFile.absolutePath })
    arguments.add(out.map { it.asFile.absolutePath })
}

fun optimizing(variant: String, compile: TaskProvider<GenerateWasmGCTask>): Provider<out Task> {
    if (!releaseBuild) {
        return compile
    }
    val optimize = tasks.register<Exec>("optimize${variant.replaceFirstChar { it.uppercase() }}") {
        description = "shrink the release $variant webassembly with binaryen"
        val web = rootProject.layout.projectDirectory.dir("web")
        val wasmOpt = web.file("node_modules/.bin/wasm-opt").asFile
        val name = bundle.bundleName.map { "$it.wasm" }
        val emitted = wasmDir(variant).zip(name) { dir, file -> dir.file(file) }
        val optimized = bundlesDir.zip(name) { dir, file -> dir.dir(variant).file(file) }
        dependsOn(compile)
        inputs.file(emitted).withPropertyName("emitted")
        inputs.file(web.file("node_modules/binaryen/package.json")).withPropertyName("binaryen")
            .optional()
        outputs.file(optimized)
        outputs.cacheIf { true }
        executable = wasmOpt.absolutePath
        argumentProviders.add(CommandLineArgumentProvider {
            listOf(emitted.get().asFile.absolutePath, "-o", optimized.get().asFile.absolutePath) +
                WASM_FEATURES + "-Os"
        })
        doFirst {
            check(wasmOpt.exists()) { "wasm-opt is missing. Run make deps." }
        }
    }
    return shrinks(variant).flatMap { if (it) optimize else compile }
}

val built = listOf(
    writeCatalog,
    writeResourcePack,
    optimizing("wasm", generateWasmGC),
    optimizing("jspi", generateWasmJspi),
)

configurations.consumable("bundleFull")
configurations.consumable("bundleFast")

artifacts {
    add("bundleFull", bundlesDir) { builtBy(built, "generateJavaScript") }
    add("bundleFast", bundlesDir) { builtBy(built) }
}

tasks.register("bundle") {
    group = "flagon"
    description = "compile this module to webassembly gc and to javascript"
    dependsOn(built, "generateJavaScript")
}

tasks.register("bundleFast") {
    group = "flagon"
    description = "compile this module to webassembly gc only"
    dependsOn(built)
}
