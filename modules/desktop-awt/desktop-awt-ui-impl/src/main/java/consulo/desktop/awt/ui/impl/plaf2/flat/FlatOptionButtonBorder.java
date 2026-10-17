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
package consulo.desktop.awt.ui.impl.plaf2.flat;

import com.formdev.flatlaf.ui.FlatButtonBorder;
import com.formdev.flatlaf.util.UIScale;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class FlatOptionButtonBorder extends FlatButtonBorder {
    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        Insets result = super.getBorderInsets(c, insets);
        if (FlatOptionButtonUI.hasOptions(c)) {
            if (c.getComponentOrientation().isLeftToRight()) {
                result.right += FlatOptionButtonUI.arrowAreaWidth();
            }
            else {
                result.left += FlatOptionButtonUI.arrowAreaWidth();
            }
        }
        return result;
    }

    public int getOuterWidth(Component c) {
        return Math.round(UIScale.scale((float) getFocusWidth(c)) + UIScale.scale((float) getLineWidth(c)));
    }

    public int getFocusOnlyWidth(Component c) {
        return Math.round(UIScale.scale((float) getFocusWidth(c)));
    }

    public Paint getSeparatorPaint(Component c) {
        return getBorderColor(c);
    }
}
