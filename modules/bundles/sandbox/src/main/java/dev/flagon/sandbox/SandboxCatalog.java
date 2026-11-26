package dev.flagon.sandbox;

import dev.flagon.runtime.ProgramCatalog;

import java.io.IOException;

/** writes the sandbox bundle's catalog beside the bundle. run by the build, never by the page. */
public final class SandboxCatalog {

    private SandboxCatalog() {
    }

    public static void main(String[] arguments) throws IOException {
        ProgramCatalog.writeTo(SandboxMain.registry(), arguments[0]);
    }
}
