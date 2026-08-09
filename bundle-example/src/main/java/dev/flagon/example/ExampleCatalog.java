package dev.flagon.example;

import dev.flagon.runtime.ProgramCatalog;

import java.io.IOException;

/** writes the example bundle's catalog beside the bundle. run by the build, never by the page. */
public final class ExampleCatalog {

    private ExampleCatalog() {
    }

    public static void main(String[] arguments) throws IOException {
        ProgramCatalog.writeTo(ExampleMain.registry(), arguments[0]);
    }
}
