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
 * @author UNV
 * @since 2026-02-10
 */
public /*sealed*/ class AbstractJoinedLocalizedValue extends CachingLocalizedValue
    /*permits JoinedLocalizedValue, SeparatorJoinedLocalizedValue, SeparatorJoinedLocalizedValue2*/ {

    @Nonnull
    protected final LocalizedValue[] myValues;

    protected AbstractJoinedLocalizedValue(@Nonnull LocalizationManager manager, @Nonnull LocalizedValue[] values) {
        super(manager);
        myValues = values;
    }

    @Nonnull
    @Override
    public String getId() {
        StringBuilder sb = new StringBuilder().append('[');
        for (int i = 0, n = myValues.length; i < n; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(myValues[i].getId());
        }
        return sb.append("]->join").toString();
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
            || o instanceof AbstractJoinedLocalizedValue that
            && Arrays.equals(myValues, that.myValues);
    }
}
