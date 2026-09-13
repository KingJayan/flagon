package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

/**
 * the browser classlib's {@link java.lang.reflect.Array} makes one level of array at a time, so
 * a call for several levels at once is sent to Flagon, which builds them.
 */
public final class SendArrayCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.lang.reflect.Array";
    private static final String ARRAYS = "dev.flagon.platform.Arrays";

    private static final Map<String, String> SENT = Map.of("newInstance:2", "newInstance");

    private static final String[] NOT_OURS = {"org.teavm.", ARRAYS};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, SUBJECT, ARRAYS, SENT, NOT_OURS);
    }
}
