package dev.flagon.build

import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

abstract class UntrackedArguments : CommandLineArgumentProvider {

    @get:Internal
    abstract val arguments: ListProperty<String>

    override fun asArguments(): Iterable<String> = arguments.get()
}
