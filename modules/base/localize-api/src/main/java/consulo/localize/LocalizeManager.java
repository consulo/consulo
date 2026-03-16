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

import consulo.disposer.Disposable;
import consulo.localize.internal.LocalizeManagerHolder;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public abstract class LocalizeManager {
    public static LocalizeManager get() {
        return LocalizeManagerHolder.get();
    }

    /**
     * Parse localizeKeyInfo
     *
     * @param localizeKeyInfo string like 'consulo.platform.base.IdeLocalize@text.some.value'
     * @return localize value, if key not found, or parsing error return localize value like parameter
     */
    public abstract LocalizeValue fromStringKey(String localizeKeyInfo);

    /**
     * Return unformatted localize text
     *
     * @throws IllegalArgumentException if key is invalid
     */
    public abstract Map.Entry<Locale, String> getUnformattedText(LocalizeKey key);
    public abstract Locale parseLocale(String localeText);

    public void setLocale(@Nullable Locale locale) {
        setLocale(locale, true);
    }

    public abstract void setLocale(@Nullable Locale locale, boolean fireEvents);
    public abstract Locale getLocale();
    public abstract Locale getAutoDetectedLocale();

    public abstract boolean isDefaultLocale();
    public abstract Set<Locale> getAvaliableLocales();

    public abstract void addListener(LocalizeManagerListener listener, Disposable disposable);

    public abstract byte getModificationCount();
    public abstract String formatText(String unformattedText, Locale locale, Object... args);
}
