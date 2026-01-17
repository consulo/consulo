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
package consulo.localization.internal;

import consulo.localization.LocalizationManager;
import consulo.localization.LocalizedValue;
import jakarta.annotation.Nonnull;

import java.util.Arrays;

/**
 * @author VISTALL
 * @author NYUrchenko
 * @since 2021-09-24
 */
public /*sealed*/ class JoinedLocalizedValue extends CachingLocalizedValue
    /*permits SeparatorJoinedLocalizedValue, SeparatorJoinedLocalizedValue2*/ {
    @Nonnull
    protected final LocalizedValue[] myValues;

    public JoinedLocalizedValue(@Nonnull LocalizationManager manager, @Nonnull LocalizedValue[] values) {
        super(manager);
        myValues = values;
    }

    @Nonnull
    @Override
    protected String calcValue() {
        StringBuilder builder = new StringBuilder();
        for (LocalizedValue value : myValues) {
            builder.append(value.getValue());
        }
        return builder.toString();
    }

    @Override
    protected int calcHashCode() {
        return Arrays.hashCode(myValues);
    }

    @Override
    public boolean equals(Object o) {
        return o == this
            || o instanceof JoinedLocalizedValue that
            && Arrays.equals(myValues, that.myValues);
    }
}
