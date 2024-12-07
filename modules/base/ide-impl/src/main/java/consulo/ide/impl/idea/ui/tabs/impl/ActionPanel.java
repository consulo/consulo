/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.tabs.impl;

import consulo.ide.impl.idea.execution.ui.layout.impl.JBRunnerTabs;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.tab.TabInfo;

import javax.swing.*;
import java.awt.*;

public class ActionPanel extends NonOpaquePanel {
    private boolean myActionsIsVisible = false;

    private final ActionToolbar myActionToolbar;

    public ActionPanel(JBTabsImpl tabs, TabInfo tabInfo) {
        ActionGroup group = tabInfo.getTabLabelActions() != null ? tabInfo.getTabLabelActions() : new DefaultActionGroup();

        myActionToolbar = ActionManager.getInstance().createActionToolbar("RunToolbar", group, true);
        myActionToolbar.setTargetComponent(tabs);
        myActionToolbar.setMiniMode(true);
        myActionToolbar.setContentAreaFilled(false);

        int topPadding = 1;
        int leftPadding = 2;
        if (tabs instanceof JBRunnerTabs) {
            topPadding = 4;
        }

        JComponent component = myActionToolbar.getComponent();
        component.setBorder(JBUI.Borders.empty(topPadding, leftPadding, 0, 0));
        add(component);
    }

    @RequiredUIAccess
    public boolean update() {
        boolean old = myActionsIsVisible;

        myActionToolbar.updateActionsImmediately();
        myActionsIsVisible = myActionToolbar.hasVisibleActions();

        return old != myActionsIsVisible;
    }

    @Override
    public Dimension getPreferredSize() {
        return myActionsIsVisible ? super.getPreferredSize() : new Dimension(0, 0);
    }
}
