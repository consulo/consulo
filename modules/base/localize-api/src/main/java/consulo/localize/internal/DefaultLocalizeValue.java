/*
 * Copyright 2013-2020 consulo.io
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
import jakarta.annotation.Nonnull;

import java.util.Locale;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-05-20
 */
public final class DefaultLocalizeValue extends BaseLocalizeValue {
    private final LocalizeKey myLocalizeKey;

    public DefaultLocalizeValue(@Nonnull LocalizeKey localizeKey) {
        this(localizeKey, ourEmptyArgs);
    }

    public DefaultLocalizeValue(@Nonnull LocalizeKey localizeKey, @Nonnull Object... args) {
        super(args);
        myLocalizeKey = localizeKey;
    }

    @Nonnull
    @Override
    protected Map.Entry<Locale, String> getUnformattedText(@Nonnull LocalizeManager localizeManager) {
        return localizeManager.getUnformattedText(myLocalizeKey);
    }
}
