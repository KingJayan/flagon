package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

/** the shape a 1.13+ client runs: a render thread draining what worker threads produced. */
public final class ThreadingDemoProgram implements Program {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int FRAMES = 30;
    private static final int CHUNKS = 12;

    private final ConcurrentLinkedQueue<String> ready = new ConcurrentLinkedQueue<>();

    @Override
    public String id() {
        return "threading";
    }

    @Override
    public String description() {
        return "runs a render loop while worker threads load chunks behind it";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();

        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Flagon — threading");
        Display.create();

        threadsRunBesideTheLoop(console);
        executorsAndFutures(console);
        completableFutureChains(console);
        latchesAndLocks(console);
        readersAndAWriter(console);
        conditionsAndPools(console);
        aMonitorHeldAcrossASleep(console);
        chunksArriveWhileTheLoopDraws(console);

        Display.destroy();
        console.info("threading: done");
    }

    /** a method that never suspends must still wait for a monitor another thread holds */
    private void aMonitorHeldAcrossASleep(ConsoleSink console) throws InterruptedException {
        Object lock = new Object();
        CountDownLatch holding = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            synchronized (lock) {
                holding.countDown();
                try {
                    Thread.sleep(20L);
                } catch (InterruptedException stopped) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        holder.start();
        holding.await();
        console.info("threading: entered a held monitor " + enter(lock));
    }

    private static int entered;

    private static int enter(Object lock) {
        synchronized (lock) {
            return ++entered;
        }
    }

    /** a condition hands a lock back and forth, and the pool classes run work the same way */
    private void conditionsAndPools(ConsoleSink console) throws Exception {
        ReentrantLock lock = new ReentrantLock();
        Condition ready = lock.newCondition();
        boolean[] signalled = {false};

        Thread waiter = new Thread(() -> {
            lock.lock();
            try {
                while (!signalled[0]) {
                    ready.await();
                }
            } catch (InterruptedException stopped) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        waiter.start();
        Thread.sleep(5L);

        lock.lock();
        try {
            signalled[0] = true;
            ready.signalAll();
        } finally {
            lock.unlock();
        }
        waiter.join(1000L);
        console.info("threading: a condition woke the waiter " + !waiter.isAlive());

        ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        FutureTask<String> task = new FutureTask<>(() -> "from a future task");
        pool.execute(task);
        console.info("threading: a future task answers " + task.get());
        pool.shutdown();

        ScheduledThreadPoolExecutor clock = new ScheduledThreadPoolExecutor(1);
        ScheduledFuture<String> later =
                clock.schedule(() -> "after a delay", 10L, TimeUnit.MILLISECONDS);
        console.info("threading: a scheduled task answers " + later.get());
        clock.shutdown();

        AtomicReferenceArray<String> slots = new AtomicReferenceArray<>(2);
        slots.set(0, "first");
        console.info("threading: a slot swaps only from what it held "
                + slots.compareAndSet(0, "first", "second") + " "
                + slots.compareAndSet(1, "first", "third") + " " + slots.get(0));
    }

    /** a worker that sleeps must not stop the frame loop */
    private void threadsRunBesideTheLoop(ConsoleSink console) throws Exception {
        AtomicInteger ticks = new AtomicInteger();
        Thread worker = new Thread(() -> {
            for (int tick = 0; tick < 5; tick++) {
                try {
                    Thread.sleep(1L);
                } catch (InterruptedException stopped) {
                    return;
                }
                ticks.incrementAndGet();
            }
        }, "worker");
        worker.start();

        int frames = 0;
        while (worker.isAlive() && frames < FRAMES) {
            Display.update();
            frames++;
        }
        worker.join();
        console.info("threading: worker ticked " + ticks.get() + " over " + (frames > 0)
                + " frames drawn beside it");
    }

    private void executorsAndFutures(ConsoleSink console) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int chunk = 0; chunk < 6; chunk++) {
            int number = chunk;
            futures.add(pool.submit(() -> {
                Thread.sleep(1L);
                return number * number;
            }));
        }
        int total = 0;
        for (Future<Integer> future : futures) {
            total += future.get();
        }
        pool.shutdown();
        console.info("threading: pool summed " + total + ", terminated "
                + pool.awaitTermination(1L, TimeUnit.SECONDS));
    }

    /** the shape chunk loading takes: supplyAsync, a map, a compose and a join of many */
    private void completableFutureChains(ConsoleSink console) throws Exception {
        CompletableFuture<String> chained = CompletableFuture
                .supplyAsync(() -> 6)
                .thenApply(count -> count * 7)
                .thenCompose(answer -> CompletableFuture.supplyAsync(() -> "chunk " + answer))
                .thenApply(String::trim);
        console.info("threading: chain produced " + chained.get(2L, TimeUnit.SECONDS));

        CompletableFuture<Integer> left = CompletableFuture.supplyAsync(() -> 2);
        CompletableFuture<Integer> right = CompletableFuture.supplyAsync(() -> 3);
        console.info("threading: combined " + left.thenCombine(right, Integer::sum).get());

        CompletableFuture<?>[] batch = new CompletableFuture<?>[4];
        AtomicInteger built = new AtomicInteger();
        for (int chunk = 0; chunk < batch.length; chunk++) {
            batch[chunk] = CompletableFuture.supplyAsync(built::incrementAndGet);
        }
        CompletableFuture.allOf(batch).get(2L, TimeUnit.SECONDS);
        console.info("threading: allOf waited for " + built.get());

        String recovered = CompletableFuture.<String>supplyAsync(() -> {
            throw new IllegalStateException("chunk is corrupt");
        }).exceptionally(thrown -> "recovered").get();
        console.info("threading: a failed stage " + recovered);
    }

    private void latchesAndLocks(ConsoleSink console) throws Exception {
        CountDownLatch latch = new CountDownLatch(3);
        ReentrantLock lock = new ReentrantLock();
        int[] guarded = {0};
        for (int worker = 0; worker < 3; worker++) {
            new Thread(() -> {
                lock.lock();
                try {
                    guarded[0]++;
                } finally {
                    lock.unlock();
                }
                latch.countDown();
            }).start();
        }
        boolean opened = latch.await(2L, TimeUnit.SECONDS);
        console.info("threading: latch opened " + opened + " with " + guarded[0] + " under a lock");
    }

    private void readersAndAWriter(ConsoleSink console) throws Exception {
        ReentrantReadWriteLock guard = new ReentrantReadWriteLock();
        int[] value = {0};

        guard.readLock().lock();
        guard.readLock().lock();
        console.info("threading: " + guard.getReadLockCount() + " readers hold the lock");
        guard.readLock().unlock();
        guard.readLock().unlock();

        CountDownLatch written = new CountDownLatch(1);
        new Thread(() -> {
            guard.writeLock().lock();
            try {
                value[0] = 7;
            } finally {
                guard.writeLock().unlock();
            }
            written.countDown();
        }).start();

        console.info("threading: the writer finished " + written.await(2L, TimeUnit.SECONDS)
                + " and left " + value[0]);

        guard.readLock().lock();
        try {
            guard.writeLock().lock();
            console.info("threading: a reader took the write lock, which should not happen");
        } catch (IllegalStateException refused) {
            console.info("threading: upgrading a read lock is refused rather than hung");
        } finally {
            guard.readLock().unlock();
        }
    }

    /** the handoff itself: workers queue results, the loop drains them a frame at a time */
    private void chunksArriveWhileTheLoopDraws(ConsoleSink console) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        for (int chunk = 0; chunk < CHUNKS; chunk++) {
            int number = chunk;
            workers.execute(() -> {
                try {
                    Thread.sleep(1L);
                } catch (InterruptedException stopped) {
                    return;
                }
                ready.offer("chunk-" + number);
            });
        }

        int drained = 0;
        int frames = 0;
        int emptyFrames = 0;
        while (drained < CHUNKS && frames < FRAMES) {
            String chunk = ready.poll();
            if (chunk == null) {
                emptyFrames++;
            } else {
                drained++;
            }
            Display.update();
            frames++;
        }
        workers.shutdown();
        console.info("threading: drained " + drained + " of " + CHUNKS + " chunks over "
                + frames + " frames");
        console.info("threading: the loop kept drawing while empty " + (emptyFrames > 0));
    }
}
