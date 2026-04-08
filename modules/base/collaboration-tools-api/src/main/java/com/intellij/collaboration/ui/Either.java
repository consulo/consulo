// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Function;

/**
 * Represents a union type. Either the left value or the right value is present.
 * The right value is by convention the most expected value while the left value is
 * usually the exceptional value.
 * <p>
 * You can use this class to model some outcome that may fail. Similar to a Result,
 * but with a specific error type. For instance {@code Either<Exception, Int>} may model
 * the result of a division. The result will then either be some {@code DivideByZeroException}
 * for instance, or a simple number.
 */
@ApiStatus.Internal
public sealed interface Either<A, B> {
    default @Nullable A asLeftOrNull() {
        return this instanceof Left<A, ?> left ? left.value() : null;
    }

    default @Nullable B asRightOrNull() {
        return this instanceof Right<?, B> right ? right.value() : null;
    }

    default boolean isLeft() {
        return this instanceof Left;
    }

    default boolean isRight() {
        return this instanceof Right;
    }

    default <A2, B2> @Nonnull Either<A2, B2> bimap(
        @Nonnull Function<? super A, ? extends A2> ifLeft,
        @Nonnull Function<? super B, ? extends B2> ifRight
    ) {
        if (this instanceof Left<A, B> left) {
            return new Left<>(ifLeft.apply(left.value()));
        }
        else if (this instanceof Right<A, B> right) {
            return new Right<>(ifRight.apply(right.value()));
        }
        throw new IllegalStateException();
    }

    default <C> C fold(
        @Nonnull Function<? super A, ? extends C> ifLeft,
        @Nonnull Function<? super B, ? extends C> ifRight
    ) {
        if (this instanceof Left<A, B> left) {
            return ifLeft.apply(left.value());
        }
        else if (this instanceof Right<A, B> right) {
            return ifRight.apply(right.value());
        }
        throw new IllegalStateException();
    }

    record Left<A, B>(A value) implements Either<A, B> {
    }

    record Right<A, B>(B value) implements Either<A, B> {
    }

    static <A> @Nonnull Either<A, ?> left(A value) {
        return new Left<>(value);
    }

    static <B> @Nonnull Either<?, B> right(B value) {
        return new Right<>(value);
    }
}
