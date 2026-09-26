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

import consulo.localization.LocalizationManager;
import consulo.localization.Localized;
import org.jspecify.annotations.Nullable;

/**
 * @author UNV
 * @since 2026-09-25
 */
public final class Localized2LocalizedValue extends CachingLocalizedValue {
    private final Localized myLocalized;

    public Localized2LocalizedValue(LocalizationManager manager, Localized localized) {
        super(manager);
        myLocalized = localized;
        if (localized.isEmpty()) {
            throw new IllegalArgumentException("Expecting non-empty argument");
        }
    }

    @Override
    protected String calcValue() {
        return myLocalized.toString();
    }

    @Override
    public String getId() {
        return myLocalized.getId() + "→LocalizedValue";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == this
            || obj instanceof Localized2LocalizedValue that && myLocalized.equals(that.myLocalized);
    }

    @Override
    protected int calcHashCode() {
        return myLocalized.hashCode();
    }
}