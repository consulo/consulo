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
package consulo.ide.impl.idea.xdebugger.impl.actions;

import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.dataContext.DataContext;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.XBreakpointUtil;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.XLineBreakpointImpl;
import consulo.ide.impl.idea.xdebugger.impl.ui.DebuggerUIUtil;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

public class XDebuggerEditBreakpointActionHandler extends EditBreakpointActionHandler {
  @Override
  protected void doShowPopup(Project project, JComponent component, Point whereToShow, Object breakpoint) {
    DebuggerUIUtil.showXBreakpointEditorBalloon(project, whereToShow, component, false, (XBreakpoint)breakpoint);
  }

  @Override
  public boolean isEnabled(@Nonnull Project project, AnActionEvent event) {
    DataContext dataContext = event.getDataContext();
    Editor editor = dataContext.getData(Editor.KEY);
    if (editor == null) return false;
    final Pair<GutterIconRenderer,Object> pair = XBreakpointUtil.findSelectedBreakpoint(project, editor);
    return pair.first != null && pair.second instanceof XLineBreakpointImpl;
  }
}
