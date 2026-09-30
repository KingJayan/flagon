import dev.flagon.build.replaces

plugins {
    id("flagon.toolchain")
    id("flagon.publishing")
}

val stock = configurations.create("stock") {
    isTransitive = false
}

val main = the<SourceSetContainer>()["main"]

val unpackStock = tasks.register<Sync>("unpackStock") {
    val java: FileTree = main.java
    val resources: FileTree = main.resources
    from(provider { stock.map { zipTree(it) } })
    exclude { replaces(it.relativePath.pathString, java, resources) }
    inputs.files(java, resources).withPropertyName("patches")
    into(layout.buildDirectory.dir("stock"))
}

main.output.dir(
    mapOf("builtBy" to unpackStock),
    layout.buildDirectory.dir("stock"),
)

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

listOf("apiElements", "runtimeElements").forEach { name ->
    configurations.named(name) {
        outgoing.variants.removeIf { it.name == "classes" || it.name == "resources" }
    }
}
