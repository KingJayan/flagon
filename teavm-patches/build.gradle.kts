import java.util.Properties

// a separate build reads no root gradle.properties. a provider keeps it tracked
val shared = providers.fileContents(
    layout.projectDirectory.dir("..").file("gradle.properties"),
).asText.map { text -> Properties().apply { load(text.reader()) } }

subprojects {
    group = shared.map { it.getProperty("group") }.get()
    version = shared.map { it.getProperty("version") }.get()
}
