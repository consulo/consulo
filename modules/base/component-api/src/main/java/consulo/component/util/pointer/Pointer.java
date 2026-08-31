// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.component.util.pointer;

import consulo.annotation.access.RequiredReadAction;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * <h3>Example 1</h3>
 * <p>
 * Smart pointers might be used to restore the element across different read actions.
 * </p>
 * <p>
 * Elements are expected to stay valid within a single read action.
 * It's highly advised to split long read actions into several short ones, but this also means
 * that some write action might be run in between these short read actions,
 * which could potentially change the model of the element (reference model, PSI model, framework model or whatever model).
 * </p>
 * <pre>{@code
 * Pointer<T> pointer = readAction(() -> {
 *   T instance = obtainSomeInstanceWhichIsValidWithinAReadAction();
 *   return instance.createPointer();
 * });
 * // the pointer might be safely stored in the UI or another model for later usage
 * readAction(() -> { // another read action
 *   T restoredInstance = pointer.dereference();
 *   if (restoredInstance == null) {
 *     // instance was invalidated, act accordingly
 *     return;
 *   }
 *   // at this point the instance is valid because it should've not exist if it's not
 *   doSomething(restoredInstance);
 * });
 *
 * readAction(() -> {
 *   // same pointer may be used in several subsequent read actions
 *   T restoredInstance = pointer.dereference();
 *   ...
 * });
 * }</pre>
 *
 * <h3>Example 2</h3>
 * <p>
 * Pointers might be used to avoid hard references to the element to save the memory.
 * In this case the pointer stores minimal needed information to be able to restore the element when requested.
 * </p>
 *
 * <h3>Equality</h3>
 * <p>
 * It's expected that most pointers would require a read action for comparison, thus no equality is defined for pointers.
 * Pointers should be {@linkplain Pointer#dereference de-referenced} in a read action, and their values should be compared instead.
 * </p>
 *
 * @param <T> type of underlying element
 */
public interface Pointer<T> {

    /**
     * Dereferences this pointer to the current value.
     * <p>
     * Must be called under read lock and from a background thread.
     * Must not be called under write lock.
     * </p>
     * <p>
     * The returned value is expected to be valid in the current read action.
     * To use a value in another read action, create and dereference a pointer again.
     * </p>
     *
     * @return referenced value, or {@code null} if the value was invalidated or cannot be restored
     */
    @RequiredReadAction
    @Nullable
    T dereference();

    /**
     * Creates a pointer which holds a strong reference to {@code value}.
     * The pointer is always dereferenced to the same object.
     * <p>
     * Use only for values that are known to be non-invalidating and safe to retain strongly.
     * </p>
     */
    static <T> Pointer<T> hardPointer(T value) {
        return () -> value;
    }

    /**
     * Creates a pointer which restores its value from {@code underlyingPointer}
     * using {@code restoration}.
     * <p>
     * If the underlying value cannot be restored, this pointer dereferences to {@code null}.
     * {@code restoration} may also return {@code null}.
     * </p>
     */
    static <T, U> Pointer<T> delegatingPointer(
        Pointer<? extends U> underlyingPointer,
        Function<? super U, ? extends @Nullable T> restoration
    ) {
        return new DelegatingPointer.ByValue<>(underlyingPointer, restoration);
    }

    /**
     * Creates the same pointer as {@link #delegatingPointer(Pointer, Function)}, and additionally passes the created pointer to
     * {@code restoration} so the restored value may cache it.
     * <p>
     * This is useful when the restored value is a short-lived object recreated in every read action and wants
     * to reuse the pointer that produced it on subsequent {@code createPointer()} calls, instead of allocating a fresh pointer each
     * time. The restored value stores the pointer in a field; the next {@code createPointer()} call short-circuits and returns the cached
     * pointer.
     * </p>
     */
    static <T, U> Pointer<T> selfDelegatingPointer(
        Pointer<? extends U> underlyingPointer,
        BiFunction<? super U, ? super Pointer<T>, ? extends @Nullable T> restoration
    ) {
        return new DelegatingPointer.ByValueAndPointer<>(underlyingPointer, restoration);
    }
}
