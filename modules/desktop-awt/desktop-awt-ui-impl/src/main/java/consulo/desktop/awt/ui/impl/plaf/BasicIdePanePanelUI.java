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

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;
import java.awt.*;

public class BasicIdePanePanelUI extends BasicPanelUI {
    public static final String UI_CLASS_ID = "IdePanePanelUI";

    public static final String ISLAND_ROLE = "IdePanePanel.islandRole";

    public static final String TOOL_WINDOW_ROLE = "toolWindow";

    public static final String EDITOR_ROLE = "editor";

    public static ComponentUI createUI(JComponent c) {
        return new BasicIdePanePanelUI();
    }

    public void paintAfterChildren(JComponent c, Graphics g) {
    }

    public boolean isPaintingOrigin(JComponent c) {
        return false;
    }

    protected static @Nullable String getIslandRole(JComponent c) {
        return c.getClientProperty(ISLAND_ROLE) instanceof String role ? role : null;
    }
}
