import dev.flagon.build.BundleExtension
import dev.flagon.build.DownloadFile
import dev.flagon.build.WASI_ADAPTER_SHA256
import dev.flagon.build.WASI_ADAPTER_URL
import dev.flagon.build.RunMain
import dev.flagon.build.UntrackedArguments
import dev.flagon.build.WASM_FEATURES
import dev.flagon.build.wasmTools
import org.gradle.accessors.dm.LibrariesForLibs
import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.OptimizationLevel
import org.teavm.gradle.api.SourceFilePolicy
import org.teavm.gradle.tasks.GenerateJavaScriptTask
import org.teavm.gradle.tasks.GenerateWasmGCTask
import org.teavm.gradle.tasks.TeaVMTask

plugins {
    id("flagon.java")
    id("org.teavm")
}

val libs = the<LibrariesForLibs>()
// apart from the published modules, so :bundles:wasi and :platform:wasi are two modules
group = "dev.flagon.bundles"

val bundle = extensions.create<BundleExtension>("bundle")
val mainSources = the<SourceSetContainer>()["main"]

// keeps parameter names, which reflection and the typescript declarations read back
tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-parameters")
}

bundle.bundleName.convention(project.name.removePrefix("bundle-"))
bundle.resourcesFrom.convention(
    layout.dir(provider { mainSources.output.resourcesDir }),
)
bundle.reflectClasses.convention("")
bundle.reflectProxies.convention("")
bundle.reflectConverted.convention("")
bundle.compilerMemory.convention(1024)
bundle.shrinkCoroutineWasm.convention(true)
bundle.apartFrom.convention(0)

val bundlesDir = layout.buildDirectory.dir("flagon")
val releaseBuild = providers.gradleProperty("flagon.release").isPresent
// wasm with names, a source map and the java sources beside it, for browser devtools
val debugBuild = providers.gradleProperty("flagon.debug").isPresent
// release wasm goes through wasm-opt before it lands beside the other outputs, which would drop the debug info
val shrinking = releaseBuild && !debugBuild
fun shrinks(variant: String): Provider<Boolean> =
    if (variant == "wasm") bundle.shrinkCoroutineWasm.map { it && shrinking } else provider { shrinking }

fun wasmDir(variant: String) = shrinks(variant).flatMap {
    if (it) layout.buildDirectory.dir("teavm/$variant") else bundlesDir.map { dir -> dir.dir(variant) }
}
// one glob per line in a vendor/ file, joined the way the compiler properties take them
fun vendorGlobs(name: String): Provider<String> =
    providers.fileContents(rootProject.layout.projectDirectory.file("vendor/$name")).asText
        .map { text ->
            text.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
                .joinToString(",")
        }.orElse("")
val preserve = providers.gradleProperty("flagon.preserve").orElse(vendorGlobs("preserve.txt"))
val reflected = bundle.reflectClasses.zip(vendorGlobs("reflect.txt")) { own, vendor ->
    listOf(own, vendor).filter { it.isNotBlank() }.joinToString(",")
}

val teavmClasspath = configurations.named("teavmClasspath")
val bundleTools = configurations.create("bundleTools")
val typesTool = configurations.create("typesTool")

val teavmPatched = configurations.create("teavmPatched") {
    isTransitive = false
}

val teavmCompiler = configurations.create("teavmCompiler") {
    exclude(group = "org.teavm", module = "teavm-core")
    exclude(group = "org.teavm", module = "teavm-jso-impl")
}

dependencies {
    bundleTools(project(":toolchain:resource-pack"))
    typesTool(project(":toolchain:js-types"))
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
        "flagon.reflect.classes" to reflected.get(),
        "flagon.reflect.proxies" to bundle.reflectProxies.get(),
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
        obfuscated.set(releaseBuild && !debugBuild)
        strict.set(true)
        sourceMap.set(debugBuild)
        sourceFilePolicy.set(SourceFilePolicy.COPY)
        debugInformation.set(false)
        copyRuntime.set(false)
        properties.putAll(reflectionProperties)
    }
}

val generateWasmGC = tasks.named<GenerateWasmGCTask>("generateWasmGC")

