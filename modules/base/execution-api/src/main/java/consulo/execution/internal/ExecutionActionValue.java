/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.internal;

import consulo.localization.LocalizedValue;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.util.TextWithMnemonic;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2024-10-21
 */
public final class ExecutionActionValue implements LocalizeValueWithMnemonic {
    private static final String REPLACEMENT_CHAR = "⊹";

    @Nonnull
    public static LocalizeValue buildWithConfiguration(@Nonnull Function<String, LocalizeValue> function, String configurationName) {
        String dummyStr = REPLACEMENT_CHAR.repeat(configurationName.length());

        LocalizeValue dummyValue = function.apply(dummyStr);
        return new ExecutionActionValue(dummyValue, dummyStr, configurationName);
    }

    @Nonnull
    private final LocalizeValue myOriginal;
    @Nonnull
    private final String myParamValue;
    @Nonnull
    private final String myConfigurationName;

    public ExecutionActionValue(@Nonnull LocalizeValue original, @Nonnull String paramValue, @Nonnull String configurationName) {
        myOriginal = original;
        myParamValue = paramValue;
        myConfigurationName = configurationName;
    }

    @Nonnull
    @Override
    public TextWithMnemonic mnemonic() {
        TextWithMnemonic text = TextWithMnemonic.parse(myOriginal.getValue());
        if (!text.hasMnemonic()) {
            return text;
        }

        return text.replaceFirst(myParamValue, myConfigurationName);
    }

    @Nonnull
    @Override
    public String getValue() {
        return myOriginal.getValue().replace(myParamValue, myConfigurationName);
    }

    @Override
    public byte getModificationCount() {
        return myOriginal.getModificationCount();
    }

    @Override
    public int compareTo(@Nonnull LocalizedValue that) {
        return myOriginal.compareTo(that);
    }

    @Override
    public boolean equals(Object o) {
        return this == o
            || o instanceof ExecutionActionValue that
            && myOriginal.equals(that.myOriginal)
            && myParamValue.equals(that.myParamValue)
            && myConfigurationName.equals(that.myConfigurationName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myOriginal, myParamValue, myConfigurationName);
    }
}
