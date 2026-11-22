plugins {
    id("flagon.classlib")
    id("flagon.published")
}

classlib {
    module.set("java.net.http")
}

dependencies {
    api(project(":platform-api"))
    api(project(":runtime"))
}
