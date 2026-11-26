package dev.flagon.app;

import dev.flagon.runtime.ProgramCatalog;

import java.io.IOException;

/** writes the app bundle's catalog beside the bundle. run by the build, never by the page. */
public final class AppCatalog {

    private AppCatalog() {
    }

    public static void main(String[] arguments) throws IOException {
        ProgramCatalog.writeTo(AppMain.registry(), arguments[0]);
    }
}
