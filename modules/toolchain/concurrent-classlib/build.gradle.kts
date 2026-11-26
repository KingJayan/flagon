plugins {
    id("flagon.classlib")
    id("flagon.published")
}

dependencies {
    api(project(":platform:api"))
    api(project(":runtime"))
}
