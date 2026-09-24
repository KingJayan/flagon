plugins {
    id("flagon.published")
}

val stock = configurations.create("stock") {
    isTransitive = false
}

val unpackStock = tasks.register<Sync>("unpackStock") {
    from(provider { stock.map { zipTree(it) } })
    into(layout.buildDirectory.dir("stock"))
}

the<SourceSetContainer>()["main"].output.dir(
    mapOf("builtBy" to unpackStock),
    layout.buildDirectory.dir("stock"),
)

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

listOf("apiElements", "runtimeElements").forEach { name ->
    configurations.named(name) {
        outgoing.variants.removeIf { it.name == "classes" || it.name == "resources" }
    }
}

tasks.withType<Checkstyle>().configureEach {
    enabled = false
}
