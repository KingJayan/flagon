package dev.flagon.build

import org.gradle.api.provider.Property

abstract class ClasslibExtension {
    abstract val module: Property<String>
}
