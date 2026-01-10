/*
 * Copyright 2013-2025 consulo.io
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
package consulo.localize.internal;

import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author UNV
 * @since 2025-12-06
 */
public class EmptyLocalizeValue implements LocalizeValue {
    public static final EmptyLocalizeValue VALUE = new EmptyLocalizeValue();

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isNotEmpty() {
        return false;
    }

    @Nonnull
    @Override
    public String getValue() {
        return "";
    }

    @Nullable
    @Override
    public String getNullIfEmpty() {
        return null;
    }

    @Nonnull
    @Override
    public LocalizeValue orIfEmpty(LocalizeValue defaultValue) {
        return defaultValue;
    }

    @Override
    public byte getModificationCount() {
        return 0;
    }

    @Nonnull
    @Override
    public LocalizeValue map(@Nonnull Function<String, String> mapper) {
        return this;
    }

    @Nonnull
    @Override
    public LocalizeValue map(@Nonnull BiFunction<LocalizeManager, String, String> mapper) {
        return this;
    }

    @Override
    public int compareTo(@Nonnull LocalizeValue other) {
        return "".compareTo(other.getValue());
    }

    @Override
    public int compareIgnoreCase(@Nonnull LocalizeValue other) {
        return "".compareToIgnoreCase(other.getValue());
    }

    @Override
    public String toString() {
        return getValue();
    }

    private EmptyLocalizeValue() {
    }
}
