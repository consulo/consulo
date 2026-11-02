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

import com.formdev.flatlaf.ui.FlatTextBorder;
import com.formdev.flatlaf.ui.FlatUIUtils;

import java.awt.Component;

/**
 * The text field border of an editor text field, rounded like the border of a text component although the editor
 * text field is not one.
 *
 * @author VISTALL
 * @since 2026-11-02
 */
public class FlatEditorTextFieldBorder extends FlatTextBorder {
    @Override
    protected int getArc(Component c) {
        if (isCellEditor(c)) {
            return 0;
        }

        Boolean roundRect = FlatUIUtils.isRoundRect(c);
        if (roundRect == null) {
            roundRect = this.roundRect;
        }
        return roundRect != null ? (roundRect ? Short.MAX_VALUE : 0) : arc;
    }
}
