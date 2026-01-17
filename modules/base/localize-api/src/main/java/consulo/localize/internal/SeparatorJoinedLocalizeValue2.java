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

import consulo.localization.internal.SeparatorJoinedLocalizedValue2;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-09-18
 */
@SuppressWarnings("deprecation")
public class SeparatorJoinedLocalizeValue2 extends SeparatorJoinedLocalizedValue2 implements LocalizeValue {
    public SeparatorJoinedLocalizeValue2(
        @Nonnull LocalizeManager manager,
        @Nonnull LocalizeValue separator,
        @Nonnull LocalizeValue[] values
    ) {
        super(manager, separator, values);
    }
}
