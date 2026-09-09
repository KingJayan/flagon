package dev.flagon.example;

import dev.flagon.platform.browser.BrowserPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.MainProgram;
import dev.flagon.runtime.ProgramRegistry;
import org.example.blocks.BlocksGame;
import org.example.spinner.SpinnerApp;
import org.lwjgl.guide.HelloWorld;

//the whole of what a bundle for an application jar has to say: which host, and which main

public final class ExampleMain {

    private ExampleMain() {}

    public static void main(String[] arguments) {
        FlagonRuntime.boot(BrowserPlatform.create(arguments), registry());
    }

    public static ProgramRegistry registry() {
        return new ProgramRegistry()
                .register(new MainProgram("spinner", "the example application", SpinnerApp::main))
                .register(new MainProgram("blocks", "the example game", BlocksGame::main))
                .register(new MainProgram("hello-lwjgl3",
                        "LWJGL's own guide sample, built against the real LWJGL 3",
                        HelloWorld::main));
    }
}
