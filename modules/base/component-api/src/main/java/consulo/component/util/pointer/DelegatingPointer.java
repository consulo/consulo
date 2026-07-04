// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.component.util.pointer;

import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

abstract class DelegatingPointer<T, U> implements Pointer<T> {

    private final Pointer<? extends U> myUnderlyingPointer;

    protected DelegatingPointer(Pointer<? extends U> underlyingPointer) {
        myUnderlyingPointer = underlyingPointer;
    }

    @Override
    public final @Nullable T dereference() {
        U underlyingValue = myUnderlyingPointer.dereference();
        return underlyingValue == null ? null : dereference(underlyingValue);
    }

    protected abstract T dereference(U underlyingValue);

    static final class ByValue<T, U> extends DelegatingPointer<T, U> {

        private final Function<? super U, ? extends T> myRestoration;

        ByValue(Pointer<? extends U> underlyingPointer,
                Function<? super U, ? extends T> restoration) {
            super(underlyingPointer);
            myRestoration = restoration;
        }

        @Override
        protected T dereference(U underlyingValue) {
            return myRestoration.apply(underlyingValue);
        }
    }

    static final class ByValueAndPointer<T, U> extends DelegatingPointer<T, U> {

        private final BiFunction<? super U, ? super Pointer<T>, ? extends T> myRestoration;

        ByValueAndPointer(Pointer<? extends U> underlyingPointer,
                          BiFunction<? super U, ? super Pointer<T>, ? extends T> restoration) {
            super(underlyingPointer);
            myRestoration = restoration;
        }

        @Override
        protected T dereference(U underlyingValue) {
            return myRestoration.apply(underlyingValue, this);
        }
    }
}
