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

import consulo.localize.LocalizeManager;
import consulo.localize.Localized;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * @author UNV
 * @since 2026-09-25
 */
public final class Localized2LocalizeValue extends BaseLocalizeValue {
    private final Localized myLocalized;

    public Localized2LocalizeValue(Localized localized) {
        super(EMPTY_ARGS);
        myLocalized = localized;
    }

    @Override
    public boolean isEmpty() {
        return myLocalized.isEmpty();
    }

    @Override
    protected Map.Entry<Locale, String> getUnformattedText(LocalizeManager localizeManager) {
        return Map.entry(localizeManager.getLocale(), myLocalized.toString());
    }

    @Override
    public String getId() {
        return myLocalized.getId() + "→LocalizeValue";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == this
            || obj instanceof Localized2LocalizeValue that && myLocalized.equals(that.myLocalized);
    }

    @Override
    public int hashCode() {
        return myLocalized.hashCode();
    }
}
