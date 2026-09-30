plugins {
    base
    // loaded here so that no change to build-logic reaches the teavm task classpath
    alias(libs.plugins.teavm) apply false
}

val bundleProjects = listOf(
    ":bundle-sandbox",
    ":bundle-pausing",
    ":bundle-example",
    ":bundle-app",
).filter { findProject(it) != null }

val bundleFull = configurations.dependencyScope("bundleFull")
val bundleFast = configurations.dependencyScope("bundleFast")
val bundlesFull = configurations.resolvable("bundlesFull") { extendsFrom(bundleFull.get()) }
val bundlesFast = configurations.resolvable("bundlesFast") { extendsFrom(bundleFast.get()) }

dependencies {
    bundleProjects.forEach {
        bundleFull(project(it, "bundleFull"))
        bundleFast(project(it, "bundleFast"))
    }
}

fun Sync.intoWeb(bundles: Provider<ResolvableConfiguration>) {
    group = "flagon"
    from(bundles)
    from("teavm/src/main/resources/org/teavm/backend/wasm/wasm-gc-module-runtime.js") {
        into("wasm")
    }
    into(layout.projectDirectory.dir("web/public/flagon"))
}

tasks.register<Sync>("bundles") {
    description = "compile every bundle to webassembly gc and to javascript"
    intoWeb(bundlesFull)
}

tasks.register<Sync>("bundlesFast") {
    description = "compile every bundle to webassembly gc only"
    intoWeb(bundlesFast)
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
