import java.util.Properties

// a separate build reads no gradle.properties of the root
val shared = Properties().apply { file("../gradle.properties").inputStream().use(::load) }

subprojects {
    group = shared.getProperty("group")
    version = shared.getProperty("version")
}
