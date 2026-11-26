package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

public final class SendRuntimeCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String RUNTIME = "java.lang.Runtime";
    private static final String RUNTIMES = "dev.flagon.platform.Runtimes";
    private static final String SYSTEM = "java.lang.System";
    private static final String SYSTEMS = "dev.flagon.platform.Systems";

    private static final Map<String, String> FROM_RUNTIME = Map.of(
            "maxMemory", "maxMemory",
            "exec", "exec");

    private static final Map<String, String> FROM_SYSTEM = Map.of(
            "exit", "exit",
            "loadLibrary", "loadLibrary",
            "load", "load",
            "mapLibraryName", "mapLibraryName");

    private static final String[] NOT_OURS = {"org.teavm.", RUNTIMES, SYSTEMS,
        "dev.flagon.platform.ShutdownHooks"};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, RUNTIME, RUNTIMES, FROM_RUNTIME, NOT_OURS);
        Reroute.apply(type, SYSTEM, SYSTEMS, FROM_SYSTEM, NOT_OURS);
    }
}
