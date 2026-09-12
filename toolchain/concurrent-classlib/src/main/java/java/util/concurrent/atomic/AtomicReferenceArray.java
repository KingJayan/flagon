package java.util.concurrent.atomic;

/**
 * an array whose slots change one at a time.
 *
 * <p>the green threads under this run one at a time and switch only where a thread suspends,
 * and none of these suspends, so a read and the write after it always see the same slot.
 */
public class AtomicReferenceArray<E> {

    private final Object[] slots;

    public AtomicReferenceArray(int length) {
        this.slots = new Object[length];
    }

    public AtomicReferenceArray(E[] initial) {
        this.slots = initial.clone();
    }

    public int length() {
        return slots.length;
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) slots[index];
    }

    public void set(int index, E value) {
        slots[index] = value;
    }

    public void lazySet(int index, E value) {
        set(index, value);
    }

    @SuppressWarnings("unchecked")
    public E getAndSet(int index, E value) {
        E was = (E) slots[index];
        slots[index] = value;
        return was;
    }

    public boolean compareAndSet(int index, E expected, E value) {
        if (slots[index] != expected) {
            return false;
        }
        slots[index] = value;
        return true;
    }

    public boolean weakCompareAndSet(int index, E expected, E value) {
        return compareAndSet(index, expected, value);
    }

    @Override
    public String toString() {
        return java.util.Arrays.toString(slots);
    }
}
