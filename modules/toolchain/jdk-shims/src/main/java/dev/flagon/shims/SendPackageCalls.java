package dev.flagon.shims;

import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.util.Map;

public final class SendPackageCalls implements TeaVMPlugin, ClassHolderTransformer {

    private static final String SUBJECT = "java.lang.Package";
    private static final String PACKAGES = "dev.flagon.platform.Packages";

    private static final Map<String, String> SENT = Map.of(
            "getSpecificationTitle", "getSpecificationTitle",
            "getSpecificationVersion", "getSpecificationVersion",
            "getSpecificationVendor", "getSpecificationVendor",
            "getImplementationTitle", "getImplementationTitle",
            "getImplementationVersion", "getImplementationVersion",
            "getImplementationVendor", "getImplementationVendor");

    private static final String[] NOT_OURS = {"org.teavm.", PACKAGES};

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        Reroute.apply(type, SUBJECT, PACKAGES, SENT, NOT_OURS);
    }
}
