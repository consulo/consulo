/*
 * Copyright 2013-2019 consulo.io
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

import consulo.container.plugin.util.PlatformServiceLoader;
import consulo.disposer.Disposable;

import jakarta.annotation.Nonnull;

import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public abstract class LocalizeManager {
    private static LocalizeManager ourInstance = PlatformServiceLoader.findImplementation(LocalizeManager.class, ServiceLoader::load);

    @Nonnull
    public static LocalizeManager get() {
        return ourInstance;
    }

    /**
     * Parse localizeKeyInfo
     *
     * @param localizeKeyInfo string like 'consulo.platform.base.IdeLocalize@text.some.value'
     * @return localize value, if key not found, or parsing error return localize value like parameter
     */
    @Nonnull
    public abstract LocalizeValue fromStringKey(@Nonnull String localizeKeyInfo);

    /**
     * Return unformatted localize text
     *
     * @throws IllegalArgumentException if key is invalid
     */
    @Nonnull
    public abstract Map.Entry<Locale, String> getUnformattedText(@Nonnull LocalizeKey key);

    @Nonnull
    public abstract Locale parseLocale(@Nonnull String localeText);

    public void setLocale(@Nonnull Locale locale) {
        setLocale(locale, true);
    }

    public abstract void setLocale(@Nonnull Locale locale, boolean fireEvents);

    @Nonnull
    public abstract Locale getLocale();

    public abstract boolean isDefaultLocale();

    @Nonnull
    public abstract Set<Locale> getAvaliableLocales();

    public abstract void addListener(@Nonnull LocalizeManagerListener listener, @Nonnull Disposable disposable);

    public abstract long getModificationCount();

    @Nonnull
    public abstract String formatText(String unformattedText, Locale locale, Object... args);
}