// sources only feed source maps, which only debug builds write, and teavm keys them by absolute path
if (!debugBuild) {
    tasks.withType<GenerateWasmGCTask>().configureEach { sourceFiles.setFrom() }
}
tasks.withType<GenerateJavaScriptTask>().configureEach { sourceFiles.setFrom() }

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
    sourceMap.set(generateWasmGC.flatMap { it.sourceMap })
    sourceFilePolicy.set(generateWasmGC.flatMap { it.sourceFilePolicy })
    sourceFiles.from(generateWasmGC.map { it.sourceFiles })
    debugInformation.set(false)
    properties.putAll(generateWasmGC.flatMap { it.properties })
    properties.put("flagon.jspi", "true")
}

tasks.withType<TeaVMTask>().configureEach {
    outOfProcess.set(true)
    processMemory.set(bundle.compilerMemory)
    daemonClasspath.setFrom(teavmCompiler)
    outputs.cacheIf { true }
    val rest = classpath.from.toTypedArray()
    classpath.setFrom(teavmPatched, *rest)
    // a debug build leaves a source map and sources behind, which a later build would ship
    val out = outputDir
    doFirst { out.get().deleteRecursively() }
}

val writeCatalog = tasks.register<RunMain>("writeCatalog") {
    description = "list the programs the bundle carries"
    val out = layout.buildDirectory.file(bundle.bundleName.map { "flagon/$it.catalog.json" })
    runtimeClasspath.from(mainSources.runtimeClasspath)
    mainClass.set(bundle.catalogClass)
    outputs.file(out)
    outputs.cacheIf { true }
    paths.add(out)
}

val writeResourcePack = tasks.register<RunMain>("writeResourcePack") {
    description = "pack the files the bundle serves beside its classes"
    val out = layout.buildDirectory.file(bundle.bundleName.map { "flagon/$it.pack" })
    runtimeClasspath.from(mainSources.runtimeClasspath, bundleTools)
    mainClass.set("dev.flagon.pack.WriteResourcePack")
    inputs.files(bundle.resourcesFrom).withPropertyName("resources").optional()
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(out)
    outputs.dir(layout.buildDirectory.dir(bundle.bundleName.map { "flagon/$it.files" }))
    outputs.cacheIf { true }
    paths.add(bundle.resourcesFrom)
    paths.add(out)
    paths.add(layout.buildDirectory.dir("transcoded"))
    arguments.add(bundle.apartFrom.map { "--apart-from=$it" })
    val apart = vendorGlobs("apart.txt")
    inputs.property("apart", apart)
    arguments.add(apart.map { "--apart=$it" })
    arguments.addAll(bundle.vorbisQuality.map { listOf("--vorbis-quality=$it") }.orElse(listOf()))
}

val typesDir = layout.buildDirectory.dir("types")
val writeTypes = tasks.register<RunMain>("writeTypes") {
    description = "declare the javascript module's exports for typescript"
    val out = typesDir.zip(bundle.bundleName) { dir, name -> dir.file("js/$name.d.ts") }
    runtimeClasspath.from(mainSources.runtimeClasspath, typesTool)
    mainClass.set("dev.flagon.types.WriteTypes")
    outputs.file(out)
    outputs.cacheIf { true }
    paths.add(out)
    arguments.add(bundle.mainClass)
}

