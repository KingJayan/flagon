package dev.flagon.sandbox;

import dev.flagon.platform.browser.BrowserPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.ProgramRegistry;

/**
 entry point of sandbox bundle
 */
public final class SandboxMain {

    private SandboxMain() {
    }

    public static void main(String[] arguments) {
        FlagonRuntime.boot(BrowserPlatform.create(arguments), registry());
    }

    public static ProgramRegistry registry() {
        return new ProgramRegistry()
                .register(new HelloProgram())
                .register(new ChattyProgram())
                .register(new RuntimeSelfTestProgram())
                .register(new ShutdownHookProgram())
                .register(new ClassLoaderProgram())
                .register(new ResourceProgram())
                .register(new JdkGapsProgram())
                .register(new ImageProgram())
                .register(new CryptoProgram())
                .register(new LwjglDemoProgram())
                .register(new Lwjgl3DemoProgram())
                .register(new GlfwDemoProgram())
                .register(new CoreProfileDemoProgram())
                .register(new CoreClassesDemoProgram())
                .register(new FramebufferDemoProgram())
                .register(new ThreadingDemoProgram())
                .register(new InputDemoProgram())
                .register(new DisplayDemoProgram())
                .register(new VertexArrayDemoProgram())
                .register(new ShaderDemoProgram())
                .register(new FixedFunctionShaderProgram())
                .register(new HalfShaderProgram())
                .register(new TextureDemoProgram())
                .register(new FileSystemDemoProgram())
                .register(new AudioDemoProgram())
                .register(new NetDemoProgram());
    }
}
