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

import consulo.localization.LocalizationKey;
import consulo.localization.LocalizationManager;
import consulo.localization.LocalizedValue;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author VISTALL
 * @author NYUrchenko
 * @since 2020-05-20
 */
public /*final*/ class DefaultLocalizedValue extends CachingLocalizedValue {
    protected static final Object[] EMPTY_ARGS = new Object[0];

    @Nonnull
    private final LocalizationKey myLocalizationKey;
    @Nonnull
    private final Object[] myArgs;

    public DefaultLocalizedValue(@Nonnull LocalizationManager manager, @Nonnull LocalizationKey key) {
        this(manager, key, EMPTY_ARGS);
    }

    public DefaultLocalizedValue(@Nonnull LocalizationManager manager, @Nonnull LocalizationKey key, @Nonnull Object... args) {
        super(manager);
        myArgs = args;
        myLocalizationKey = key;
    }

    @Nonnull
    @Override
    public Optional<LocalizationKey> getKey() {
        return Optional.of(myLocalizationKey);
    }

    @Nonnull
    @Override
    protected String calcValue() {
        Map.Entry<Locale, String> unformattedText = myLocalizationManager.getUnformattedText(myLocalizationKey);
        if (myArgs.length > 0) {
            Object[] args = new Object[myArgs.length];
            // change LocalizeValue if found in args
            for (int i = 0; i < myArgs.length; i++) {
                Object oldValue = myArgs[i];

                args[i] = oldValue instanceof LocalizedValue oldLocalizedValue ? oldLocalizedValue.getValue() : oldValue;
            }

            return myLocalizationManager.formatText(unformattedText.getValue(), unformattedText.getKey(), args);
        }
        else {
            return unformattedText.getValue();
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o
            || o instanceof DefaultLocalizedValue that
            && Objects.equals(myLocalizationKey, that.myLocalizationKey)
            && Arrays.equals(myArgs, that.myArgs);
    }

    @Override
    public int calcHashCode() {
        return myLocalizationKey.hashCode() + 29 * Arrays.hashCode(myArgs);
    }
}
