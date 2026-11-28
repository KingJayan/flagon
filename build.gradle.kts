plugins {
    base
    // loaded here so that no change to build-logic reaches the teavm task classpath
    alias(libs.plugins.teavm) apply false
}

val bundleProjects = listOf(
    ":bundles:sandbox",
    ":bundles:pausing",
    ":bundle-example",
    ":bundles:app",
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
    from("modules/teavm/core/src/main/resources/org/teavm/backend/wasm/wasm-gc-module-runtime.js") {
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

tasks.register("test") {
    group = "verification"
    description = "run the tests of the teavm patches"
    dependsOn(patches.map { gradle.includedBuild("teavm-patches").task(":$it:test") })
}

// the published builds beside this one: the patched teavm and the bundle plugin
fun includedPublish(task: String) =
    patches.map { gradle.includedBuild("teavm-patches").task(":$it:$task") } +
        gradle.includedBuild("build-logic").task(":bundle-plugin:$task")

val publishAll = tasks.register("publish") {
    group = "publishing"
    description = "deploy every module to the package registry"
    dependsOn(includedPublish("publish"))
}

val publishLocal = tasks.register("publishToMavenLocal") {
    group = "publishing"
    description = "install every module in the local maven repository"
    dependsOn(includedPublish("publishToMavenLocal"))
}

subprojects {
    val module = this
    plugins.withId("flagon.published") {
        publishAll.configure { dependsOn(module.tasks.named("publish")) }
        publishLocal.configure { dependsOn(module.tasks.named("publishToMavenLocal")) }
    }
}