fun optimizing(variant: String, compile: TaskProvider<GenerateWasmGCTask>): Provider<out Task> {
    if (!shrinking) {
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
        inputs.file(emitted).withPropertyName("emitted").withPathSensitivity(PathSensitivity.NONE)
        inputs.file(web.file("node_modules/binaryen/package.json")).withPropertyName("binaryen")
            .withPathSensitivity(PathSensitivity.NONE).optional()
        inputs.property("features", WASM_FEATURES)
        outputs.file(optimized)
        outputs.cacheIf { true }
        executable = wasmOpt.absolutePath
        argumentProviders.add(objects.newInstance<UntrackedArguments>().apply {
            arguments.set(
                emitted.zip(optimized) { from, to ->
                    listOf(from.asFile.absolutePath, "-o", to.asFile.absolutePath) +
                        WASM_FEATURES + "-Os"
                },
            )
        })
        doFirst {
            check(wasmOpt.exists()) { "wasm-opt is missing. Run make deps." }
            optimized.get().asFile.parentFile.run { deleteRecursively(); mkdirs() }
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
    add("bundleFull", typesDir) { builtBy(writeTypes) }
    add("bundleFast", bundlesDir) { builtBy(built) }
}

tasks.register("bundle") {
    group = "flagon"
    description = "compile this module to webassembly gc and to javascript"
    dependsOn(built, "generateJavaScript", writeTypes)
}

tasks.register("bundleFast") {
    group = "flagon"
    description = "compile this module to webassembly gc only"
    dependsOn(built)
}

// a wasi preview 2 component: webassembly gc that imports wasi in place of javascript
val wasiDir = layout.buildDirectory.dir("wasi")
val generateWasi = tasks.register<GenerateWasmGCTask>("generateWasi") {
    description = "compile this module to a webassembly gc core module that imports wasi preview 1"
    classpath.from(generateWasmGC.map { it.classpath })
    mainClass.set(bundle.wasiMainClass.orElse(bundle.mainClass))
    outputDir.set(layout.buildDirectory.dir("teavm/wasi").map { it.asFile })
    targetFileName.set(bundle.bundleName.map { "$it.core.wasm" })
    optimization.set(generateWasmGC.flatMap { it.optimization })
    // wasm-tools refuses the name section teavm writes, and wasi prints no stack anyway
    obfuscated.set(true)
    strict.set(generateWasmGC.flatMap { it.strict })
    sourceMap.set(false)
    debugInformation.set(false)
    properties.putAll(generateWasmGC.flatMap { it.properties })
    properties.put("flagon.wasi", "true")
}

val wasiAdapter = tasks.register<DownloadFile>("wasiAdapter") {
    description = "fetch the wasmtime adapter from wasi preview 1 to preview 2"
    url.set(WASI_ADAPTER_URL)
    sha256.set(WASI_ADAPTER_SHA256)
    output.set(layout.buildDirectory.file("wasi-adapter/wasi_snapshot_preview1.command.wasm"))
}

val wasmToolsPath = providers.gradleProperty("flagon.wasmTools")
val core = generateWasi.flatMap { task ->
    task.outputDir.zip(task.targetFileName) { dir, name -> File(dir, name) }
}
val embedded = layout.buildDirectory.file("teavm/wasi/embedded.wasm")
val embedWasi = tasks.register<Exec>("embedWasi") {
    description = "embed the wit world of the module's @Export methods"
    dependsOn(generateWasi)
    inputs.files(generateWasi).withPropertyName("core").withPathSensitivity(PathSensitivity.NONE)
    outputs.file(embedded)
    argumentProviders.add(objects.newInstance<UntrackedArguments>().apply {
        arguments.set(core.zip(embedded) { wasm, out ->
            listOf("component", "embed", wasm.path.removeSuffix(".wasm") + ".wit", wasm.path,
                "--world", "bundle", "-o", out.asFile.path)
        })
    })
    val tools = wasmToolsPath
    doFirst { executable = wasmTools(tools.orNull) }
}

tasks.register<Exec>("wasi") {
    group = "flagon"
    description = "build this module as a wasi preview 2 component, with no javascript in it"
    dependsOn(embedWasi, wasiAdapter)
    val component = wasiDir.zip(bundle.bundleName) { dir, name -> dir.file("$name.wasm") }
    inputs.files(embedWasi).withPropertyName("embedded").withPathSensitivity(PathSensitivity.NONE)
    inputs.files(wasiAdapter).withPropertyName("adapter").withPathSensitivity(PathSensitivity.NONE)
    outputs.file(component)
    argumentProviders.add(objects.newInstance<UntrackedArguments>().apply {
        arguments.set(component.zip(wasiAdapter.flatMap { it.output }) { out, adapter ->
            listOf("--adapt", "wasi_snapshot_preview1=" + adapter.asFile.path, "-o",
                out.asFile.path)
        }.zip(embedded) { rest, from -> listOf("component", "new", from.asFile.path) + rest })
    })
    val tools = wasmToolsPath
    doFirst { executable = wasmTools(tools.orNull) }
}
