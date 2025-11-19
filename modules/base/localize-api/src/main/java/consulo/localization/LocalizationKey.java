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
package consulo.localization;

import consulo.localization.internal.DefaultLocalizationKey;
import consulo.localization.internal.LocalizationManagerHolder;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.Locale;

/**
 * @author VISTALL
 * @author NYUrchenko
 * @since 2017-11-09
 */
public interface LocalizationKey {
    @Nonnull
    static LocalizeKey of(@Nonnull String localizationId, @Nonnull String key) {
        return new DefaultLocalizationKey(LocalizationManagerHolder.get(), localizationId, key.toLowerCase(Locale.ROOT));
    }

    @Nonnull
    static LocalizeKey of(@Nonnull String localizationId, @Nonnull String key, int argumentsCount) {
        // TODO [VISTALL] make optimization for future use on call #getValue()
        return of(localizationId, key);
    }

    @Nonnull
    String getLocalizationId();

    @Nonnull
    String getKey();

    @Nonnull
    LocalizeValue getValue();

    @Deprecated(forRemoval = true)
    @Nonnull
    default LocalizeValue getValue(Object arg) {
        return getValue(new Object[] {arg});
    }

    @Deprecated(forRemoval = true)
    @Nonnull
    default LocalizeValue getValue(Object arg0, Object arg1) {
        return getValue(new Object[] {arg0, arg1});
    }

    @Deprecated(forRemoval = true)
    @Nonnull
    default LocalizeValue getValue(Object arg0, Object arg1, Object arg2) {
        return getValue(new Object[] {arg0, arg1, arg2});
    }

    @Deprecated(forRemoval = true)
    @Nonnull
    default LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3) {
        return getValue(new Object[] {arg0, arg1, arg2, arg3});
    }

    @Deprecated(forRemoval = true)
    @Nonnull
    default LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
        return getValue(new Object[] {arg0, arg1, arg2, arg3, arg4});
    }

    @Nonnull
    LocalizeValue getValue(@Nonnull Object... args);
}
