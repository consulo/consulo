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
package consulo.util.lang.ref;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author UNV
 * @since 2026-04-04
 */
public final class InitializedSimpleReference<T extends @Nullable Object> implements Supplier<T> {
    private boolean myInitialized = false;
    private @Nullable T myValue;

    private InitializedSimpleReference() {
        myInitialized = false;
        myValue = null;
    }

    private InitializedSimpleReference(T value) {
        myInitialized = true;
        myValue = value;
    }

    public boolean isInitialized() {
        return myInitialized;
    }

    @SuppressWarnings("NullAway")
    public T get() {
        if (!myInitialized) {
            throw new IllegalStateException("Reference is not initialized");
        }
        // Field myValue before initialization can contain technical null value.
        // If initialized, myValue nullability is the same as T nullability.
        // But we cannot describe it to a static validation, so disabling NullAway validation here.
        return myValue;
    }

    public void set(T value) {
        myInitialized = true;
        myValue = value;
    }

    public boolean setIfNotInitialized(T value) {
        if (!myInitialized) {
            myValue = value;
            myInitialized = true;
            return true;
        }
        return false;
    }

    public static <T extends @Nullable Object> InitializedSimpleReference<T> empty() {
        return new InitializedSimpleReference<>();
    }

    public static <T extends @Nullable Object> InitializedSimpleReference<T> of(T value) {
        return new InitializedSimpleReference<>(value);
    }

    @Override
    public String toString() {
        return String.valueOf(myValue);
    }
}
