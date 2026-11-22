package dev.flagon.sandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Phaser;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

/**
 * the java.util.concurrent behavior a page must match. the jvm test checks these lines are what
 * the jdk says, and the browser test checks a bundle says the same.
 */
public final class ConcurrencyChecks {

    private interface Check {
        void run(List<String> out) throws Exception;
    }

    private ConcurrencyChecks() {
    }

    public static List<String> run() {
        List<String> out = new ArrayList<>();
        check(out, "barrier", ConcurrencyChecks::barrier);
        check(out, "exchanger", ConcurrencyChecks::exchanger);
        check(out, "phaser", ConcurrencyChecks::phaser);
        check(out, "stamped", ConcurrencyChecks::stamped);
        check(out, "forkjoin", ConcurrencyChecks::forkJoin);
        check(out, "completer", ConcurrencyChecks::completer);
        check(out, "skiplist", ConcurrencyChecks::skipList);
        check(out, "priority", ConcurrencyChecks::priority);
        check(out, "delay", ConcurrencyChecks::delay);
        check(out, "future", ConcurrencyChecks::future);
        return out;
    }

    private static void check(List<String> out, String name, Check check) {
        List<String> lines = new ArrayList<>();
        try {
            check.run(lines);
        } catch (Throwable thrown) {
            lines.add("threw " + thrown);
        }
        for (String line : lines) {
            out.add(name + ": " + line);
        }
    }

    private static Thread start(Runnable body) {
        Thread thread = new Thread(body);
        thread.start();
        return thread;
    }

    private static String nameOf(Throwable thrown) {
        return thrown == null ? "none" : thrown.getClass().getSimpleName();
    }

    private static void barrier(List<String> out) throws Exception {
        AtomicInteger trips = new AtomicInteger();
        CyclicBarrier barrier = new CyclicBarrier(3, trips::incrementAndGet);
        List<Integer> arrivals = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        for (int at = 0; at < 3; at++) {
            threads.add(start(() -> {
                try {
                    for (int round = 0; round < 2; round++) {
                        arrivals.add(barrier.await());
                    }
                } catch (InterruptedException | BrokenBarrierException failed) {
                    arrivals.add(-1);
                }
            }));
        }
        for (Thread thread : threads) {
            thread.join();
        }
        List<Integer> sorted = new ArrayList<>(arrivals);
        Collections.sort(sorted);
        out.add("tripped " + trips.get() + " times, arrival indexes " + sorted);

        CyclicBarrier lonely = new CyclicBarrier(2);
        String timedOut;
        try {
            lonely.await(10, TimeUnit.MILLISECONDS);
            timedOut = "none";
        } catch (TimeoutException expected) {
            timedOut = nameOf(expected);
        }
        boolean broken = lonely.isBroken();
        String next;
        try {
            lonely.await();
            next = "none";
        } catch (BrokenBarrierException expected) {
            next = nameOf(expected);
        }
        lonely.reset();
        out.add("a lone wait throws " + timedOut + ", breaks " + broken + ", then " + next
                + ", and reset mends " + !lonely.isBroken());

        CyclicBarrier failing = new CyclicBarrier(1, () -> {
            throw new IllegalStateException("action");
        });
        String thrown;
        try {
            failing.await();
            thrown = "none";
        } catch (IllegalStateException expected) {
            thrown = expected.getMessage();
        }
        out.add("a failing action throws " + thrown + " and breaks " + failing.isBroken());
    }

    private static void exchanger(List<String> out) throws Exception {
        Exchanger<String> exchanger = new Exchanger<>();
        String[] got = new String[1];
        Thread other = start(() -> {
            try {
                got[0] = exchanger.exchange("from the worker");
            } catch (InterruptedException stopped) {
                got[0] = "interrupted";
            }
        });
        String mine = exchanger.exchange("from main");
        other.join();
        out.add("main got " + mine + ", the worker got " + got[0]);
        String timedOut;
        try {
            exchanger.exchange("alone", 10, TimeUnit.MILLISECONDS);
            timedOut = "none";
        } catch (TimeoutException expected) {
            timedOut = nameOf(expected);
        }
        out.add("a lone exchange throws " + timedOut);
    }

