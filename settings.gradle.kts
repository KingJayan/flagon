rootProject.name = "flagon"

pluginManagement {
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

include(
    "platform-api",
    "platform-browser",
    "runtime",
    "lwjgl-core",
    "lwjgl2-compat",
    "lwjgl3-compat",
    "bundle-sandbox",
    "bundle-pausing",
    "toolchain:app-entry",
    "toolchain:awt-classlib",
    "toolchain:classfile",
    "toolchain:concurrent-classlib",
    "toolchain:crypto-classlib",
    "toolchain:jdk-classlib",
    "toolchain:jdk-shims",
    "toolchain:nio-classlib",
    "toolchain:reflect-policy",
    "toolchain:resource-pack",
    "toolchain:socket-classlib",
    "toolchain:sound-classlib",
)

if (file("vendor").listFiles()?.any { it.name.endsWith(".jar") } == true) {
    include("bundle-app")
}

includeBuild("teavm-patches")

include("example-app")
project(":example-app").projectDir = file("examples/app")

include("thirdparty-app")
project(":thirdparty-app").projectDir = file("examples/thirdparty")

include("bundle-example")
project(":bundle-example").projectDir = file("examples/bundle")
