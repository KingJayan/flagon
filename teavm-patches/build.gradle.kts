import java.util.Properties

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

    val stock = configurations.create("stock") { isTransitive = false }

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
