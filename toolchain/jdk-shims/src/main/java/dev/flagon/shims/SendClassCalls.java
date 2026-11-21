package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

public final class SendClassCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.lang.Class";
    private static final String CLASSES = "dev.flagon.platform.Classes";

    private static final Map<String, String> SENT = Map.of(
            "getGenericSuperclass", "getGenericSuperclass",
            "getGenericInterfaces", "getGenericInterfaces",
            "getTypeParameters", "getTypeParameters",
            "getSigners", "getSigners",
            "isAnonymousClass", "isAnonymousClass",
            "getProtectionDomain", "getProtectionDomain",
            "getResource", "getResource");

    private static final String[] NOT_OURS = {"org.teavm.", CLASSES,
        "dev.flagon.platform.ClassLoaders"};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, SUBJECT, CLASSES, SENT, NOT_OURS);
    }
}
