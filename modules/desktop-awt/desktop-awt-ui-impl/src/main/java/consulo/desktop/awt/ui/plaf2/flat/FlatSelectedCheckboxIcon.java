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

import com.formdev.flatlaf.icons.FlatCheckBoxMenuItemIcon;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2024-12-01
 */
public class FlatSelectedCheckboxIcon extends FlatCheckBoxMenuItemIcon {
    @Override
    protected void paintIcon(Component c, Graphics2D g2) {
        g2.setColor(getCheckmarkColor(c));
        paintCheckmark(g2);
    }
}
