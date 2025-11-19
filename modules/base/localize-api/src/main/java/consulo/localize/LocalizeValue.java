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
package consulo.localize;

import consulo.annotation.DeprecationInfo;
import consulo.localization.LocalizedValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author UNV
 * @since 2025-11-18
 */
public interface LocalizeValue extends LocalizedValue {
    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue empty() {
        return LocalizedValue.empty();
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue space() {
        return LocalizedValue.space();
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue colon() {
        return LocalizedValue.colon();
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue dot() {
        return LocalizedValue.dot();
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue questionMark() {
        return LocalizedValue.questionMark();
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue of() {
        return LocalizedValue.of();
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue localizeTODO(@Nonnull String text) {
        return LocalizedValue.localizeTODO(text);
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue of(@Nonnull String text) {
        return LocalizedValue.of(text);
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue of(char c) {
        return LocalizedValue.of(c);
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue ofNullable(@Nullable String text) {
        return LocalizedValue.ofNullable(text);
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue join(@Nonnull LocalizeValue... values) {
        return LocalizedValue.join(values);
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue join(@Nonnull String separator, @Nonnull LocalizeValue... values) {
        return LocalizedValue.join(separator, values);
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizedValue")
    @Nonnull
    static LocalizeValue joinWithSeparator(@Nonnull LocalizeValue separator, @Nonnull LocalizeValue... values) {
        return LocalizedValue.joinWithSeparator(separator, values);
    }
}
