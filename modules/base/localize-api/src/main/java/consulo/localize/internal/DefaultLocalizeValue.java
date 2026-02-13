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

import consulo.localization.internal.DefaultLocalizedValue;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 2020-05-20
 */
@SuppressWarnings("deprecation")
public class DefaultLocalizeValue extends DefaultLocalizedValue implements LocalizeValue {
    public static final Comparator<LocalizeValue> CASE_INSENSITIVE_ORDER = (lv1, lv2) -> {
        String v1 = lv1.get(), v2 = lv2.get();
        int insensitive = v1.compareToIgnoreCase(v2);
        return insensitive != 0 ? insensitive : v1.compareTo(v2);
    };

    public DefaultLocalizeValue(@Nonnull LocalizeManager manager, @Nonnull LocalizeKey key) {
        super(manager, key);
    }

    public DefaultLocalizeValue(@Nonnull LocalizeManager manager, @Nonnull LocalizeKey key, @Nonnull Object... args) {
        super(manager, key, args);
    }
}
