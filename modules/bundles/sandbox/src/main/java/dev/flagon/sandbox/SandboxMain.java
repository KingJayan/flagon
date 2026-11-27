package dev.flagon.sandbox;

import dev.flagon.platform.browser.BrowserPlatform;
import dev.flagon.runtime.FlagonRuntime;
import dev.flagon.runtime.ProgramRegistry;
import org.teavm.interop.Export;
import org.teavm.jso.JSExport;

/**
 entry point of sandbox bundle
 */
public final class SandboxMain {

    private SandboxMain() {
    }

    /** a wasm page calls this as __flagonExports.add, with no javascript between */
    @Export(name = "add")
    public static int add(int a, int b) {
        return a + b;
    }

    /** the javascript module exports this, and its typescript declarations name it */
    @JSExport
    public static String greet(String name) {
        return "Hello, " + name + "!";
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
                .register(new GraphicsProgram())
                .register(new CryptoProgram())
                .register(new HttpProgram())
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
                .register(new NetDemoProgram())
                .register(new CompilerShapesProgram())
                .register(new SerialProgram())
                .register(new ConcurrencyProgram())
                .register(new LocaleProgram())
                .register(new WebApisProgram())
                .register(new WorkersProgram())
                .register(new WorkersProgram.Echo());
    }
}
