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

import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;

/**
 * @author VISTALL
 * @since 2025-09-18
 */
public final class SeparatorJoinedLocalizeValue2 extends BaseJoinedLocalizeValue {
    private final LocalizeValue mySeparator;

    public SeparatorJoinedLocalizeValue2(LocalizeValue separator, LocalizeValue[] values) {
        super(values);
        mySeparator = separator;
    }
    @Override
    public String getId() {
        return super.getId() + '(' + mySeparator.getId() + ')';
    }
    @Override
    protected String calcValue(LocalizeManager manager) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < myArgs.length; i++) {
            if (i != 0) {
                builder.append(mySeparator.getValue());
            }

            Object ar = myArgs[i];

            String value = ar instanceof LocalizeValue lv ? lv.getValue() : String.valueOf(ar);

            builder.append(value);
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o == this
            || super.equals(o)
            && o instanceof SeparatorJoinedLocalizeValue2 that
            && mySeparator.equals(that.mySeparator);
    }
}
