/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.util.MnemonicInfo;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2020-05-21
 */
public abstract class LocalizeAction extends AbstractAction {
    private LocalizeValue myTextValue;

    @Deprecated
    @DeprecationInfo("Use constructor with LocalizeValue")
    protected LocalizeAction(String name) {
        this(LocalizeValue.of(name));
    }

    protected LocalizeAction(LocalizeValue nameValue) {
        setText(nameValue);
    }

    public void setText(LocalizeValue text) {
        myTextValue = text;

        updateName();
    }

    public void updateName() {
        if (withMnemonic()) {
            String textValue = myTextValue.get();
            MnemonicInfo mnemonicInfo = MnemonicInfo.parse(textValue);
            if (mnemonicInfo == null) {
                putValue(NAME, textValue);
            }
            else {
                putValue(NAME, mnemonicInfo.getText());

                if (mnemonicInfo.getIndex() != -1) {
                    putValue(MNEMONIC_KEY, (int)mnemonicInfo.getKeyCode());
                    putValue(DISPLAYED_MNEMONIC_INDEX_KEY, mnemonicInfo.getIndex());
                }
            }
        }
        else {
            putValue(NAME, myTextValue.getValue());
        }
    }

    @Nonnull
    public LocalizeValue getTextValue() {
        return myTextValue;
    }

    protected boolean withMnemonic() {
        return true;
    }
}
