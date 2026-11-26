package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

public final class SendArrayCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.lang.reflect.Array";
    private static final String ARRAYS = "dev.flagon.platform.Arrays";

    private static final Map<String, String> SENT = Map.ofEntries(
            Map.entry("newInstance:2", "newInstance"),
            Map.entry("getBoolean", "getBoolean"),
            Map.entry("getByte", "getByte"),
            Map.entry("getChar", "getChar"),
            Map.entry("getShort", "getShort"),
            Map.entry("getInt", "getInt"),
            Map.entry("getLong", "getLong"),
            Map.entry("getFloat", "getFloat"),
            Map.entry("getDouble", "getDouble"),
            Map.entry("setBoolean", "setBoolean"),
            Map.entry("setByte", "setByte"),
            Map.entry("setChar", "setChar"),
            Map.entry("setShort", "setShort"),
            Map.entry("setInt", "setInt"),
            Map.entry("setLong", "setLong"),
            Map.entry("setFloat", "setFloat"),
            Map.entry("setDouble", "setDouble"));

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
