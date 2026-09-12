package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.lang.management.ManagementFactory;
import java.net.IDN;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** shows the jdk calls the browser classlib leaves out, which the build sends to Flagon. */
public final class JdkGapsProgram implements Program {

    @Override
    public String id() {
        return "jdkgaps";
    }

    @Override
    public String description() {
        return "asks for the jdk calls the browser classlib has none of";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();

        console.info("jdkgaps: the memory ceiling is "
                + Runtime.getRuntime().maxMemory() / (1024 * 1024) + "mb");

        try {
            Runtime.getRuntime().exec("ls");
            console.info("jdkgaps: a process started, which cannot happen");
        } catch (RuntimeException refused) {
            console.info("jdkgaps: starting a process is refused by name");
        }

        try {
            System.loadLibrary("lwjgl");
            console.info("jdkgaps: a native library loaded, which cannot happen");
        } catch (RuntimeException refused) {
            console.info("jdkgaps: loading a native library is refused by name");
        }

        console.info("jdkgaps: the superclass of a string is "
                + String.class.getGenericSuperclass());
        console.info("jdkgaps: a string is anonymous " + String.class.isAnonymousClass());

        console.info("jdkgaps: punycode writes a name as " + IDN.toASCII("münchen.de"));

        console.info("jdkgaps: the sql day is " + java.sql.Date.valueOf("2026-01-02"));
        console.info("jdkgaps: uptime is at least 0ms "
                + (ManagementFactory.getRuntimeMXBean().getUptime() >= 0));

        Callable<Object> wrapped = Executors.callable(() -> { });
        console.info("jdkgaps: a wrapped runnable answers " + wrapped.call());

        URL url = new URL("http://localhost/nothing");
        console.info("jdkgaps: a connection through a named proxy opens "
                + (url.openConnection(Proxy.NO_PROXY) != null));
    }
}
