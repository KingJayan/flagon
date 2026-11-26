package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * a reusable barrier with a changing number of parties. a tree of phasers shares the root's
 * phase and monitor; a child arrives at its parent once all of its own parties have arrived.
 */
public class Phaser {

    private static final int MAX_PARTIES = 0xFFFF;

    private final Phaser parent;
    private final Phaser root;
    private final List<Phaser> children = new ArrayList<>();
    private int parties;
    private int unarrived;
    /** kept on the root only */
    private int phase;
    private boolean terminated;

    public Phaser() {
        this(null, 0);
    }

    public Phaser(int parties) {
        this(null, parties);
    }

    public Phaser(Phaser parent) {
        this(parent, 0);
    }

    public Phaser(Phaser parent, int parties) {
        if (parties < 0 || parties > MAX_PARTIES) {
            throw new IllegalArgumentException("illegal number of parties: " + parties);
        }
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        if (parent != null) {
            synchronized (root) {
                parent.children.add(this);
            }
        }
        if (parties > 0) {
            doRegister(parties);
        }
    }

    public int register() {
        return doRegister(1);
    }

    public int bulkRegister(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("negative count: " + count);
        }
        if (count == 0) {
            return getPhase();
        }
        return doRegister(count);
    }

    private int doRegister(int count) {
        synchronized (root) {
            if (root.terminated) {
                return root.reportedPhase();
            }
            if (parties + count > MAX_PARTIES) {
                throw new IllegalStateException("more than " + MAX_PARTIES + " parties");
            }
            if (parties == 0 && parent != null) {
                parent.doRegister(1);
            }
            parties += count;
            unarrived += count;
            return root.phase;
        }
    }

    public int arrive() {
        return doArrive(false);
    }

    public int arriveAndDeregister() {
        return doArrive(true);
    }

    public int arriveAndAwaitAdvance() {
        synchronized (root) {
            int arrived = doArrive(false);
            if (arrived < 0) {
                return arrived;
            }
            return awaitAdvance(arrived);
        }
    }

    private int doArrive(boolean deregister) {
        synchronized (root) {
            if (root.terminated) {
                return root.reportedPhase();
            }
            if (unarrived == 0) {
                throw new IllegalStateException("there are no unarrived parties");
            }
            int arrivedAt = root.phase;
            unarrived--;
            if (deregister) {
                parties--;
            }
            if (unarrived == 0) {
                if (parent != null) {
                    parent.doArrive(parties == 0);
                } else {
                    advance();
                }
            }
            return arrivedAt;
        }
    }

    /** runs on the root with its monitor held */
    private void advance() {
        if (onAdvance(phase, parties)) {
            terminated = true;
        }
        phase = (phase + 1) & Integer.MAX_VALUE;
        reset();
        root.notifyAll();
    }

    private void reset() {
        unarrived = parties;
        for (Phaser child : children) {
            child.reset();
        }
    }

    public int awaitAdvance(int phase) {
        try {
            return await(phase, false, false, 0L);
        } catch (InterruptedException | TimeoutException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public int awaitAdvanceInterruptibly(int phase) throws InterruptedException {
        try {
            return await(phase, true, false, 0L);
        } catch (TimeoutException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public int awaitAdvanceInterruptibly(int phase, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        return await(phase, true, true, unit.toMillis(timeout));
    }

    private int await(int asked, boolean interruptible, boolean timed, long millis)
            throws InterruptedException, TimeoutException {
        synchronized (root) {
            if (asked < 0) {
                return asked;
            }
            long deadline = System.currentTimeMillis() + millis;
            boolean interrupted = false;
            while (!root.terminated && root.phase == asked) {
                try {
                    if (!timed) {
                        root.wait();
                    } else {
                        long left = deadline - System.currentTimeMillis();
                        if (left <= 0L) {
                            throw new TimeoutException();
                        }
                        root.wait(left);
                    }
                } catch (InterruptedException stopped) {
                    if (interruptible) {
                        throw stopped;
                    }
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            return root.reportedPhase();
        }
    }

    public void forceTermination() {
        synchronized (root) {
            if (!root.terminated) {
                root.terminated = true;
                root.notifyAll();
            }
        }
    }

    public final int getPhase() {
        return root.reportedPhase();
    }

    private int reportedPhase() {
        return terminated ? phase | Integer.MIN_VALUE : phase;
    }

    public int getRegisteredParties() {
        return parties;
    }

    public int getArrivedParties() {
        return parties - unarrived;
    }

    public int getUnarrivedParties() {
        return unarrived;
    }

    public Phaser getParent() {
        return parent;
    }

    public Phaser getRoot() {
        return root;
    }

    public boolean isTerminated() {
        return root.terminated;
    }

    protected boolean onAdvance(int phase, int registeredParties) {
        return registeredParties == 0;
    }

    @Override
    public String toString() {
        return super.toString() + "[phase = " + root.reportedPhase() + " parties = " + parties
                + " arrived = " + (parties - unarrived) + "]";
    }
}
