rootProject.name = "flagon"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// a project at :a:b lives in modules/a/b
fun module(path: String) {
    include(path)
    val parts = path.split(":")
    parts.indices.forEach { i ->
        val sub = parts.take(i + 1)
        project(":" + sub.joinToString(":")).projectDir = file("modules/" + sub.joinToString("/"))
    }
}

listOf(
    "platform:api",
    "platform:browser",
    "platform:wasi",
    "runtime",
    "lwjgl:core",
    "lwjgl:lwjgl2",
    "lwjgl:lwjgl3",
    "bundles:sandbox",
    "bundles:pausing",
    "bundles:wasi",
    "toolchain:app-entry",
    "toolchain:awt-classlib",
    "toolchain:classfile",
    "toolchain:concurrent-classlib",
    "toolchain:crypto-classlib",
    "toolchain:http-classlib",
    "toolchain:jdk-classlib",
    "toolchain:jdk-shims",
    "toolchain:js-types",
    "toolchain:nio-classlib",
    "toolchain:reflect-policy",
    "toolchain:resource-pack",
    "toolchain:socket-classlib",
    "toolchain:sound-classlib",
    "toolchain:time-classlib",
).forEach(::module)

if (file("vendor").listFiles()?.any { it.name.endsWith(".jar") } == true) {
    module("bundles:app")
}

includeBuild("modules/teavm/patches") {
    name = "teavm-patches"
}

include("example-app")
project(":example-app").projectDir = file("examples/app")

include("thirdparty-app")
project(":thirdparty-app").projectDir = file("examples/thirdparty")

include("bundle-example")
project(":bundle-example").projectDir = file("examples/bundle")
