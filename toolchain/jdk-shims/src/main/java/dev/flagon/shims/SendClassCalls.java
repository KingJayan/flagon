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

    /** sent from application code only: java.lang.Class calls getDeclaredMethods itself */
    private static final Map<String, String> ADDED = Map.ofEntries(
            Map.entry("getDeclaredMethods", "getDeclaredMethods"),
            Map.entry("getMethods", "getMethods"),
            Map.entry("isRecord", "isRecord"),
            Map.entry("isSealed", "isSealed"),
            Map.entry("getPermittedSubclasses", "getPermittedSubclasses"),
            Map.entry("getEnclosingMethod", "getEnclosingMethod"),
            Map.entry("getEnclosingConstructor", "getEnclosingConstructor"),
            Map.entry("getDeclaredAnnotation", "getDeclaredAnnotation"),
            Map.entry("getAnnotationsByType", "getAnnotationsByType"),
            Map.entry("getDeclaredAnnotationsByType", "getDeclaredAnnotationsByType"),
            Map.entry("getPackageName", "getPackageName"),
            Map.entry("arrayType", "arrayType"),
            Map.entry("componentType", "componentType"),
            Map.entry("descriptorString", "descriptorString"),
            Map.entry("isSynthetic", "isSynthetic"),
            Map.entry("isHidden", "isHidden"),
            Map.entry("getNestHost", "getNestHost"),
            Map.entry("isNestmateOf", "isNestmateOf"));

    private static final String[] NOT_OURS = {"org.teavm.", CLASSES,
        "dev.flagon.platform.ClassLoaders", "dev.flagon.platform.ReflectionWatch"};

    private static final String[] NOT_APPLICATIONS = {"java.", "javax.", "jdk.", "sun.",
        "org.teavm.", "dev.flagon.platform."};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, SUBJECT, CLASSES, SENT, NOT_OURS);
        Reroute.apply(type, SUBJECT, CLASSES, ADDED, NOT_APPLICATIONS);
    }
}
