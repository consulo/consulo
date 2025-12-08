/*
 * Copyright 2013-2017 consulo.io
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
import consulo.localization.LocalizationKey;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2017-11-09
 */
public interface LocalizeKey extends LocalizationKey {
    @Deprecated
    @DeprecationInfo("Use methods of LocalizationKey")
    @Nonnull
    static LocalizeKey of(@Nonnull String localizationId, @Nonnull String key) {
        return LocalizationKey.of(localizationId, key);
    }

    @Deprecated
    @DeprecationInfo("Use methods of LocalizationKey")
    @Nonnull
    static LocalizeKey of(@Nonnull String localizationId, @Nonnull String key, int argumentsCount) {
        return LocalizationKey.of(localizationId, key, argumentsCount);
    }
}
