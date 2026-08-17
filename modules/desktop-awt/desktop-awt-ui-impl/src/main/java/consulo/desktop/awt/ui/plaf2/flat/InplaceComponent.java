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
package consulo.desktop.awt.ui.plaf2.flat;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2024-12-19
 */
public class InplaceComponent {
    /**
     * @see com.formdev.flatlaf.ui.FlatTextFieldUI#prepareLeadingOrTrailingComponent(javax.swing.JComponent)
     */
    public static void prepareLeadingOrTrailingComponent(JComponent c) {
        c.putClientProperty(FlatClientProperties.STYLE_CLASS, "inTextField");

        if (c instanceof JButton || c instanceof JToggleButton) {
            c.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);

            if (!c.isCursorSet()) {
                c.setCursor(Cursor.getDefaultCursor());
            }
        }
        else if (c instanceof JToolBar) {
            for (Component child : c.getComponents()) {
                if (child instanceof JComponent) {
                    ((JComponent) child).putClientProperty(FlatClientProperties.STYLE_CLASS, "inTextField");
                }
            }

            if (!c.isCursorSet()) {
                c.setCursor(Cursor.getDefaultCursor());
            }
        }
    }
}
