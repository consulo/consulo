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
package consulo.desktop.awt.ui.plaf2.flat;

import com.formdev.flatlaf.ui.FlatMenuItemRenderer;
import com.formdev.flatlaf.ui.FlatMenuItemUI;
import consulo.ui.ex.action.util.ShortcutUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2025-09-13
 */
public class FlatMenuItemUI2 extends FlatMenuItemUI {
    private static class FlatMenuItemRenderer2 extends FlatMenuItemRenderer {
        protected FlatMenuItemRenderer2(JMenuItem menuItem, Icon checkIcon, Icon arrowIcon, Font acceleratorFont, String acceleratorDelimiter) {
            super(menuItem, checkIcon, arrowIcon, acceleratorFont, acceleratorDelimiter);
        }

        @Override
        protected String getTextForAccelerator(KeyStroke accelerator) {
            return ShortcutUtil.getKeystrokeTextValue(accelerator).get();
        }
    }

    public static ComponentUI createUI(JComponent c) {
        return new FlatMenuItemUI2();
    }

    @Override
    protected FlatMenuItemRenderer createRenderer() {
        return new FlatMenuItemRenderer2(menuItem, checkIcon, arrowIcon, acceleratorFont, acceleratorDelimiter);
    }
}
