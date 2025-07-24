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
package consulo.execution.debug.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointUtil;
import consulo.execution.debug.impl.internal.breakpoint.ui.BreakpointsDialogFactory;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author Jeka
 */
@ActionImpl(id = "ViewBreakpoints")
public class ViewBreakpointsAction extends AnAction implements DumbAware {
    private Object myInitialBreakpoint;

    public ViewBreakpointsAction() {
        super(XDebuggerLocalize.actionViewBreakpointsText(), LocalizeValue.empty(), ExecutionDebugIconGroup.actionViewbreakpoints());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return;
        }

        if (myInitialBreakpoint == null) {
            Editor editor = dataContext.getData(Editor.KEY);
            if (editor != null) {
                myInitialBreakpoint = XBreakpointUtil.findSelectedBreakpoint(project, editor).second;
            }
        }

        BreakpointsDialogFactory.getInstance(project).showDialog(myInitialBreakpoint);
        myInitialBreakpoint = null;
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        event.getPresentation().setEnabled(event.hasData(Project.KEY));
    }
}