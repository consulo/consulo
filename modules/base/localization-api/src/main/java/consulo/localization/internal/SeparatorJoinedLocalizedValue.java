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

/**
 * @author VISTALL
 * @author NYUrchenko
 * @since 2025-09-13
 */
public /*final*/ class SeparatorJoinedLocalizedValue extends JoinedLocalizedValue {
    @Nonnull
    private final String mySeparator;

    public SeparatorJoinedLocalizedValue(
        @Nonnull LocalizationManager manager,
        @Nonnull String separator,
        @Nonnull LocalizedValue[] values
    ) {
        super(manager, values);
        mySeparator = separator;
    }

    @Nonnull
    @Override
    protected String calcValue() {
        StringBuilder builder = new StringBuilder();
        boolean needsSeparator = false;
        for (LocalizedValue value : myValues) {
            if (value == LocalizedValue.empty()) {
                continue;
            }

            if (needsSeparator) {
                builder.append(mySeparator);
            }

            builder.append(value.getValue());

            needsSeparator = true;
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o == this
            || super.equals(o)
            && o instanceof SeparatorJoinedLocalizedValue that
            && mySeparator.equals(that.mySeparator);
    }
}
