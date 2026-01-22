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
package consulo.localization;

import consulo.localization.internal.DefaultLocalizationKey;
import consulo.localization.internal.LocalizationManagerHolder;
import jakarta.annotation.Nonnull;

import java.util.Locale;

/**
 * @author VISTALL
 * @author UNV
 * @since 2017-11-09
 */
public interface LocalizationKey {
    @Nonnull
    static LocalizationKey of(@Nonnull String localizationId, @Nonnull String key) {
        return new DefaultLocalizationKey(LocalizationManagerHolder.get(), localizationId, key.toLowerCase(Locale.ROOT));
    }

    @Nonnull
    static LocalizationKey of(@Nonnull String localizationId, @Nonnull String key, int argumentsCount) {
        // TODO [VISTALL] make optimization for future use on call #getValue()
        return of(localizationId, key);
    }

    @Nonnull
    String getLocalizationId();

    @Nonnull
    String getKey();

    @Nonnull
    LocalizedValue getValue();

    @Nonnull
    LocalizedValue getValue(@Nonnull Object... args);
}
