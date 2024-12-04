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

/*
 * Class ViewBreakpointsAction
 * @author Jeka
 */
package consulo.ide.impl.idea.xdebugger.impl.actions;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.XBreakpointUtil;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.ui.BreakpointsDialogFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

public class ViewBreakpointsAction extends AnAction implements AnAction.TransparentUpdate, DumbAware {
    private Object myInitialBreakpoint;

    public ViewBreakpointsAction() {
        super(ActionLocalize.actionViewbreakpointsText(), LocalizeValue.of(), PlatformIconGroup.debuggerViewbreakpoints());
    }

    @RequiredUIAccess
    @Override
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

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        Project project = event.getDataContext().getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        presentation.setEnabled(true);
    }
}