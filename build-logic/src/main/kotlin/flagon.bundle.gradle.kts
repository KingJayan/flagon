// dev.flagon.bundle as this repository builds it: its java conventions, the binaryen from web/,
// the app's lists in vendor/, and the outputs the root copies into the site
plugins {
    id("flagon.java")
    id("dev.flagon.bundle")
}

// apart from the published modules, so :bundles:wasi and :platform:wasi are two modules
group = "dev.flagon.bundles"

val web = rootProject.layout.projectDirectory.dir("web")
val bundle = the<dev.flagon.build.BundleExtension>()
bundle.wasmOpt.set(web.file("node_modules/.bin/wasm-opt"))

tasks.withType<Exec>().matching { it.name.startsWith("optimize") }.configureEach {
    // the npm wasm-opt is a script that stays the same across binaryen versions
    inputs.file(web.file("node_modules/binaryen/package.json")).withPropertyName("binaryen")
        .withPathSensitivity(PathSensitivity.NONE).optional()
    val wasmOpt = web.file("node_modules/.bin/wasm-opt").asFile
    doFirst { check(wasmOpt.exists()) { "wasm-opt is missing. Run make deps." } }
}

val bundlesDir = layout.buildDirectory.dir("flagon")

configurations.consumable("bundleFull")
configurations.consumable("bundleFast")

artifacts {
    add("bundleFull", bundlesDir) { builtBy("bundle") }
    add("bundleFull", layout.buildDirectory.dir("types")) { builtBy("bundle") }
    add("bundleFast", bundlesDir) { builtBy("bundleFast") }
}
