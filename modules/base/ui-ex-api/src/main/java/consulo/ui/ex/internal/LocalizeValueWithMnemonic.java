/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ui.ex.internal;

import consulo.localize.LocalizeValue;
import consulo.ui.util.TextWithMnemonic;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-10-21
 */
public interface LocalizeValueWithMnemonic extends LocalizeValue {
    static TextWithMnemonic get(@Nonnull LocalizeValue localizeValue) {
        if (localizeValue instanceof LocalizeValueWithMnemonic withMnemonic) {
            return withMnemonic.mnemonic();
        }

        return TextWithMnemonic.parse(localizeValue.get());
    }

    @Nonnull
    TextWithMnemonic mnemonic();
}
