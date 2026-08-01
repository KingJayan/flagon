package dev.flagon.example;

import dev.flagon.platform.browser.BrowserPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.MainProgram;
import dev.flagon.runtime.ProgramRegistry;
import org.example.blocks.BlocksGame;
import org.example.spinner.SpinnerApp;

/**
 * the whole of what a bundle for an application jar has to say: which host, and which main.
 *
 * <p>{@code SpinnerApp} is a class out of a jar that was compiled against the real lwjgl and
 * has no idea this file exists. nothing here patches it — the compatibility layer supplies
 * {@code org.lwjgl.*} on the classpath in place of the artifact it was built with.
 */
public final class ExampleMain {

    private ExampleMain() {
    }

    public static void main(String[] arguments) {
        ProgramRegistry registry = new ProgramRegistry()
                .register(new MainProgram("spinner", "the example application", SpinnerApp::main))
                .register(new MainProgram("blocks", "the example game", BlocksGame::main));
        FlagonRuntime.boot(BrowserPlatform.create(arguments), registry);
    }
}
