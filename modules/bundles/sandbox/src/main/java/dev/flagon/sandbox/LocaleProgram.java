package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

/** prints the locale checks, which the browser test compares with what the jdk printed */
public final class LocaleProgram implements Program {

    @Override
    public String id() {
        return "locale";
    }

    @Override
    public String description() {
        return "java.text and java.time in several locales, on the page's Intl";
    }

    @Override
    public void run(String[] arguments) {
        ConsoleSink console = Platforms.get().console();
        for (String line : LocaleChecks.run()) {
            console.info("locale: " + line);
        }
        console.info("locale: done");
    }
}
