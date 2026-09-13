package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

public final class SendCollectionCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.util.Collections";
    private static final String MAPS = "dev.flagon.platform.Maps";

    private static final Map<String, String> SENT = Map.of(
            "unmodifiableSortedMap", "unmodifiableSortedMap");

    private static final String[] NOT_OURS = {"org.teavm.", MAPS};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, SUBJECT, MAPS, SENT, NOT_OURS);
    }
}
