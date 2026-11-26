package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** reads what the jvm wrote, and writes the same bytes back */
public final class SerialProgram implements Program {

    @Override
    public String id() {
        return "serial";
    }

    @Override
    public String description() {
        return "reads a java serialization stream the jvm wrote and writes the same bytes";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();
        byte[] jvm = fixture();
        SerialSamples.Holder read;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(jvm))) {
            read = (SerialSamples.Holder) in.readObject();
        }
        List<String> differences = SerialSamples.differences(read);
        console.info("serial: read " + jvm.length + " bytes with "
                + differences.size() + " differences " + differences);

        byte[] ours = write(SerialSamples.graph());
        int mismatch = -1;
        for (int at = 0; at < Math.max(ours.length, jvm.length) && mismatch < 0; at++) {
            if (at >= ours.length || at >= jvm.length || ours[at] != jvm[at]) {
                mismatch = at;
            }
        }
        console.info(mismatch < 0 ? "serial: wrote the same " + ours.length + " bytes as the jvm"
                : "serial: wrote " + ours.length + " bytes, first differing at " + mismatch);

        Map<String, Set<Integer>> hashed = new HashMap<>();
        hashed.put("odd", new HashSet<>(List.of(1, 3)));
        IllegalStateException thrown = new IllegalStateException("bad",
                new java.io.IOException("io"));
        Object[] back = (Object[]) read(write(new Object[] {hashed, thrown}));
        Throwable cause = ((Throwable) back[1]).getCause();
        console.info("serial: a round trip keeps " + back[0] + " and "
                + ((Throwable) back[1]).getMessage() + " caused by " + cause.getMessage());
    }

    /** the fixture is kept apart from the pack, so it may arrive after the program starts */
    private byte[] fixture() throws IOException, InterruptedException {
        for (int tries = 0; ; tries++) {
            try (InputStream in = getClass().getResourceAsStream("serial.bin")) {
                return in.readAllBytes();
            } catch (IOException early) {
                if (tries == 100) {
                    throw early;
                }
                Thread.sleep(50);
            }
        }
    }

    private static byte[] write(Object graph) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(graph);
        }
        return bytes.toByteArray();
    }

    private static Object read(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return in.readObject();
        }
    }
}
