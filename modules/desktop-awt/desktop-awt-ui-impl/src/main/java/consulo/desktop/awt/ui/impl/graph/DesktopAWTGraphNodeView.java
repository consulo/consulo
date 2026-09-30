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
package consulo.desktop.awt.ui.impl.graph;

import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.VerticalLayout;
import consulo.ui.impl.graph.GraphNodeContent;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
class DesktopAWTGraphNodeView extends JPanel {
    DesktopAWTGraphNodeView(GraphNodeContent<DesktopAWTGraphRowPresentation> content) {
        super(new VerticalLayout(0));
        setOpaque(false);

        SimpleColoredComponent header = content.getHeader().getComponent();
        header.setTextAlign(SwingConstants.CENTER);
        header.setIpad(JBUI.insets(6, 10));
        add(header);

        for (List<DesktopAWTGraphRowPresentation> section : content.getSections()) {
            JComponent separator = new JPanel();
            separator.setBackground(JBColor.border());
            separator.setPreferredSize(new Dimension(1, 1));
            add(separator);

            for (DesktopAWTGraphRowPresentation row : section) {
                SimpleColoredComponent component = row.getComponent();
                component.setIpad(JBUI.insets(2, 10));
                add(component);
            }
        }
    }

    static SimpleColoredComponent createRowComponent() {
        SimpleColoredComponent component = new SimpleColoredComponent();
        component.setOpaque(false);
        return component;
    }
}
