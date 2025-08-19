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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.ide.impl.idea.openapi.vcs.actions.AbstractVcsAction;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.versionControlSystem.VcsToolWindow;
import consulo.versionControlSystem.action.VcsContext;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 * @since 2006-08-18
 */
public class ShowChangesViewAction extends AbstractVcsAction {
    @Override
    @RequiredUIAccess
    protected void actionPerformed(@Nonnull VcsContext e) {
        if (e.getProject() == null) {
            return;
        }
        ToolWindowManager manager = ToolWindowManager.getInstance(e.getProject());
        if (manager != null) {
            ToolWindow window = manager.getToolWindow(VcsToolWindow.ID);
            if (window != null) {
                window.show(null);
            }
        }
    }

    @Override
    protected void update(@Nonnull VcsContext vcsContext, @Nonnull Presentation presentation) {
        presentation.setVisible(getActiveVcses(vcsContext).size() > 0);
    }

    @Override
    protected boolean forceSyncUpdate(@Nonnull AnActionEvent e) {
        return true;
    }
}
