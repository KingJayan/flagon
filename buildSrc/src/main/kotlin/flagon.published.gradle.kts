plugins {
    id("flagon.java")
    `maven-publish`
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
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
