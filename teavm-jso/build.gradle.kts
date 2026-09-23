description = "teavm-jso-impl 0.15.0, rebuilt with the JavaScript interop fixes Flagon needs"

dependencies {
    "stock"(libs.teavm.jso.impl)
    "compileOnly"(libs.teavm.jso.impl)
    "api"(libs.teavm.core)
    "api"(libs.teavm.jso)
    "api"(libs.teavm.rhino)
}
