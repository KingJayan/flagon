package dev.flagon.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

interface RunMainParameters : WorkParameters {
    val mainClass: Property<String>

    val arguments: ListProperty<String>
}

abstract class RunMainAction : WorkAction<RunMainParameters> {
    override fun execute() {
        val main = Class.forName(parameters.mainClass.get())
            .getMethod("main", Array<String>::class.java)
        main.invoke(null, parameters.arguments.get().toTypedArray())
    }
}

/** calls a main class in the daemon, under a class loader of its own */
abstract class RunMain : DefaultTask() {

    @get:Inject
    abstract val workers: WorkerExecutor

    @get:Classpath
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:Input
    abstract val mainClass: Property<String>

    @get:Input
    abstract val arguments: ListProperty<String>

    @TaskAction
    fun run() {
        workers.classLoaderIsolation { classpath.from(runtimeClasspath) }
            .submit(RunMainAction::class.java) {
                mainClass.set(this@RunMain.mainClass)
                arguments.set(this@RunMain.arguments)
            }
    }
}
