plugins {
    id("flagon.published")
}

dependencies {
    api(project(":platform:api"))
}

val testFixtures = configurations.register("testFixtures") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val testJar = tasks.register<Jar>("testJar") {
    archiveClassifier.set("tests")
    from(sourceSets["test"].output)
}

artifacts {
    add(testFixtures.name, testJar)
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "lwjgl-core"
    artifact(testJar)
}

val shaderSources = layout.projectDirectory.dir("src/main/glsl")
val generatedShaders = layout.buildDirectory.dir("generated/sources/glsl")

val generateShaders = tasks.register("generateShaders") {
    val sources = shaderSources
    val target = generatedShaders
    inputs.dir(sources)
    outputs.dir(target)
    doLast {
        fun literal(name: String) = sources.file(name).asFile.readText()
            .replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val out = target.get().file("dev/flagon/lwjgl/internal/FixedFunctionShaders.java").asFile
        out.parentFile.mkdirs()
        out.writeText(
            """
            |package dev.flagon.lwjgl.internal;
            |
            |final class FixedFunctionShaders {
            |    static final String VERTEX = "${literal("fixed-function.vert")}";
            |    static final String FRAGMENT = "${literal("fixed-function.frag")}";
            |
            |    private FixedFunctionShaders() {
            |    }
            |}
            |""".trimMargin()
        )
    }
}

sourceSets.main { java.srcDir(generateShaders) }
