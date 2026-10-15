/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ui.ex.awt.internal;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2025-03-04
 */
public class AWTComboBoxStyle {
    private static final String FLAT_STYLE = "FlatLaf.style";

    public static void makeBorderInline(JComboBox<?> comboBox) {
        addStyle(comboBox, "borderColor: #0000; disabledBorderColor: #0000; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; innerOutlineWidth: 0; arc: 0");
    }

    public static void makeTransparent(JComboBox<?> comboBox) {
        comboBox.setOpaque(false);
        addStyle(
            comboBox,
            "background: #0000; disabledBackground: #0000; focusedBackground: #0000; "
                + "buttonBackground: #0000; buttonEditableBackground: #0000; buttonFocusedBackground: #0000"
        );
    }

    private static void addStyle(JComboBox<?> comboBox, String style) {
        Object oldStyle = comboBox.getClientProperty(FLAT_STYLE);
        if (oldStyle instanceof String old && !old.isBlank()) {
            comboBox.putClientProperty(FLAT_STYLE, old + "; " + style);
        }
        else {
            comboBox.putClientProperty(FLAT_STYLE, style);
        }
    }
}
