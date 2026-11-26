package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

/**
 * sends the reflection calls the browser classlib leaves out to the classes that answer them:
 * record components and parameters to the ones this classlib adds, and the rest of Method to
 * dev.flagon.platform.Methods.
 */
public final class SendReflectionCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String RECORDS = "java.lang.reflect.RecordComponent";
    private static final String PARAMETERS = "java.lang.reflect.Parameter";
    private static final String METHODS = "dev.flagon.platform.Methods";

    private static final Map<String, String> COMPONENTS = Map.of("getRecordComponents", "of");
    private static final Map<String, String> NAMED = Map.of("getParameters", "of");
    private static final Map<String, String> METHOD = Map.of(
            "isDefault", "isDefault",
            "getDefaultValue", "getDefaultValue",
            "getAnnotationsByType", "getAnnotationsByType",
            "getDeclaredAnnotationsByType", "getDeclaredAnnotationsByType");

    private static final String[] NOT_OURS = {"java.", "javax.", "jdk.", "sun.", "org.teavm.",
        "dev.flagon.platform."};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, "java.lang.Class", RECORDS, COMPONENTS, NOT_OURS);
        for (String executable : new String[] {"java.lang.reflect.Method",
            "java.lang.reflect.Constructor", "java.lang.reflect.Executable"}) {
            Reroute.apply(type, executable, PARAMETERS, NAMED, NOT_OURS);
        }
        Reroute.apply(type, "java.lang.reflect.Method", METHODS, METHOD, NOT_OURS);
    }
}
