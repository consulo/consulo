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
package consulo.application.util;

import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author UNV
 * @since 2026-02-19
 */
class TestLocalizedValue implements LocalizeValue {
    @Nonnull
    private final String myValue;

    TestLocalizedValue(@Nonnull String value) {
        myValue = value;
    }

    @Nonnull
    @Override
    public String getId() {
        return "";
    }

    @Nonnull
    @Override
    public String get() {
        return myValue;
    }

    @Nonnull
    @Override
    public String getValue() {
        return myValue;
    }

    @Override
    public byte getModificationCount() {
        return 0;
    }

    @Nonnull
    @Override
    public LocalizeValue map(@Nonnull Function<String, String> mapper) {
        return new TestLocalizedValue(mapper.apply(myValue));
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
            || obj instanceof TestLocalizedValue that
            && Objects.equals(myValue, that.myValue);
    }

    @Override
    public String toString() {
        return get();
    }
}
