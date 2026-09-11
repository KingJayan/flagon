package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/** shows what a ClassLoader can answer in a bundle: the classes the build kept, and no more. */
public final class ClassLoaderProgram implements Program {

    @Override
    public String id() {
        return "classloader";
    }

    @Override
    public String description() {
        return "loads a class by name and scans for a resource";
    }

    @Override
    public void run(String[] arguments) throws IOException {
        ConsoleSink console = Platforms.get().console();
        ClassLoader loader = ClassLoaderProgram.class.getClassLoader();

        try {
            Class<?> found = loader.loadClass("dev.flagon.sandbox.Reflected");
            console.info("classloader: loaded " + found.getName());
        } catch (ClassNotFoundException missing) {
            console.info("classloader: a class the build kept was not found: " + missing);
        }

        try {
            loader.loadClass("com.example.NeverCompiled");
            console.info("classloader: a class nobody compiled was loaded, which cannot happen");
        } catch (ClassNotFoundException missing) {
            console.info("classloader: a class nobody compiled is refused by name");
        }

        Enumeration<URL> scanned = loader.getResources("META-INF/services/nothing");
        console.info("classloader: scanning found any resource " + scanned.hasMoreElements());
    }
}
