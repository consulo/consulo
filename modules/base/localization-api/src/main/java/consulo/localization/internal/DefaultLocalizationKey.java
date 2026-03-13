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
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @author UNV
 * @since 2020-05-20
 */
public final class DefaultLocalizationKey implements LocalizationKey {
    private final LocalizationManager myLocalizationManager;
    private final String myLocalizationId;
    private final String myKey;

    @Nullable
    private LocalizedValue myDefaultValue = null;

    public DefaultLocalizationKey(LocalizationManager manager, String localizationId, String key) {
        myLocalizationManager = manager;
        myLocalizationId = localizationId;
        myKey = key;
    }

    @Override
    public String getLocalizationId() {
        return myLocalizationId;
    }

    @Override
    public String getKey() {
        return myKey;
    }

    @Override
    public LocalizedValue getValue() {
        LocalizedValue defaultValue = myDefaultValue;
        if (defaultValue != null) {
            return defaultValue;
        }

        myDefaultValue = defaultValue = new DefaultLocalizedValue(myLocalizationManager, this);
        return defaultValue;
    }

    @Override
    public LocalizedValue getValue(Object... args) {
        return new DefaultLocalizedValue(myLocalizationManager, this, args);
    }

    @Override
    public String toString() {
        return myLocalizationId + "@" + myKey;
    }
}
