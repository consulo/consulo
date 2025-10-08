/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ActionImpl(id = XDebuggerActions.MUTE_BREAKPOINTS)
public class MuteBreakpointAction extends ToggleAction {
    private final XDebuggerMuteBreakpointsHandler myHandler = new XDebuggerMuteBreakpointsHandler();

    public MuteBreakpointAction() {
        super(
            XDebuggerLocalize.actionMuteBreakpointsText(),
            XDebuggerLocalize.actionMuteBreakpointsDescription(),
            ExecutionDebugIconGroup.actionMutebreakpoints()
        );
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        return project != null && myHandler.isEnabled(project, e) && myHandler.isSelected(project, e);
    }

    @Override
    @RequiredUIAccess
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        Project project = e.getData(Project.KEY);
        if (project != null && myHandler.isEnabled(project, e)) {
            myHandler.setSelected(project, e, state);
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        Project project = e.getData(Project.KEY);
        e.getPresentation().setEnabled(project != null && myHandler.isEnabled(project, e));
    }
}
