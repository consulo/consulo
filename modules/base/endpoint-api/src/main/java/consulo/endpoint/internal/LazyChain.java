// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.internal;

import consulo.util.lang.lazy.LazyValue;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public sealed abstract class LazyChain<T> permits LazyChain.Immediate, LazyChain.Delayed {
    private LazyChain() {
    }

    public abstract T getValue();

    public abstract LazyChain<T> chain(UnaryOperator<T> trans);

    public LazyChain<T> chainLazy(UnaryOperator<T> trans) {
        return new Delayed<>(() -> trans.apply(getValue()));
    }

    public static final class Immediate<T> extends LazyChain<T> {
        private final T myValue;

        public Immediate(T value) {
            myValue = value;
        }

        @Override
        public T getValue() {
            return myValue;
        }

        @Override
        public Immediate<T> chain(UnaryOperator<T> trans) {
            return new Immediate<>(trans.apply(myValue));
        }
    }

    private static final class Delayed<T> extends LazyChain<T> {
        private final LazyValue<T> myValue;

        private Delayed(Supplier<T> trans) {
            myValue = LazyValue.notNull(trans);
        }

        @Override
        public T getValue() {
            return myValue.get();
        }

        @Override
        public LazyChain<T> chain(UnaryOperator<T> trans) {
            return chainLazy(trans);
        }
    }
}
