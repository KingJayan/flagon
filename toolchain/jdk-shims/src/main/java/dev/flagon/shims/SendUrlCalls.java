package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

public final class SendUrlCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.net.URL";
    private static final String URLS = "dev.flagon.platform.Urls";

    private static final Map<String, String> SENT = Map.of("openConnection:1", "openConnection");

    private static final String[] NOT_OURS = {"org.teavm.", URLS};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, SUBJECT, URLS, SENT, NOT_OURS);
    }
}
