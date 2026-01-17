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
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-05-20
 */
public final class DefaultLocalizeKey implements LocalizeKey {
    private final String myLocalizeId;
    private final String myKey;

    private LocalizeValue myDefaultValue;

    public DefaultLocalizeKey(String localizeId, String key) {
        myLocalizeId = localizeId;
        myKey = key;
    }

    @Nonnull
    @Override
    public String getLocalizeId() {
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

        defaultValue = new DefaultLocalizeValue(this);
        myDefaultValue = defaultValue;
        return defaultValue;
    }

    @Nonnull
    @Override
    public LocalizeValue getValue(Object[] args) {
        return new DefaultLocalizeValue(this, args);
    }

    @Override
    public String toString() {
        return myLocalizeId + "@" + myKey;
    }
}
