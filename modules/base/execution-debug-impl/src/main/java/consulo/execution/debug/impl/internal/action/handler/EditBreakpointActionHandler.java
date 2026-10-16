/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.action.handler;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointUtil;
import consulo.execution.debug.impl.internal.breakpoint.ui.BreakpointItem;
import consulo.execution.debug.impl.internal.breakpoint.ui.BreakpointsDialogFactory;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.RelativePoint2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.Pair;

public abstract class EditBreakpointActionHandler extends DebuggerActionHandler {
    @RequiredUIAccess
    protected abstract void doShowPopup(Project project, Component anchor, Point2D whereToShow, Object breakpoint);

    @RequiredUIAccess
    @Override
    public void perform(Project project, AnActionEvent event) {
        Editor editor = event.getData(Editor.KEY);
        if (editor == null) {
            return;
        }

        Pair<GutterIconRenderer, Object> pair = XBreakpointUtil.findSelectedBreakpoint(project, editor);

        Object breakpoint = pair.second;
        GutterIconRenderer breakpointGutterRenderer = pair.first;

        if (breakpointGutterRenderer == null) {
            return;
        }
        editBreakpoint(project, editor, breakpoint, breakpointGutterRenderer);
    }

    @RequiredUIAccess
    public void editBreakpoint(Project project, Editor editor, Object breakpoint, GutterIconRenderer breakpointGutterRenderer) {
        if (BreakpointsDialogFactory.getInstance(project).isBreakpointPopupShowing()) {
            return;
        }
        RelativePoint2D center = ((EditorEx) editor).getGutterComponentEx().getIconCenter(breakpointGutterRenderer);
        if (center != null) {
            Point2D point = center.getUIPoint().translate(breakpointGutterRenderer.getIcon().getWidth() / 2, 0);
            doShowPopup(project, center.getUIComponent(), point, breakpoint);
        }
    }

    @RequiredUIAccess
    public void editBreakpoint(Project project, Component parent, Point2D whereToShow, BreakpointItem breakpoint) {
        doShowPopup(project, parent, whereToShow, breakpoint.getBreakpoint());
    }
}