    private static void phaser(List<String> out) throws Exception {
        Phaser phaser = new Phaser(1);
        AtomicInteger work = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int at = 0; at < 3; at++) {
            phaser.register();
            threads.add(start(() -> {
                for (int round = 0; round < 2; round++) {
                    work.incrementAndGet();
                    phaser.arriveAndAwaitAdvance();
                }
                phaser.arriveAndDeregister();
            }));
        }
        out.add("four parties registered " + phaser.getRegisteredParties());
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndAwaitAdvance();
        for (Thread thread : threads) {
            thread.join();
        }
        out.add("after two rounds the phase is " + phaser.getPhase() + " with work "
                + work.get() + " and " + phaser.getRegisteredParties() + " party left");
        int last = phaser.arriveAndDeregister();
        out.add("the last party leaves at phase " + last + ", terminated "
                + phaser.isTerminated() + ", phase negative " + (phaser.getPhase() < 0));

        Phaser root = new Phaser();
        Phaser child = new Phaser(root, 2);
        out.add("a child with two parties registers " + root.getRegisteredParties()
                + " with its root");
        child.arrive();
        int before = root.getPhase();
        child.arrive();
        out.add("the root goes from phase " + before + " to " + root.getPhase()
                + " and the child reads " + child.getPhase());
        out.add("awaiting an old phase returns " + phaser.awaitAdvance(0) + " at once");

