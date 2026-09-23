plugins {
    id("flagon.published")
}

/** the stock jar this module rebuilds, resolved alone */
val stock: Configuration by configurations.creating {
    isTransitive = false
}

// the stock classes join this module's own output, which precedes them on every classpath,
// so a consumer sees one whole library with this module's fixes in it
val unpackStock by tasks.registering(Sync::class) {
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

// a consumer that reads loose class directories would miss the stock half, so hand it the jar
listOf("apiElements", "runtimeElements").forEach { name ->
    configurations.named(name) {
        outgoing.variants.removeIf { it.name == "classes" || it.name == "resources" }
    }
}

tasks.withType<Checkstyle>().configureEach {
    enabled = false
}
