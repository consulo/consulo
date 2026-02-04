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
package consulo.localization.internal;

import consulo.localization.LocalizationManager;
import consulo.localization.LocalizedValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author UNV
 * @since 2025-12-06
 */
public /*final*/ class EmptyLocalizedValue implements LocalizedValue {
    public static final EmptyLocalizedValue VALUE = new EmptyLocalizedValue();

    private static final String EMPTY_STRING = "";

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
        return EMPTY_STRING;
    }

    @Nullable
    @Override
    public String getNullIfEmpty() {
        return null;
    }

    @Nonnull
    @Override
    public LocalizedValue orIfEmpty(LocalizedValue defaultValue) {
        return defaultValue;
    }

    @Override
    public byte getModificationCount() {
        return 0;
    }

    @Nonnull
    @Override
    public LocalizedValue map(@Nonnull Function<String, String> mapper) {
        return this;
    }

    @Nonnull
    @Override
    public LocalizedValue map(@Nonnull BiFunction<LocalizationManager, String, String> mapper) {
        return this;
    }

    @Override
    public String toString() {
        return getValue();
    }

    protected EmptyLocalizedValue() {
    }
}
