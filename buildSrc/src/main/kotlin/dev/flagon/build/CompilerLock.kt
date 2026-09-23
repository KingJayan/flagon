package dev.flagon.build

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/** one teavm compilation at a time, because each one wants gigabytes of the daemon's heap */
abstract class CompilerLock : BuildService<BuildServiceParameters.None>
