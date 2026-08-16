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
package consulo.desktop.qt.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.util.TextWithMnemonic;

/**
 * The api marks the mnemonic of a text with {@code _} or {@code &}, and qt marks it with {@code &} alone, so a
 * value handed to a widget untouched is drawn with the marker in it - the new project form read "Project
 * n&ame:" that way.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public final class QtMnemonic {
    /**
     * The text of a widget which knows about mnemonics - every {@link io.qt.widgets.QAbstractButton}, a
     * {@link io.qt.gui.QAction}, a {@link io.qt.widgets.QLabel} with a buddy. The marker of the api is dropped
     * and written back where the parse found it, and every other {@code &} is doubled so qt draws it.
     */
    public static String withMnemonic(LocalizeValue value) {
        TextWithMnemonic textWithMnemonic = LocalizeValueWithMnemonic.get(value);

        String plainText = textWithMnemonic.getText();
        int mnemonicIndex = textWithMnemonic.getMnemonicIndex();

        StringBuilder builder = new StringBuilder(plainText.length() + 8);
        for (int i = 0; i < plainText.length(); i++) {
            char c = plainText.charAt(i);

            if (i == mnemonicIndex || c == '&') {
                builder.append('&');
            }

            builder.append(c);
        }

        return builder.toString();
    }

    /**
     * The text of a widget which draws no mnemonic at all - the caption of a section, the title of a tab - the
     * same way the web frontend answers every text it renders.
     */
    public static String plain(LocalizeValue value) {
        return LocalizeValueWithMnemonic.get(value).getText();
    }

    private QtMnemonic() {
    }
}
