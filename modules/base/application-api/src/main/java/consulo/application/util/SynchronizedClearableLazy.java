package consulo.application.util;

import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SynchronizedClearableLazy<T extends @Nullable Object> implements Supplier<T> {
    private final Supplier<T> initializer;
    private final AtomicReference<T> computedValue = new AtomicReference<>(notYetInitialized());

    private static final Object NOT_YET_INITIALIZED = ObjectUtil.sentinel("Not yet initialized");

    public SynchronizedClearableLazy(Supplier<T> initializer) {
        this.initializer = initializer;
    }

    @SuppressWarnings("unchecked")
    private static <T> T notYetInitialized() {
        return (T) NOT_YET_INITIALIZED;
    }

    @SuppressWarnings("NullAway")
    private static <T extends @Nullable Object> T nullize(T t) {
        // NullAway problem: this null can be returned only if T is nullable, if T is not-nullable, null would never be returned
        // Static validator doesn't understand that this case is safe, so suppressing NullAway validation
        return isInitialized(t) ? t : null;
    }

    private static <T> boolean isInitialized(T t) {
        return t != NOT_YET_INITIALIZED;
    }

    /**
     * Returns the computed value if it has been initialized; otherwise returns null.
     */
    public @Nullable T getValueIfInitialized() {
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
    public @Nullable T drop() {
        return nullize(computedValue.getAndSet(notYetInitialized()));
    }

    /**
     * Compares the current value with the expected value and resets it to the uninitialized state if they are equal.
     */
    public boolean compareAndDrop(T expectedValue) {
        return computedValue.compareAndSet(expectedValue, notYetInitialized());
    }
}
