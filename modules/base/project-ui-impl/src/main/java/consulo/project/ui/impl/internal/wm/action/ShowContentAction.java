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
package consulo.project.ui.impl.internal.wm.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.action.ShadowAction;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowContentUiType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.awt.*;

@ActionImpl(id = "ShowContent")
public class ShowContentAction extends AnAction implements DumbAware {
    private ToolWindow myWindow;

    @Inject
    public ShowContentAction() {
        super(ActionLocalize.actionShowcontentText(), ActionLocalize.actionShowcontentDescription());
    }

    public ShowContentAction(ToolWindow window, JComponent c) {
        myWindow = window;
        AnAction original = ActionManager.getInstance().getAction("ShowContent");
        new ShadowAction(this, original, c);
        copyFrom(original);
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        ToolWindow window = getWindow(e);
        e.getPresentation().setEnabled(window != null && window.getContentManager().getContentCount() > 1);
        e.getPresentation().setTextValue(
            window == null || window.getContentUiType() == ToolWindowContentUiType.TABBED
                ? ActionLocalize.actionShowcontentText()
                : ActionLocalize.actionShowcontentViewsText()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        getWindow(e).showContentPopup(e.getInputEvent());
    }

    @Nullable
    @RequiredUIAccess
    private ToolWindow getWindow(AnActionEvent event) {
        if (myWindow != null) {
            return myWindow;
        }

        Project project = event.getData(Project.KEY);
        if (project == null) {
            return null;
        }

        ToolWindowManager manager = ToolWindowManager.getInstance(project);

        ToolWindow window = manager.getToolWindow(manager.getActiveToolWindowId());
        if (window == null) {
            return null;
        }

        Component context = event.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        if (context == null) {
            return null;
        }

        return SwingUtilities.isDescendingFrom(window.getComponent(), context) ? window : null;
    }
}
