package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

/**
 * the browser classlib's {@link Runtime} has no shutdown hooks, so calls that want them are
 * sent to {@code ShutdownHooks}, which keeps the list and runs it when the program ends.
 */
public final class SendShutdownHooks implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.lang.Runtime";
    private static final String HOOKS = "dev.flagon.platform.ShutdownHooks";

    private static final Map<String, String> SENT = Map.of(
            "addShutdownHook", "add",
            "removeShutdownHook", "remove",
            "exit", "exit",
            "halt", "halt");

    /** the replacement calls Runtime itself, so rewriting it would only call itself back */
    private static final String[] NOT_OURS = {"org.teavm.", HOOKS};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, SUBJECT, HOOKS, SENT, NOT_OURS);
    }
}
