package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

public final class SendCharsetCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.nio.charset.Charset";
    private static final String CHARSETS = "dev.flagon.platform.Charsets";

    private static final Map<String, String> SENT = Map.of("isSupported", "isSupported");

    private static final String[] NOT_OURS = {"org.teavm.", CHARSETS};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, SUBJECT, CHARSETS, SENT, NOT_OURS);
    }
}
