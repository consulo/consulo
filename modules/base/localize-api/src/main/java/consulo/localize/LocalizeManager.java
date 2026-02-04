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
package consulo.localize;

import consulo.annotation.DeprecationInfo;
import consulo.localization.LocalizationManager;
import consulo.localize.internal.LocalizeManagerHolder;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
@Deprecated
@DeprecationInfo("Use LocalizationManager")
@SuppressWarnings("deprecation")
public interface LocalizeManager extends LocalizationManager {
    @Nonnull
    static LocalizeManager get() {
        return LocalizeManagerHolder.get();
    }

    /**
     * Parse localizeKeyInfo
     *
     * @param localizeKeyInfo string like 'consulo.platform.base.IdeLocalize@text.some.value'
     * @return localize value, if key not found, or parsing error return localize value like parameter
     */
    @Nonnull
    @Override
    LocalizeValue fromStringKey(@Nonnull String localizeKeyInfo);
}
