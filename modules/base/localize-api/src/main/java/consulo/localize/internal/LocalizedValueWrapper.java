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

import consulo.localization.LocalizedValue;
import consulo.localization.internal.EmptyLocalizedValue;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2026-01-12
 */
@SuppressWarnings("deprecation")
public class LocalizedValueWrapper implements LocalizeValue {
    private final LocalizedValue myLocalizedValue;

    @SuppressWarnings("deprecation")
    public static LocalizeValue wrap(LocalizedValue localizedValue) {
        return switch (localizedValue) {
            case LocalizeValue lv -> lv;
            case EmptyLocalizedValue empty -> EmptyLocalizeValue.VALUE;
            default -> new LocalizedValueWrapper(localizedValue);
        };
    }
    @Nonnull
    @Override
    public String getValue() {
        return myLocalizedValue.getValue();
    }

    @Override
    public byte getModificationCount() {
        return myLocalizedValue.getModificationCount();
    }

    private LocalizedValueWrapper(LocalizedValue localizedValue) {
        myLocalizedValue = localizedValue;
    }
}
