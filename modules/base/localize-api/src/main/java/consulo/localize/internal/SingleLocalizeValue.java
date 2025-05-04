/*
 * Copyright 2013-2017 consulo.io
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
 * @since 09-Nov-17
 */
public class SingleLocalizeValue implements LocalizeValue {
    public static final SingleLocalizeValue ourEmpty = new SingleLocalizeValue("");
    public static final SingleLocalizeValue ourSpace = new SingleLocalizeValue(" ");
    public static final SingleLocalizeValue ourColon = new SingleLocalizeValue(":");
    public static final SingleLocalizeValue ourDot = new SingleLocalizeValue(".");
    public static final SingleLocalizeValue ourQuestionMark = new SingleLocalizeValue("?");

    private final String myValue;

    public SingleLocalizeValue(String value) {
        myValue = value;
    }

    @Nonnull
    @Override
    public String getValue() {
        return myValue;
    }

    @Override
    public long getModificationCount() {
        return 0;
    }

    @Override
    public int compareIgnoreCase(@Nonnull LocalizeValue other) {
        return getValue().compareToIgnoreCase(other.getValue());
    }

    @Override
    public int compareTo(@Nonnull LocalizeValue o) {
        return getValue().compareTo(o.getValue());
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
        SingleLocalizeValue that = (SingleLocalizeValue)o;
        return Objects.equals(myValue, that.myValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myValue);
    }
}
