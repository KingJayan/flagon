package dev.flagon.wasi;

import dev.flagon.runtime.Program;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class HelloProgram implements Program {

    @Override
    public String id() {
        return "hello";
    }

    @Override
    public String description() {
        return "greets its arguments and does a little arithmetic";
    }

    @Override
    public void run(String[] arguments) {
        String who = arguments.length == 0 ? "wasi" : String.join(" ", arguments);
        System.out.println("Hello, " + who + "!");
        System.out.println("sin(1) = " + Math.sin(1) + ", 2^0.5 = " + Math.pow(2, 0.5));
        System.out.println("the clock is running: " + (System.currentTimeMillis() > 0));
        // with no Intl, locale data is en-US
        System.out.println(NumberFormat.getInstance(Locale.GERMANY).format(1234567.891) + " on "
                + LocalDate.of(2006, 1, 2).format(DateTimeFormatter.ofPattern(
                        "EEEE, d MMMM uuuu")));
        System.err.println("and stderr is separate");
    }
}
