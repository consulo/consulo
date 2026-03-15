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
package consulo.test.light.impl;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeManagerListener;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-02-28
 */
public class LightLocalizationManager extends LocalizeManager {
    private Locale myLocale = Locale.US;

    
    @Override
    public LocalizeValue fromStringKey(String localizeKeyInfo) {
        List<String> values = StringUtil.split(localizeKeyInfo, "@");
        if (values.size() != 2) {
            return LocalizeValue.of(localizeKeyInfo);
        }

        LocalizeKey localizeKey = LocalizeKey.of(values.get(0), values.get(1));
        return localizeKey.getValue();
    }

    
    @Override
    public Map.Entry<Locale, String> getUnformattedText(LocalizeKey key) {
        return Map.entry(myLocale, "[" + key.toString() + "]");
    }

    
    @Override
    public Locale parseLocale(String localeText) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocale(@Nullable Locale locale, boolean fireEvents) {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public Locale getLocale() {
        return myLocale;
    }

    
    @Override
    public Locale getAutoDetectedLocale() {
        return myLocale;
    }

    @Override
    public boolean isDefaultLocale() {
        return true;
    }

    
    @Override
    public Set<Locale> getAvaliableLocales() {
        return Set.of(myLocale);
    }

    @Override
    public void addListener(LocalizeManagerListener listener, Disposable disposable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte getModificationCount() {
        return 1;
    }

    
    @Override
    public String formatText(String unformattedText, Locale locale, Object[] args) {
        if (args.length == 0) {
            return unformattedText;
        }

        StringBuilder builder = new StringBuilder(unformattedText);
        builder.append("(");
        for (int i = 0; i < args.length; i++) {
            if (i != 0) {
                builder.append(", ");
            }

            Object arg = args[i];
            if (arg instanceof LocalizeValue localizeValue) {
                builder.append(localizeValue.get());
            } else {
                builder.append(String.valueOf(arg));
            }
        }
        builder.append(")");
        return builder.toString();
    }
}
