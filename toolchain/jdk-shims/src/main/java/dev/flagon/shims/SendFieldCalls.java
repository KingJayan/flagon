package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

public final class SendFieldCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.lang.reflect.Field";
    private static final String FIELDS = "dev.flagon.platform.Fields";

    private static final Map<String, String> SENT = Map.of(
            "getLong", "getLong",
            "getInt", "getInt");

    private static final String[] NOT_OURS = {"org.teavm.", FIELDS};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, SUBJECT, FIELDS, SENT, NOT_OURS);
    }
}
