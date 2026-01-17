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

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @author NYUrchenko
 * @since 2017-11-09
 */
public /*final*/ class ConstantLocalizedValue extends AbstractLocalizedValue {
    public static final ConstantLocalizedValue SPACE = new ConstantLocalizedValue(" ");
    public static final ConstantLocalizedValue COLON = new ConstantLocalizedValue(":");
    public static final ConstantLocalizedValue DOT = new ConstantLocalizedValue(".");
    public static final ConstantLocalizedValue QUESTION_MARK = new ConstantLocalizedValue("?");

    @Nonnull
    private final String myValue;

    public ConstantLocalizedValue(@Nonnull String value) {
        myValue = value;
    }

    @Nonnull
    @Override
    public String getValue() {
        return myValue;
    }

    @Override
    public byte getModificationCount() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return this == o
            || o instanceof ConstantLocalizedValue that
            && myValue.equals(that.myValue);
    }

    @Override
    public int hashCode() {
        return myValue.hashCode();
    }
}
