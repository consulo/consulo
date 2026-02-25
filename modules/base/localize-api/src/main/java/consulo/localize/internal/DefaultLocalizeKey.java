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
package consulo.localize.internal;

import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-05-20
 */
@SuppressWarnings("deprecation")
public class DefaultLocalizeKey implements LocalizeKey {
    @Nonnull
    private final LocalizeManager myLocalizeManager;
    @Nonnull
    private final String myLocalizeId;
    @Nonnull
    private final String myKey;

    private LocalizeValue myDefaultValue;

    public DefaultLocalizeKey(@Nonnull LocalizeManager manager, @Nonnull String localizeId, @Nonnull String key) {
        myLocalizeManager = manager;
        myLocalizeId = localizeId;
        myKey = key;
    }

    @Nonnull
    @Override
    public String getLocalizationId() {
        return myLocalizeId;
    }

    @Nonnull
    @Override
    public String getKey() {
        return myKey;
    }

    @Nonnull
    @Override
    public LocalizeValue getValue() {
        LocalizeValue defaultValue = myDefaultValue;
        if (defaultValue != null) {
            return defaultValue;
        }

        myDefaultValue = defaultValue = new DefaultLocalizeValue(myLocalizeManager, this);
        return defaultValue;
    }

    @Nonnull
    @Override
    public LocalizeValue getValue(@Nonnull Object... args) {
        return new DefaultLocalizeValue(myLocalizeManager, this, args);
    }

    @Override
    public String toString() {
        return myLocalizeId + "@" + myKey;
    }
}
