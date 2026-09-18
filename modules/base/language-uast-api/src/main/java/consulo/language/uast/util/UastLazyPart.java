// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.uast.util;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * More lightweight replacement for {@code lazy { }} inside UAST element implementations.
 * It is used to decrease memory allocations during {@code toUElement()} conversions.
 */
public final class UastLazyPart<T extends @Nullable Object> {
    public static final Object UNINITIALIZED_UAST_PART = new Object() {
        @Override
        public String toString() {
            return "UNINITIALIZED_UAST_PART";
        }
    };

    private @Nullable Object myValue = UNINITIALIZED_UAST_PART;

    public @Nullable Object getValue() {
        return myValue;
    }

    public void setValue(@Nullable Object value) {
        myValue = value;
    }

    // NullAway: the part holds the sentinel or a value of T, which may itself be a nullable type
    @SuppressWarnings({"unchecked", "NullAway"})
    public static <T extends @Nullable Object> T getOrBuild(UastLazyPart<T> part, Supplier<T> initializer) {
        Object current = part.myValue;
        if (current == UNINITIALIZED_UAST_PART) {
            current = initializer.get();
            part.myValue = current;
        }
        return (T) current;
    }

    @Override
    public String toString() {
        if (myValue == UNINITIALIZED_UAST_PART) {
            return "UastLazyPart()";
        }

        return "UastLazyPart(" + myValue + ")";
    }
}
