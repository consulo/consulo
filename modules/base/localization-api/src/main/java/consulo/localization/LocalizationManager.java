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

import consulo.disposer.Disposable;
import consulo.localization.internal.LocalizationManagerHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public interface LocalizationManager {
    @Nonnull
    static LocalizationManager get() {
        return LocalizationManagerHolder.get();
    }

    /**
     * Parse localizeKeyInfo
     *
     * @param localizeKeyInfo string like 'consulo.platform.base.IdeLocalize@text.some.value'
     * @return localize value, if key not found, or parsing error return localize value like parameter
     */
    @Nonnull
    LocalizedValue fromStringKey(@Nonnull String localizeKeyInfo);

    /**
     * Return unformatted localize text
     *
     * @throws IllegalArgumentException if key is invalid
     */
    @Nonnull
    Map.Entry<Locale, String> getUnformattedText(@Nonnull LocalizationKey key);

    @Nonnull
    Locale parseLocale(@Nonnull String localeText);

    default void setLocale(@Nullable Locale locale) {
        setLocale(locale, true);
    }

    void setLocale(@Nullable Locale locale, boolean fireEvents);

    @Nonnull
    Locale getLocale();

    @Nonnull
    Locale getAutoDetectedLocale();

    boolean isDefaultLocale();

    @Nonnull
    Set<Locale> getAvailableLocales();

    void addListener(@Nonnull LocalizationManagerListener listener, @Nonnull Disposable disposable);

    byte getModificationCount();

    @Nonnull
    String formatText(String unformattedText, Locale locale, Object... args);
}
