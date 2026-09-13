package sun.misc;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public class Cleaner extends PhantomReference<Object> {

    private final Runnable work;

    private Cleaner(Object referent, Runnable work) {
        super(referent, new ReferenceQueue<>());
        this.work = work;
    }

    public static Cleaner create(Object referent, Runnable work) {
        return new Cleaner(referent, work);
    }

    public void clean() {
        work.run();
    }
}
