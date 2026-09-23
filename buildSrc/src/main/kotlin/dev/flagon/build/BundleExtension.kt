package dev.flagon.build

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

interface BundleExtension {
    val bundleName: Property<String>

    val mainClass: Property<String>

    val catalogClass: Property<String>

    val resourcesFrom: DirectoryProperty

    val reflectClasses: Property<String>

    val reflectProxies: Property<String>

    val reflectConstructed: RegularFileProperty

    val reflectConverted: Property<String>
}
