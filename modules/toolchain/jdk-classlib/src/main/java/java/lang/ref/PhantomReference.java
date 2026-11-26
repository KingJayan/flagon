package java.lang.ref;

public class PhantomReference<T> extends WeakReference<T> {

    public PhantomReference(T referent, ReferenceQueue<? super T> queue) {
        super(referent, queue);
    }

    @Override
    public T get() {
        return null;
    }
}
