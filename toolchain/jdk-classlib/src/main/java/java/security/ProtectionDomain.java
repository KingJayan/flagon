package java.security;

public class ProtectionDomain {

    private final CodeSource codeSource;

    public ProtectionDomain(CodeSource codeSource, PermissionCollection permissions) {
        this.codeSource = codeSource;
    }

    public final CodeSource getCodeSource() {
        return codeSource;
    }

    @Override
    public String toString() {
        return "ProtectionDomain " + codeSource;
    }
}
