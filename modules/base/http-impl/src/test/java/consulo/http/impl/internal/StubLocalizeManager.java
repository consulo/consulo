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
package consulo.http.impl.internal;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeManagerListener;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The texts of the error messages built while a request fails - a localize key stands for its text, so the tests do not
 * need the localization of the platform.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
public class StubLocalizeManager extends LocalizeManager {
    @Override
    public LocalizeValue fromStringKey(String localizeKeyInfo) {
        return LocalizeValue.of(localizeKeyInfo);
    }

    @Override
    public LocalizeValue fromException(Throwable t) {
        return LocalizeValue.of(t.toString());
    }

    @Override
    public Map.Entry<Locale, String> getUnformattedText(LocalizeKey key) {
        return Map.entry(Locale.US, key.getLocalizeId() + "@" + key.getKey());
    }

    @Override
    public Locale parseLocale(String localeText) {
        return Locale.US;
    }

    @Override
    public void setLocale(@Nullable Locale locale, boolean fireEvents) {
    }

    @Override
    public Locale getLocale() {
        return Locale.US;
    }

    @Override
    public Locale getAutoDetectedLocale() {
        return Locale.US;
    }

    @Override
    public boolean isDefaultLocale() {
        return true;
    }

    @Override
    public Set<Locale> getAvailableLocales() {
        return Set.of(Locale.US);
    }

    @Override
    public void addListener(LocalizeManagerListener listener, Disposable disposable) {
    }

    @Override
    public byte getModificationCount() {
        return 1;
    }

    @Override
    public String formatText(String unformattedText, Locale locale, Object... args) {
        return args.length == 0 ? unformattedText : unformattedText + Arrays.toString(args);
    }
}
