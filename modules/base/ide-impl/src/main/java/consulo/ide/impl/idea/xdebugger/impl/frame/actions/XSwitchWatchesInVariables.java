// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.xdebugger.impl.frame.actions;

import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.ide.impl.idea.xdebugger.impl.ui.XDebugSessionTab;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class XSwitchWatchesInVariables extends ToggleAction {
    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        XDebugSessionTab tab = e.getData(XDebugSessionTab.TAB_KEY);
        return tab == null || tab.isWatchesInVariables();
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        XDebugSessionTab tab = e.getData(XDebugSessionTab.TAB_KEY);
        if (tab != null) {
            tab.setWatchesInVariables(!tab.isWatchesInVariables());
        }
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return ExecutionDebugIconGroup.nodeWatch();
    }
}