        String overArrived;
        try {
            new Phaser().arrive();
            overArrived = "none";
        } catch (IllegalStateException expected) {
            overArrived = nameOf(expected);
        }
        out.add("arriving with no parties throws " + overArrived);
    }

    private static void stamped(List<String> out) throws Exception {
        StampedLock lock = new StampedLock();
        long optimistic = lock.tryOptimisticRead();
        out.add("an optimistic stamp is optimistic " + StampedLock.isOptimisticReadStamp(optimistic)
                + " and valid " + lock.validate(optimistic));
        long write = lock.writeLock();
        out.add("a write stamp is a write stamp " + StampedLock.isWriteLockStamp(write)
                + ", locked " + lock.isWriteLocked() + ", optimistic read "
                + lock.tryOptimisticRead()
                + ", try read " + lock.tryReadLock());
        lock.unlockWrite(write);
        out.add("after a write the old optimistic stamp is valid " + lock.validate(optimistic));

        long read = lock.readLock();
        long second = lock.readLock();
        out.add("two reads: count " + lock.getReadLockCount() + ", read stamp "
                + StampedLock.isReadLockStamp(read) + ", try write " + lock.tryWriteLock()
                + ", convert to write " + lock.tryConvertToWriteLock(read));
        lock.unlockRead(second);
        long converted = lock.tryConvertToWriteLock(read);
        out.add("one read converts to a write " + StampedLock.isWriteLockStamp(converted)
                + " with " + lock.getReadLockCount() + " reads");
        long back = lock.tryConvertToReadLock(converted);
        out.add("and back to a read " + StampedLock.isReadLockStamp(back) + ", write locked "
                + lock.isWriteLocked());

        List<String> order = Collections.synchronizedList(new ArrayList<>());
        Thread writer = start(() -> {
            long stamp = lock.writeLock();
            order.add("write");
            lock.unlockWrite(stamp);
        });
        Thread.sleep(20);
        order.add("read held");
        lock.unlock(back);
        writer.join();
        out.add("a writer waits for the reader: " + order);

        String wrong;
        try {
            lock.unlockWrite(write);
            wrong = "none";
        } catch (IllegalMonitorStateException expected) {
            wrong = nameOf(expected);
        }
        out.add("unlocking a stale stamp throws " + wrong);
        java.util.concurrent.locks.Lock view = lock.asReadWriteLock().writeLock();
        view.lock();
        out.add("the write view locks " + lock.isWriteLocked());
        view.unlock();
        out.add("and unlocks " + !lock.isWriteLocked());
    }

    /** the n-th fibonacci number, split in two each step */
    private static final class Fibonacci extends RecursiveTask<Integer> {
        private final int n;

        Fibonacci(int n) {
            this.n = n;
        }

        @Override
        protected Integer compute() {
            if (n < 2) {
                return n;
            }
            Fibonacci first = new Fibonacci(n - 1);
            first.fork();
            return new Fibonacci(n - 2).compute() + first.join();
        }
    }

    /** adds one to each item of a range */
    private static final class Increment extends RecursiveAction {
        private final int[] items;
        private final int from;
        private final int to;

        Increment(int[] items, int from, int to) {
            this.items = items;
            this.from = from;
            this.to = to;
        }

        @Override
        protected void compute() {
            if (to - from <= 4) {
                for (int at = from; at < to; at++) {
                    items[at]++;
                }
                return;
            }
            int middle = (from + to) >>> 1;
            invokeAll(new Increment(items, from, middle), new Increment(items, middle, to));
        }
    }

    private static void forkJoin(List<String> out) throws Exception {
        ForkJoinPool pool = new ForkJoinPool(4);
        out.add("fibonacci of 20 is " + pool.invoke(new Fibonacci(20)) + " on a pool of "
                + pool.getParallelism());
        out.add("and " + new Fibonacci(15).invoke() + " invoked directly, "
                + ForkJoinPool.commonPool().invoke(new Fibonacci(12)) + " on the common pool");
        int[] items = new int[50];
        pool.invoke(new Increment(items, 0, items.length));
        out.add("an action adds " + Arrays.stream(items).sum() + " over 50 items");

        ForkJoinTask<String> where = pool.submit(() -> "in a pool " + ForkJoinTask.inForkJoinPool()
                + ", the same pool " + (ForkJoinTask.getPool() == pool));
        out.add("a submitted callable runs " + where.get());
        out.add("outside it runs in a pool " + ForkJoinTask.inForkJoinPool());

        RecursiveTask<Integer> failing = new RecursiveTask<>() {
            @Override
            protected Integer compute() {
                throw new IllegalArgumentException("bad split");
            }
        };
        pool.execute(failing);
        String joined;
        try {
            failing.join();
            joined = "none";
        } catch (IllegalArgumentException expected) {
            joined = nameOf(expected);
        }
        String got;
        try {
            failing.get();
            got = "none";
        } catch (ExecutionException expected) {
            got = nameOf(expected.getCause());
        }
        out.add("a failing task joins with " + joined + ", gets " + got + ", abnormal "
                + failing.isCompletedAbnormally());

        ForkJoinTask<?> cancelled = ForkJoinTask.adapt(() -> { });
        out.add("an unstarted task cancels " + cancelled.cancel(true) + ", done "
                + cancelled.isDone());

        List<Fibonacci> batch = new ArrayList<>();
        for (int n = 5; n <= 8; n++) {
            batch.add(new Fibonacci(n));
        }
        List<Integer> results = new ArrayList<>();
        pool.invoke(ForkJoinTask.adapt(() -> {
            for (Fibonacci each : ForkJoinTask.invokeAll(batch)) {
                results.add(each.join());
            }
        }));
        out.add("invokeAll gives " + results);
        pool.shutdown();
        out.add("shut down it terminates " + pool.awaitTermination(5, TimeUnit.SECONDS));
    }

    /** sums a range by splitting it, finishing when the last piece reports */
    private static final class Summer extends CountedCompleter<Void> {
        private final int[] items;
        private final int from;
        private final int to;
        private final AtomicInteger total;

        Summer(CountedCompleter<?> parent, int[] items, int from, int to, AtomicInteger total) {
            super(parent);
            this.items = items;
            this.from = from;
            this.to = to;
            this.total = total;
        }

        @Override
        public void compute() {
            int high = to;
            while (high - from > 4) {
                int middle = (from + high) >>> 1;
                addToPendingCount(1);
                new Summer(this, items, middle, high, total).fork();
                high = middle;
            }
            int sum = 0;
            for (int at = from; at < high; at++) {
                sum += items[at];
            }
            total.addAndGet(sum);
            tryComplete();
        }
    }

    private static void completer(List<String> out) {
        int[] items = new int[100];
        for (int at = 0; at < items.length; at++) {
            items[at] = at + 1;
        }
        AtomicInteger total = new AtomicInteger();
        ForkJoinPool pool = new ForkJoinPool(3);
        pool.invoke(new Summer(null, items, 0, items.length, total));
        out.add("a counted completer sums 1 to 100 as " + total.get());
        pool.shutdown();
    }

    private static void skipList(List<String> out) {
        ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();
        for (int key : new int[] {5, 1, 9, 3, 7}) {
            map.put(key, "v" + key);
        }
        out.add("keys in order " + map.keySet() + ", first " + map.firstKey() + ", last "
                + map.lastKey());
        out.add("ceiling of 4 is " + map.ceilingKey(4) + ", floor of 4 is " + map.floorKey(4)
                + ", higher than 9 is " + map.higherKey(9));
        ConcurrentNavigableMap<Integer, String> middle = map.subMap(3, true, 7, false);
        out.add("the sub map from 3 to 7 is " + middle + ", the head below 5 is "
                + map.headMap(5) + ", descending " + map.descendingMap().keySet());
        middle.put(4, "v4");
        out.add("a put through the view reaches the map " + map.containsKey(4));
        String outside;
        try {
            middle.put(8, "v8");
            outside = "none";
        } catch (IllegalArgumentException expected) {
            outside = nameOf(expected);
        }
        out.add("a put outside the view throws " + outside);
        for (Iterator<Integer> keys = map.keySet().iterator(); keys.hasNext();) {
            int key = keys.next();
            if (key % 2 == 1) {
                map.remove(key);
            }
        }
        out.add("removing while iterating leaves " + map);
        map.put(2, "v2");
        map.put(6, "v6");
        Map.Entry<Integer, String> first = map.pollFirstEntry();
        String setValue;
        try {
            map.firstEntry().setValue("x");
            setValue = "none";
        } catch (UnsupportedOperationException expected) {
            setValue = nameOf(expected);
        }
        out.add("poll first gives " + first + ", entries refuse setValue with " + setValue);
        String nulls;
        try {
            map.put(null, "x");
            nulls = "none";
        } catch (NullPointerException expected) {
            nulls = nameOf(expected);
        }
        String keyAdd;
        try {
            map.keySet().add(10);
            keyAdd = "none";
        } catch (UnsupportedOperationException expected) {
            keyAdd = nameOf(expected);
        }
        out.add("a null key throws " + nulls + ", adding to the key set throws " + keyAdd);
        out.add("putIfAbsent keeps " + map.putIfAbsent(4, "new") + ", merge makes "
                + map.merge(4, "+", String::concat) + ", compute removes "
                + map.compute(4, (key, value) -> null) + " leaving " + map);

        ConcurrentSkipListSet<String> words = new ConcurrentSkipListSet<>(
                Collections.reverseOrder());
        words.addAll(List.of("pear", "apple", "fig", "apple"));
        NavigableSet<String> tail = words.tailSet("fig", true);
        out.add("a reversed set " + words + ", its tail from fig " + tail + ", added twice "
                + words.add("fig"));
        out.add("poll last " + words.pollLast() + " leaves " + words + ", equal to a tree set "
                + words.equals(new TreeSet<>(List.of("pear", "fig"))));
    }

    private static void priority(List<String> out) throws Exception {
        PriorityBlockingQueue<Integer> queue = new PriorityBlockingQueue<>();
        int[] taken = new int[1];
        Thread consumer = start(() -> {
            try {
                taken[0] = queue.take();
            } catch (InterruptedException stopped) {
                taken[0] = -1;
            }
        });
        Thread.sleep(10);
        queue.put(42);
        consumer.join();
        out.add("a waiting take gets " + taken[0]);
        queue.addAll(List.of(8, 3, 5, 1));
        List<Integer> drained = new ArrayList<>();
        out.add("peek " + queue.peek() + ", drained " + queue.drainTo(drained, 3) + " as "
                + drained + ", left " + queue.poll() + ", then " + queue.poll(5,
                TimeUnit.MILLISECONDS));
        PriorityBlockingQueue<String> byLength = new PriorityBlockingQueue<>(4,
                (one, two) -> Integer.compare(one.length(), two.length()));
        byLength.addAll(List.of("ccc", "a", "bb"));
        out.add("by length takes " + byLength.take() + " then " + byLength.take()
                + ", capacity left is the maximum " + (byLength.remainingCapacity()
                == Integer.MAX_VALUE));
    }

    /** due some milliseconds after it is made */
    private static final class Due implements Delayed {
        private final String name;
        private final long at;

        Due(String name, long millis) {
            this.name = name;
            this.at = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(at - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(at, ((Due) other).at);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static void delay(List<String> out) throws Exception {
        DelayQueue<Due> queue = new DelayQueue<>();
        long start = System.currentTimeMillis();
        queue.put(new Due("later", 40));
        queue.put(new Due("soon", 15));
        out.add("nothing is due at first: poll " + queue.poll() + ", size " + queue.size());
        Due first = queue.take();
        Due second = queue.take();
        long waited = System.currentTimeMillis() - start;
        out.add("take gives " + first + " then " + second + " after at least 35ms "
                + (waited >= 35));
        out.add("an empty queue polls with a timeout " + queue.poll(5, TimeUnit.MILLISECONDS));
    }

    private static void future(List<String> out) throws Exception {
        IllegalStateException cause = new IllegalStateException("cause");
        CompletableFuture<String> failed = CompletableFuture.failedFuture(cause);
        out.add("handle on a failed future sees "
                + failed.handle((value, thrown) -> nameOf(thrown)).join()
                + ", a stage after it sees "
                + failed.thenApply(String::length).handle((value, thrown) -> nameOf(thrown) + " of "
                + nameOf(thrown.getCause())).join());

        CompletableFuture<Integer> thrown = CompletableFuture.supplyAsync(() -> {
            throw cause;
        });
        String joined;
        try {
            thrown.join();
            joined = "none";
        } catch (CompletionException expected) {
            joined = nameOf(expected) + " of " + nameOf(expected.getCause());
        }
        String got;
        try {
            thrown.get();
            got = "none";
        } catch (ExecutionException expected) {
            got = nameOf(expected) + " of " + nameOf(expected.getCause());
        }
        out.add("an async failure joins as " + joined + " and gets as " + got
                + ", exceptionNow " + nameOf(thrown.exceptionNow()));

        CompletableFuture<String> never = new CompletableFuture<>();
        String timedOut;
        try {
            never.orTimeout(20, TimeUnit.MILLISECONDS).join();
            timedOut = "none";
        } catch (CompletionException expected) {
            timedOut = nameOf(expected.getCause());
        }
        out.add("orTimeout fails with " + timedOut + ", completeOnTimeout gives "
                + new CompletableFuture<String>().completeOnTimeout("late", 20,
                TimeUnit.MILLISECONDS).join());

        CompletableFuture<String> slow = new CompletableFuture<>();
        out.add("applyToEither takes " + slow.applyToEither(
                CompletableFuture.completedFuture("fast"), String::toUpperCase).join()
                + ", acceptBoth gives " + CompletableFuture.completedFuture(2)
                .thenAcceptBoth(CompletableFuture.completedFuture(3), (one, two) -> { })
                .thenApply(ignored -> "done").join()
                + ", combineAsync gives " + CompletableFuture.completedFuture(2)
                .thenCombineAsync(CompletableFuture.supplyAsync(() -> 5), Integer::sum).join());

        AtomicInteger ran = new AtomicInteger();
        CompletableFuture.completedFuture(1).runAfterBoth(CompletableFuture.completedFuture(2),
                ran::incrementAndGet).join();
        slow.runAfterEither(CompletableFuture.completedFuture(1), ran::incrementAndGet).join();
        slow.acceptEither(CompletableFuture.completedFuture("x"), each -> ran.incrementAndGet())
                .join();
        out.add("runAfterBoth, runAfterEither and acceptEither ran " + ran.get());

        out.add("exceptionallyCompose recovers "
                + failed.exceptionallyCompose(error -> CompletableFuture.completedFuture(
                "again after " + error.getMessage())).join()
                + ", exceptionallyAsync gives "
                + failed.exceptionallyAsync(error -> "async " + nameOf(error)).join()
                + ", thenComposeAsync gives "
                + CompletableFuture.completedFuture(4).thenComposeAsync(
                value -> CompletableFuture.supplyAsync(() -> value * 10)).join());

        String whenFailed;
        try {
            CompletableFuture.completedFuture("ok").whenComplete((value, error) -> {
                throw new IllegalArgumentException("in action");
            }).join();
            whenFailed = "none";
        } catch (CompletionException expected) {
            whenFailed = nameOf(expected.getCause());
        }
        out.add("a throwing whenComplete action fails the stage with " + whenFailed);

        CompletableFuture<String> late = new CompletableFuture<>();
        CompletableFuture<Void> all = CompletableFuture.allOf(failed, late);
        boolean doneEarly = all.isDone();
        late.complete("late");
        String allFailure;
        try {
            all.join();
            allFailure = "none";
        } catch (CompletionException expected) {
            allFailure = nameOf(expected.getCause());
        }
        out.add("allOf waits for every future " + !doneEarly + " and fails with " + allFailure);

        long start = System.currentTimeMillis();
        long delayed = CompletableFuture.supplyAsync(System::currentTimeMillis,
                CompletableFuture.delayedExecutor(30, TimeUnit.MILLISECONDS)).join() - start;
        out.add("a delayed executor waits at least 25ms " + (delayed >= 25));

        CompletableFuture<String> cancelled = new CompletableFuture<>();
        cancelled.thenApply(String::length);
        int dependents = cancelled.getNumberOfDependents();
        boolean cancelledIt = cancelled.cancel(true);
        String cancelledJoin;
        try {
            cancelled.join();
            cancelledJoin = "none";
        } catch (CancellationException expected) {
            cancelledJoin = nameOf(expected);
        }
        out.add("with " + dependents + " dependent, cancel " + cancelledIt + ", cancelled "
                + cancelled.isCancelled() + ", join throws " + cancelledJoin);

        CompletableFuture<String> done = CompletableFuture.completedFuture("v");
        out.add("copy " + done.copy().join() + ", minimal stage "
                + done.minimalCompletionStage().toCompletableFuture().join() + ", resultNow "
                + done.resultNow() + ", ends " + done.toString().endsWith("[Completed normally]")
                + ", completeAsync " + new CompletableFuture<String>().completeAsync(() -> "a")
                .join() + ", getNow " + new CompletableFuture<String>().getNow("fallback"));
    }
}
