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

import consulo.localize.LocalizeValue;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.util.TextWithMnemonic;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2024-10-21
 */
public final class ExecutionActionValue implements LocalizeValueWithMnemonic {
    private static final String REPLACEMENT_CHAR = "⊹";

    
    public static LocalizeValue buildWithConfiguration(Function<String, LocalizeValue> function, String configurationName) {
        String dummyStr = REPLACEMENT_CHAR.repeat(configurationName.length());

        LocalizeValue dummyValue = function.apply(dummyStr);
        return new ExecutionActionValue(dummyValue, dummyStr, configurationName);
    }

    
    private final LocalizeValue myOriginal;
    
    private final String myParamValue;
    
    private final String myConfigurationName;

    public ExecutionActionValue(LocalizeValue original, String paramValue, String configurationName) {
        myOriginal = original;
        myParamValue = paramValue;
        myConfigurationName = configurationName;
    }

    
    @Override
    public TextWithMnemonic mnemonic() {
        TextWithMnemonic text = TextWithMnemonic.parse(myOriginal.getValue());
        if (!text.hasMnemonic()) {
            return text;
        }

        return text.replaceFirst(myParamValue, myConfigurationName);
    }

    
    @Override
    public String getId() {
        return myOriginal.getKey() + "->executionAction";
    }

    
    @Override
    public String getValue() {
        return myOriginal.getValue().replace(myParamValue, myConfigurationName);
    }

    @Override
    public byte getModificationCount() {
        return myOriginal.getModificationCount();
    }

    @Override
    public int compareTo(LocalizeValue that) {
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
