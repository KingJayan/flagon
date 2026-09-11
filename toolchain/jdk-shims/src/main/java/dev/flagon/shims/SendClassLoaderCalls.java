package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

/**
 * the browser classlib's {@link ClassLoader} has neither loadClass nor getResources, so a jar
 * that scans its own classpath does not link. both are sent to {@code ClassLoaders}.
 */
public final class SendClassLoaderCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.lang.ClassLoader";
    private static final String LOADERS = "dev.flagon.platform.ClassLoaders";

    private static final Map<String, String> SENT = Map.of(
            "loadClass", "loadClass",
            "getResources", "getResources");

    private static final String[] NOT_OURS = {"org.teavm.", LOADERS};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, SUBJECT, LOADERS, SENT, NOT_OURS);
    }
}
