package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

public final class SendResourceCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String LOADER = "java.lang.ClassLoader";
    private static final String TYPE = "java.lang.Class";
    private static final String LOADERS = "dev.flagon.platform.ClassLoaders";
    private static final String CLASSES = "dev.flagon.platform.Classes";

    private static final Map<String, String> SENT =
            Map.of("getResourceAsStream", "getResourceAsStream");

    private static final String[] NOT_OURS = {"org.teavm.", LOADERS, CLASSES,
        "dev.flagon.platform.Resources"};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, LOADER, LOADERS, SENT, NOT_OURS);
        Reroute.apply(type, TYPE, CLASSES, SENT, NOT_OURS);
    }
}
