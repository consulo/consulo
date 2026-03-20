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
package consulo.localize.internal;

import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author UNV
 * @since 2025-12-06
 */
public final class EmptyLocalizeValue implements LocalizeValue {
    public static final EmptyLocalizeValue VALUE = new EmptyLocalizeValue();

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isNotEmpty() {
        return false;
    }
    @Override
    public String getId() {
        return "empty";
    }
    @Override
    public String getValue() {
        return "";
    }

    @Override
    public @Nullable String getNullIfEmpty() {
        return null;
    }
    @Override
    public LocalizeValue orIfEmpty(LocalizeValue defaultValue) {
        return defaultValue;
    }

    @Override
    public byte getModificationCount() {
        return 0;
    }
    @Override
    public LocalizeValue map(Function<String, String> mapper) {
        return this;
    }
    @Override
    public LocalizeValue map(BiFunction<LocalizeManager, String, String> mapper) {
        return this;
    }

    @Override
    public String toString() {
        return getValue();
    }

    private EmptyLocalizeValue() {
    }
}
