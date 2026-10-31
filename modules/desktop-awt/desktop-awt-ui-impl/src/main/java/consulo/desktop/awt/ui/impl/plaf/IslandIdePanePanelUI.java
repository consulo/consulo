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
package consulo.desktop.awt.ui.impl.plaf;

import consulo.ui.ex.awt.JBUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import java.awt.*;

public class IslandIdePanePanelUI extends BasicIdePanePanelUI {
    public static ComponentUI createUI(JComponent c) {
        return new IslandIdePanePanelUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        String role = getIslandRole(c);
        Border border = c.getBorder();
        if (role != null && (border == null || border instanceof UIResource)) {
            int inset = TOOL_WINDOW_ROLE.equals(role)
                ? JBUI.getInt("Island.ToolWindow.insets", 3)
                : JBUI.getInt("Island.Editor.insets", 2);
            c.setBorder(new BorderUIResource.EmptyBorderUIResource(JBUI.insets(inset)));
        }
    }

    @Override
    public void uninstallUI(JComponent c) {
        if (c.getBorder() instanceof UIResource) {
            c.setBorder(null);
        }

        super.uninstallUI(c);
    }

    @Override
    public void paintAfterChildren(JComponent c, Graphics g) {
        if (getIslandRole(c) != null) {
            IslandPainter.paint(c, g);
        }
    }

    @Override
    public boolean isPaintingOrigin(JComponent c) {
        return getIslandRole(c) != null;
    }
}
