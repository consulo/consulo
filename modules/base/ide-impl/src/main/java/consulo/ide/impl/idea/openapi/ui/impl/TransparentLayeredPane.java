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
package consulo.ide.impl.idea.openapi.ui.impl;

import consulo.ui.ex.awt.JBLayeredPane;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;

public class TransparentLayeredPane extends JBLayeredPane {
    public TransparentLayeredPane() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // to not pass events through transparent pane
        addMouseListener(new MouseAdapter() {
        });

        addMouseMotionListener(new MouseMotionAdapter() {
        });
    }

    @Override
    public void addNotify() {
        final Container container = getParent();
        if (container != null) {
            setBounds(0, 0, container.getWidth(), container.getHeight());
        }

        super.addNotify();
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return getComponentCount() <= 1;
    }
}
