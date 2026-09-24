plugins {
    base
}

val bundleProjects = listOf(
    ":bundle-sandbox",
    ":bundle-pausing",
    ":bundle-example",
)

val bundlesDir = layout.projectDirectory.dir("web/public/flagon")

val copyWasmRuntime = tasks.register<Copy>("copyWasmRuntime") {
    group = "flagon"
    description = "put the webassembly loader beside the bundles"
    from("teavm/src/main/resources/org/teavm/backend/wasm/wasm-gc-module-runtime.js")
    into(bundlesDir)
}

fun bundlesOf(task: String) =
    (bundleProjects + ":bundle-app").filter { findProject(it) != null }.map { "$it:$task" }

tasks.register("bundles") {
    group = "flagon"
    description = "compile every bundle to webassembly gc and to javascript"
    dependsOn(copyWasmRuntime)
    dependsOn(bundlesOf("bundle"))
}

tasks.register("bundlesFast") {
    group = "flagon"
    description = "compile every bundle to webassembly gc only"
    dependsOn(copyWasmRuntime)
    dependsOn(bundlesOf("bundleFast"))
}

tasks.register("lintJava") {
    group = "verification"
    description = "check java style in every module"
    dependsOn(
        subprojects
            .filter { it.buildFile.exists() }
            .flatMap { listOf("${it.path}:checkstyleMain", "${it.path}:checkstyleTest") },
    )
}

val patches = listOf("teavm-core-patched", "teavm-jso-impl-patched")

tasks.named("clean") {
    dependsOn(patches.map { gradle.includedBuild("teavm-patches").task(":$it:clean") })
}

val publishAll = tasks.register("publish") {
    group = "publishing"
    description = "deploy every module to the package registry"
    dependsOn(patches.map { gradle.includedBuild("teavm-patches").task(":$it:publish") })
}

subprojects {
    val module = this
    plugins.withId("flagon.published") {
        publishAll.configure { dependsOn(module.tasks.named("publish")) }
    }
}
