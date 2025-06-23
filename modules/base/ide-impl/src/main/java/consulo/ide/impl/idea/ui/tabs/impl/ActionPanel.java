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
import consulo.ui.UIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.tab.TabInfo;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ActionPanel extends NonOpaquePanel {
    private final JBTabsImpl myTabs;

    private final TabLabel myTabLabel;

    private List<? extends AnAction> myVisibleActions = List.of();

    private final ActionToolbar myActionToolbar;

    public ActionPanel(JBTabsImpl tabs, TabInfo tabInfo, TabLabel tabLabel) {
        myTabs = tabs;
        myTabLabel = tabLabel;
        ActionGroup group = tabInfo.getTabLabelActions() != null ? tabInfo.getTabLabelActions() : new DefaultActionGroup();
        String actionPlace = StringUtil.defaultIfEmpty(tabInfo.getTabActionPlace(), "ActionPanel");

        myActionToolbar = ActionToolbarFactory.getInstance().createActionToolbar(actionPlace, group, ActionToolbar.Style.INPLACE);
        myActionToolbar.setTargetComponent(tabs);

        int topPadding = 1;
        int leftPadding = 2;
        if (tabs instanceof JBRunnerTabs) {
            topPadding = 4;
        }

        JComponent component = myActionToolbar.getComponent();
        component.setBorder(JBUI.Borders.empty(topPadding, leftPadding, 0, 0));
        component.setOpaque(false);

        add(component);

        setVisible(false);
    }

    @Nonnull
    public CompletableFuture<Boolean> updateAsync(@Nonnull UIAccess uiAccess) {
        List<? extends AnAction> oldVisibleActions = myVisibleActions;

        return myActionToolbar.updateActionsAsync(uiAccess).handle((actions, throwable) -> {
            if (actions == null) {
                return false;
            }

            myVisibleActions = actions;

            boolean changed = !Objects.equals(oldVisibleActions, myVisibleActions);

            if (changed) {
                uiAccess.give((Runnable) this::update);
            }
            return changed;
        });
    }

    private void update() {
        setVisible(!myVisibleActions.isEmpty());

        if (myTabLabel.getRootPane() != null) {
            if (myTabLabel.isValid()) {
                myTabLabel.repaint();
            }
            else {
                myTabs.revalidateAndRepaint(false);
            }
        }
    }
}
