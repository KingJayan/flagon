package dev.flagon.sandbox;

import dev.flagon.platform.browser.BrowserPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.ProgramRegistry;

/**
 * Entry point of the sandbox bundle.
 *
 * <p>This is the only class in the project that knows both that the host is a browser and which
 * programs exist — the composition root. Everything it wires together is reusable as-is by a
 * game bundle; only the registry contents change.
 */
public final class SandboxMain {

    private SandboxMain() {
    }

    public static void main(String[] arguments) {
        ProgramRegistry registry = new ProgramRegistry()
                .register(new HelloProgram())
                .register(new RuntimeSelfTestProgram());
        FlagonRuntime.boot(BrowserPlatform.create(arguments), registry);
    }
}
