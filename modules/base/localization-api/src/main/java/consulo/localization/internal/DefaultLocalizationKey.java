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

/**
 * @author VISTALL
 * @author NYUrchenko
 * @since 2020-05-20
 */
public final class DefaultLocalizationKey implements LocalizationKey {
    @Nonnull
    private final LocalizationManager myLocalizationManager;
    @Nonnull
    private final String myLocalizationId;
    @Nonnull
    private final String myKey;

    private LocalizedValue myDefaultValue;

    public DefaultLocalizationKey(@Nonnull LocalizationManager manager, @Nonnull String localizationId, @Nonnull String key) {
        myLocalizationManager = manager;
        myLocalizationId = localizationId;
        myKey = key;
    }

    @Nonnull
    @Override
    public String getLocalizationId() {
        return myLocalizationId;
    }

    @Nonnull
    @Override
    public String getKey() {
        return myKey;
    }

    @Nonnull
    @Override
    public LocalizedValue getValue() {
        LocalizedValue defaultValue = myDefaultValue;
        if (defaultValue != null) {
            return defaultValue;
        }

        myDefaultValue = defaultValue = new DefaultLocalizedValue(myLocalizationManager, this);
        return defaultValue;
    }

    @Nonnull
    @Override
    public LocalizedValue getValue(@Nonnull Object... args) {
        return new DefaultLocalizedValue(myLocalizationManager, this, args);
    }

    @Override
    public String toString() {
        return myLocalizationId + "@" + myKey;
    }
}
