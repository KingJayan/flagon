package dev.flagon.platform;

public final class Runtimes {

    private static final long CEILING = 512L * 1024L * 1024L;

    private Runtimes() {
    }

    public static long maxMemory(Runtime runtime) {
        return CEILING;
    }

    public static Process exec(Runtime runtime, String command) {
        throw refuse();
    }

    public static Process exec(Runtime runtime, String[] command) {
        throw refuse();
    }

    private static UnsupportedCapabilityException refuse() {
        return new UnsupportedCapabilityException(
                "a page starts no processes: there is no operating system under it to start one"
                        + " on. see flagon-nu.vercel.app/docs");
    }
}
