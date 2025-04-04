package consulo.application.util;

import consulo.util.lang.ObjectUtil;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SynchronizedClearableLazy<T> implements Supplier<T> {
    private final Supplier<T> initializer;
    private final AtomicReference<T> computedValue;

    private static final Object NOT_YET_INITIALIZED = ObjectUtil.sentinel("Not yet initialized");

    public SynchronizedClearableLazy(Supplier<T> initializer) {
        this.initializer = initializer;
        this.computedValue = new AtomicReference<>(notYetInitialized());
    }

    @SuppressWarnings("unchecked")
    private T notYetInitialized() {
        return (T) NOT_YET_INITIALIZED;
    }

    private T nullize(T t) {
        return isInitialized(t) ? t : null;
    }

    private boolean isInitialized(T t) {
        return t != NOT_YET_INITIALIZED;
    }

    /**
     * Returns the computed value if it has been initialized; otherwise returns null.
     */
    public T getValueIfInitialized() {
        return nullize(computedValue.get());
    }

    @Override
    public T get() {
        T currentValue = computedValue.get();
        if (isInitialized(currentValue)) {
            return currentValue;
        }
        synchronized (this) {
            // Under the lock, ensure initializer is called at most once.
            return computedValue.updateAndGet(old -> isInitialized(old) ? old : initializer.get());
        }
    }

    /**
     * Getter for the lazily computed value.
     */
    public T getValue() {
        return get();
    }

    /**
     * Setter to manually set the computed value.
     */
    public void setValue(T value) {
        computedValue.set(value);
    }

    /**
     * Checks if the value has been initialized.
     */
    public boolean isInitialized() {
        return isInitialized(computedValue.get());
    }

    @Override
    public String toString() {
        return computedValue.toString();
    }

    /**
     * Resets the computed value to uninitialized state and returns the previous value if it was initialized.
     */
    public T drop() {
        return nullize(computedValue.getAndSet(notYetInitialized()));
    }

    /**
     * Compares the current value with the expected value and resets it to the uninitialized state if they are equal.
     */
    public boolean compareAndDrop(T expectedValue) {
        return computedValue.compareAndSet(expectedValue, notYetInitialized());
    }
}
