import java.util.Properties

// the main build owns the version; this build only has to agree with it
val shared = Properties().apply { file("../gradle.properties").inputStream().use(::load) }

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = shared.getProperty("group")
    version = shared.getProperty("version")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        withSourcesJar()
    }

    /** the stock jar this module rebuilds, resolved alone */
    val stock = configurations.create("stock") { isTransitive = false }

    // the stock classes join this module's own output, which precedes them on every classpath
    val unpackStock = tasks.register<Sync>("unpackStock") {
        from(provider { stock.map { zipTree(it) } })
        into(layout.buildDirectory.dir("stock"))
    }

    extensions.getByType<SourceSetContainer>()["main"].output.dir(
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

    extensions.configure<PublishingExtension> {
        publications.create<MavenPublication>("maven") {
            from(components["java"])
        }
        repositories.maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/KingJayan/flagon")
            credentials {
                username = providers.gradleProperty("githubUser")
                    .orElse(providers.environmentVariable("MAVEN_USERNAME")).orNull
                password = providers.gradleProperty("githubToken")
                    .orElse(providers.environmentVariable("MAVEN_TOKEN")).orNull
            }
        }
    }
}
