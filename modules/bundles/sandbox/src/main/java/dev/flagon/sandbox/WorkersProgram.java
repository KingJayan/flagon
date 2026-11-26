package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.platform.workers.Channel;
import dev.flagon.platform.workers.Message;
import dev.flagon.platform.workers.Workers;
import dev.flagon.runtime.Program;

/** spawns echo workers and talks to them in text and bytes */
public final class WorkersProgram implements Program {

    @Override
    public String id() {
        return "workers";
    }

    @Override
    public String description() {
        return "starts bundle workers and passes them text and bytes";
    }

    @Override
    public void run(String[] arguments) {
        ConsoleSink console = Platforms.get().console();
        Workers workers = Platforms.get().workers();
        console.info("workers: available " + workers.isAvailable() + " parent "
                + (workers.parent() != null));

        Channel echo = workers.spawn("worker-echo", "loud");
        echo.send("hello");
        byte[] big = new byte[1 << 20];
        for (int at = 0; at < big.length; at++) {
            big[at] = (byte) at;
        }
        echo.send(new Message("bytes", big));
        echo.send("bye");
        Message first = echo.receive();
        console.info("workers: text came back '" + first.text() + "'");
        Message second = echo.receive();
        byte[] back = second.data();
        boolean reversed = back.length == big.length;
        for (int at = 0; reversed && at < back.length; at++) {
            reversed = back[at] == big[big.length - 1 - at];
        }
        console.info("workers: " + second.text() + " came back reversed " + reversed
                + " and the sent array is whole " + (big[5] == 5));
        console.info("workers: then '" + echo.receive().text() + "' and the end "
                + (echo.receive() == null) + " open " + echo.isOpen());

        Channel stopped = workers.spawn("worker-echo", "quiet");
        stopped.send("ignored");
        stopped.close();
        console.info("workers: a closed worker ends " + (stopped.receive() == null)
                + " and polls nothing " + (stopped.poll() == null));
        console.info("workers: done");
    }

    /** what the worker runs: answers each message until bye, then exits */
    public static final class Echo implements Program {

        @Override
        public String id() {
            return "worker-echo";
        }

        @Override
        public String description() {
            return "answers its parent's messages; the workers program starts it";
        }

        @Override
        public void run(String[] arguments) {
            ConsoleSink console = Platforms.get().console();
            Channel parent = Platforms.get().workers().parent();
            boolean loud = arguments.length > 0 && "loud".equals(arguments[0]);
            if (loud) {
                console.info("echo: a worker is running with no display "
                        + !Platforms.get().hasDisplay());
            }
            Message message;
            while ((message = parent.receive()) != null) {
                byte[] data = message.data();
                if (data != null) {
                    byte[] reversed = new byte[data.length];
                    for (int at = 0; at < data.length; at++) {
                        reversed[at] = data[data.length - 1 - at];
                    }
                    parent.send(new Message(message.text(), reversed));
                } else {
                    parent.send(message.text().toUpperCase(java.util.Locale.ROOT));
                }
                if ("bye".equals(message.text())) {
                    break;
                }
            }
            console.info("echo: done");
        }
    }
}
