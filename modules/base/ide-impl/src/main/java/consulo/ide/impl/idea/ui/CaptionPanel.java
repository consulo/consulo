/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ui;

import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author max
 */
public class CaptionPanel extends JPanel {
    public static final Key<CaptionPanel> KEY = Key.create(CaptionPanel.class);

    private static final String uiClassUD = "CaptionPanelUI";

    private boolean myActive = false;

    public CaptionPanel() {
        super(new BorderLayout());
    }

    public void setLeftComponent(@Nonnull JComponent component) {
        BorderLayout layout = (BorderLayout) getLayout();

        Component prev = layout.getLayoutComponent(BorderLayout.WEST);
        if (prev != null) {
            remove(prev);
        }

        add(component, BorderLayout.WEST);
    }

    public void setRightComponent(@Nonnull JComponent component) {
        BorderLayout layout = (BorderLayout) getLayout();

        Component prev = layout.getLayoutComponent(BorderLayout.EAST);
        if (prev != null) {
            remove(prev);
        }

        add(component, BorderLayout.EAST);
    }

    @Override
    public String getUIClassID() {
        return uiClassUD;
    }

    public boolean isActive() {
        return myActive;
    }

    public void setActive(final boolean active) {
        myActive = active;
        repaint();
    }

    public boolean isWithinPanel(MouseEvent e) {
        return false;
//        final Point p = SwingUtilities.convertPoint(e.getComponent(), e.getX(), e.getY(), this);
//        final Component c = findComponentAt(p);
//        return c != null && myButtonComponent != null && c != myButtonComponent.component();
    }
}
