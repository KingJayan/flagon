package dev.flagon.pausing;

import dev.flagon.runtime.ProgramCatalog;

import java.io.IOException;

/** writes the pausing bundle's catalog beside the bundle */
public final class PausingCatalog {

    private PausingCatalog() {
    }

    public static void main(String[] arguments) throws IOException {
        ProgramCatalog.writeTo(PausingMain.registry(), arguments[0]);
    }
}
