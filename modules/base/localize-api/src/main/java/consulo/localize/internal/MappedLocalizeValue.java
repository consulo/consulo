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
import consulo.localization.internal.MappedLocalizedValue;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-07-30
 */
public class MappedLocalizeValue extends MappedLocalizedValue implements LocalizeValue {
    public MappedLocalizeValue(
        @Nonnull LocalizeManager manager,
        @Nonnull LocalizedValue delegate,
        @Nonnull Function<String, String> mapper
    ) {
        super(manager, delegate, mapper);
    }
}
