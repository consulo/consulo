/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.module;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class ConfigurationErrorType {
    @Nonnull
    private final LocalizeValue myElementKind;
    private final boolean myCanIgnore;

    public ConfigurationErrorType(@Nonnull LocalizeValue elementKind, boolean canIgnore) {
        myElementKind = elementKind;
        myCanIgnore = canIgnore;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ConfigurationErrorType(String elementKind, boolean canIgnore) {
        this(LocalizeValue.ofNullable(elementKind), canIgnore);
    }

    @Nonnull
    public LocalizeValue getElementKind() {
        return myElementKind;
    }

    public boolean canIgnore() {
        return myCanIgnore;
    }
}
