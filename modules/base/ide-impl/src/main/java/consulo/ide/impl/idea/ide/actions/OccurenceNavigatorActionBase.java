
/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.OccurenceNavigator;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.navigation.Navigatable;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.project.ui.wm.ContentManagerUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;

abstract class OccurenceNavigatorActionBase extends AnAction implements DumbAware {
    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        OccurenceNavigator navigator = getNavigator(e.getDataContext());
        if (navigator == null) {
            return;
        }
        if (!hasOccurenceToGo(navigator)) {
            return;
        }
        OccurenceNavigator.OccurenceInfo occurenceInfo = go(navigator);
        if (occurenceInfo == null) {
            return;
        }
        Navigatable descriptor = occurenceInfo.getNavigateable();
        if (descriptor != null && descriptor.canNavigate()) {
            descriptor.navigate(false);
        }
        if (occurenceInfo.getOccurenceNumber() == -1 || occurenceInfo.getOccurencesCount() == -1) {
            return;
        }
        WindowManager.getInstance().getStatusBar(project)
            .setInfo(IdeLocalize.messageOccurrenceNOfM(occurenceInfo.getOccurenceNumber(), occurenceInfo.getOccurencesCount()).get());
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        Project project = event.getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            // make it invisible only in main menu to avoid initial invisibility in toolbars
            presentation.setVisible(!ActionPlaces.MAIN_MENU.equals(event.getPlace()));
            return;
        }
        OccurenceNavigator navigator = getNavigator(event.getDataContext());
        if (navigator == null) {
            presentation.setEnabled(false);
            // make it invisible only in main menu to avoid initial invisibility in toolbars
            presentation.setVisible(!ActionPlaces.MAIN_MENU.equals(event.getPlace()));
            return;
        }
        presentation.setVisible(true);
        presentation.setEnabled(hasOccurenceToGo(navigator));
        presentation.setText(getDescription(navigator));
    }

    protected abstract OccurenceNavigator.OccurenceInfo go(OccurenceNavigator navigator);

    protected abstract boolean hasOccurenceToGo(OccurenceNavigator navigator);

    protected abstract String getDescription(OccurenceNavigator navigator);

    @Nullable
    @RequiredUIAccess
    protected OccurenceNavigator getNavigator(DataContext dataContext) {
        ContentManager contentManager = ContentManagerUtil.getContentManagerFromContext(dataContext, false);
        if (contentManager != null) {
            Content content = contentManager.getSelectedContent();
            if (content == null) {
                return null;
            }
            JComponent component = content.getComponent();
            return findNavigator(component);
        }

        return (OccurenceNavigator)getOccurenceNavigatorFromContext(dataContext);
    }

    @Nullable
    private static OccurenceNavigator findNavigator(JComponent parent) {
        LinkedList<JComponent> queue = new LinkedList<>();
        queue.addLast(parent);
        while (!queue.isEmpty()) {
            JComponent component = queue.removeFirst();
            if (component instanceof OccurenceNavigator) {
                return (OccurenceNavigator)component;
            }
            if (component instanceof JTabbedPane) {
                final JComponent selectedComponent = (JComponent)((JTabbedPane)component).getSelectedComponent();
                if (selectedComponent != null) {
                    queue.addLast(selectedComponent);
                }
            }
            else if (component != null) {
                for (int i = 0; i < component.getComponentCount(); i++) {
                    Component child = component.getComponent(i);
                    if (!(child instanceof JComponent)) {
                        continue;
                    }
                    queue.addLast((JComponent)child);
                }
            }
        }
        return null;
    }

    @Nullable
    @RequiredUIAccess
    private static Component getOccurenceNavigatorFromContext(DataContext dataContext) {
        Window window = TargetAWT.to(WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow());

        if (window != null) {
            Component component = window.getFocusOwner();
            for (Component c = component; c != null; c = c.getParent()) {
                if (c instanceof OccurenceNavigator) {
                    return c;
                }
            }
        }

        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return null;
        }

        ToolWindowManagerEx mgr = ToolWindowManagerEx.getInstanceEx(project);

        String id = mgr.getLastActiveToolWindowId(component -> findNavigator(component) != null);
        if (id == null) {
            return null;
        }
        return (Component)findNavigator(mgr.getToolWindow(id).getComponent());
    }
}
