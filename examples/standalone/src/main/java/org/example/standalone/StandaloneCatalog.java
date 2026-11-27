package org.example.standalone;

import dev.flagon.runtime.ProgramCatalog;

import java.io.IOException;

public final class StandaloneCatalog {

    private StandaloneCatalog() {
    }

    public static void main(String[] arguments) throws IOException {
        ProgramCatalog.writeTo(StandaloneMain.registry(), arguments[0]);
    }
}
