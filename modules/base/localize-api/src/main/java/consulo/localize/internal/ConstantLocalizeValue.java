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

import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2017-11-09
 */
public final class ConstantLocalizeValue implements LocalizeValue {
    public static final ConstantLocalizeValue SPACE = new ConstantLocalizeValue(" ");
    public static final ConstantLocalizeValue COLON = new ConstantLocalizeValue(":");
    public static final ConstantLocalizeValue DOT = new ConstantLocalizeValue(".");
    public static final ConstantLocalizeValue QUESTION_MARK = new ConstantLocalizeValue("?");

    private final String myValue;

    public ConstantLocalizeValue(String value) {
        myValue = value;
    }

    @Nonnull
    @Override
    public String getId() {
        return '"' + myValue + '"';
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
    public String toString() {
        return getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConstantLocalizeValue that = (ConstantLocalizeValue) o;
        return Objects.equals(myValue, that.myValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myValue);
    }
}
