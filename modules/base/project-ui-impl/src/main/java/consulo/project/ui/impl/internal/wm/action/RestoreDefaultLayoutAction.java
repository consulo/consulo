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
package consulo.project.ui.impl.internal.wm.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ui.internal.ToolWindowLayout;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author Vladimir Kondratyev
 */
@ActionImpl(id = "RestoreDefaultLayout")
public final class RestoreDefaultLayoutAction extends AnAction implements DumbAware {
    public RestoreDefaultLayoutAction() {
        super(ActionLocalize.actionRestoredefaultlayoutText(), ActionLocalize.actionRestoredefaultlayoutDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ToolWindowLayout layout = WindowManagerEx.getInstanceEx().getLayout();
        ToolWindowManagerEx.getInstanceEx(project).setLayout(layout);
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        event.getPresentation().setEnabled(event.hasData(Project.KEY));
    }
}
