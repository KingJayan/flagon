package dev.flagon.build

import java.util.Properties

/** the versions this plugin was built with, which it resolves flagon and teavm at */
object FlagonVersions {
    private val versions = Properties().apply {
        FlagonVersions::class.java.getResourceAsStream("versions.properties")!!.use(::load)
    }

    val flagon: String = versions.getProperty("flagon")

    val teavm: String = versions.getProperty("teavm")
}
