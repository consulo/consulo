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
package consulo.desktop.awt.internal.diff.util;

import consulo.ui.ex.awt.JBValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.function.Predicates;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-07-04
 */
public class AWTDiffUtil {
    public static final JBValue TITLE_GAP = new JBValue.Float(2);

    public static consulo.ui.@Nullable Component wrap(java.awt.@Nullable Component component) {
        return component == null ? null : TargetAWT.wrap(component);
    }

    public static List<JComponent> createSyncHeightComponents(List<JComponent> components) {
        if (!ContainerUtil.exists(components, Predicates.notNull())) {
            return components;
        }
        List<JComponent> result = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            result.add(new SyncHeightComponent(components, i));
        }
        return result;
    }

    private static class SyncHeightComponent extends JPanel {
        
        private final List<JComponent> myComponents;

        public SyncHeightComponent(List<JComponent> components, int index) {
            super(new BorderLayout());
            myComponents = components;
            JComponent delegate = components.get(index);
            if (delegate != null) {
                add(delegate, BorderLayout.CENTER);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.height = getPreferredHeight();
            return size;
        }

        private int getPreferredHeight() {
            int height = 0;
            for (JComponent component : myComponents) {
                if (component == null) {
                    continue;
                }
                height = Math.max(height, component.getPreferredSize().height);
            }
            return height;
        }
    }
}
