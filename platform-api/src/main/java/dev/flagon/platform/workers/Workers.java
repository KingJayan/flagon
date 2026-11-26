package dev.flagon.platform.workers;

import dev.flagon.platform.UnsupportedCapabilityException;

/** bundle workers: this bundle started again on a thread of its own, talking by messages */
public interface Workers {

    boolean isAvailable();

    /**
     * starts this bundle in a worker, running the catalog program {@code program}. the worker
     * has no display; its console lines reach this bundle's console.
     */
    Channel spawn(String program, String... arguments);

    /** @return the channel to the bundle that spawned this one, or null outside a worker */
    Channel parent();

    /** @return workers that refuse to spawn with this reason */
    static Workers unavailable(String reason) {
        return new Workers() {
            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public Channel spawn(String program, String... arguments) {
                throw new UnsupportedCapabilityException(reason);
            }

            @Override
            public Channel parent() {
                return null;
            }
        };
    }
}
