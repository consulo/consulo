// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.review.util;

import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * You can use this class to model some outcome that may fail. Similar to a Result,
 * but with a specific error type. For instance {@code Either<Exception, Integer>} may model
 * the result of a division. The result will then either be some {@code ArithmeticException}
 * for instance, or a simple number.
 */
public sealed interface Either<A extends @Nullable Object, B extends @Nullable Object> permits Either.Left, Either.Right {
    static <A extends @Nullable Object, B extends @Nullable Object> Either<A, B> left(A value) {
        return new Left<>(value);
    }

    static <A extends @Nullable Object, B extends @Nullable Object> Either<A, B> right(B value) {
        return new Right<>(value);
    }

    default @Nullable A asLeftOrNull() {
        return this instanceof Left<A, B> left ? left.value() : null;
    }

    default @Nullable B asRightOrNull() {
        return this instanceof Right<A, B> right ? right.value() : null;
    }

    default boolean isLeft() {
        return this instanceof Left;
    }

    default boolean isRight() {
        return this instanceof Right;
    }

    default <A2 extends @Nullable Object, B2 extends @Nullable Object> Either<A2, B2> bimap(Function<? super A, ? extends A2> ifLeft,
                                                                                            Function<? super B, ? extends B2> ifRight) {
        if (this instanceof Left<A, B> left) {
            return new Left<>(ifLeft.apply(left.value()));
        }
        return new Right<>(ifRight.apply(((Right<A, B>) this).value()));
    }

    default <C extends @Nullable Object> C fold(Function<? super A, ? extends C> ifLeft, Function<? super B, ? extends C> ifRight) {
        if (this instanceof Left<A, B> left) {
            return ifLeft.apply(left.value());
        }
        return ifRight.apply(((Right<A, B>) this).value());
    }

    record Left<A extends @Nullable Object, B extends @Nullable Object>(A value) implements Either<A, B> {
    }

    record Right<A extends @Nullable Object, B extends @Nullable Object>(B value) implements Either<A, B> {
    }
}
