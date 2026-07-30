package dev.decanter.platform.browser;

import dev.decanter.platform.ConsoleSink;
import org.teavm.jso.JSBody;

/** writes to the devtools console, and to the page's bridge when one is installed. */
public final class BrowserConsole implements ConsoleSink {

    private final HostBridge bridge;

    BrowserConsole(HostBridge bridge) {
        this.bridge = bridge;
    }

    @JSBody(params = "text", script = "console.debug(text);")
    private static native void consoleDebug(String text);

    @JSBody(params = "text", script = "console.info(text);")
    private static native void consoleInfo(String text);

    @JSBody(params = "text", script = "console.warn(text);")
    private static native void consoleWarn(String text);

    @JSBody(params = "text", script = "console.error(text);")
    private static native void consoleError(String text);

    @Override
    public void write(Level level, String message, Throwable error) {
        String body = error == null ? message : message + "\n" + describe(error);
        String text = "[decanter] " + body;
        switch (level) {
            case DEBUG -> consoleDebug(text);
            case INFO -> consoleInfo(text);
            case WARN -> consoleWarn(text);
            case ERROR -> consoleError(text);
        }
        if (bridge != null) {
            bridge.log(level.name(), body);
        }
    }

    // aot stack traces are only as good as the emitted debug info, so always include type+message
    static String describe(Throwable error) {
        StringBuilder out = new StringBuilder();
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 8) {
            if (depth > 0) {
                out.append("Caused by: ");
            }
            out.append(current.getClass().getName());
            if (current.getMessage() != null) {
                out.append(": ").append(current.getMessage());
            }
            out.append('\n');
            for (StackTraceElement frame : current.getStackTrace()) {
                out.append("\tat ").append(frame).append('\n');
            }
            current = current.getCause() == current ? null : current.getCause();
            depth++;
        }
        return out.toString();
    }
}
