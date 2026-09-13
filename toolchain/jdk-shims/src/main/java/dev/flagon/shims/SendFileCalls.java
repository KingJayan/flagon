package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

/**
 * the browser classlib's file streams have no channel behind them, so a jar that asks for one
 * does not link. the ask is sent to {@code Files}, which says why there is none.
 */
public final class SendFileCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String INPUT = "java.io.FileInputStream";
    private static final String OUTPUT = "java.io.FileOutputStream";
    private static final String FILES = "dev.flagon.platform.Files";

    private static final Map<String, String> SENT = Map.of("getChannel", "getChannel");

    private static final String[] NOT_OURS = {"org.teavm.", FILES};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, INPUT, FILES, SENT, NOT_OURS);
        Reroute.apply(type, OUTPUT, FILES, SENT, NOT_OURS);
    }
}
