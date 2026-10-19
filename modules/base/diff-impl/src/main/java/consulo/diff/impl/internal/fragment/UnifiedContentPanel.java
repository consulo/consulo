/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.fragment;

import consulo.codeEditor.Editor;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.List;

class UnifiedContentPanel {
    private final DockLayout myPanel;

    @RequiredUIAccess
    public UnifiedContentPanel(List<? extends @Nullable Component> titles, Editor editor) {
        myPanel = DockLayout.create(Space.NONE);
        myPanel.center(editor.getUIComponent());

        assert titles.size() == 2;
        Component title1 = titles.get(0);
        Component title2 = titles.get(1);

        if (title1 != null || title2 != null) {
            if (title1 == null) {
                myPanel.top(title2);
            }
            else if (title2 == null) {
                myPanel.top(title1);
            }
            else {
                VerticalLayout panel = VerticalLayout.create(Space.NONE);
                panel.add(title2);
                panel.add(title1);
                myPanel.top(panel);
            }
        }
    }

    public Component getComponent() {
        return myPanel;
    }
}
