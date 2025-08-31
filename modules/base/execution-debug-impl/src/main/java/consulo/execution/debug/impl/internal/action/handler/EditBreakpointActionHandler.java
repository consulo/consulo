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
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.dataContext.DataContext;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointUtil;
import consulo.execution.debug.impl.internal.breakpoint.ui.BreakpointItem;
import consulo.execution.debug.impl.internal.breakpoint.ui.BreakpointsDialogFactory;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

public abstract class EditBreakpointActionHandler extends DebuggerActionHandler {

  protected abstract void doShowPopup(Project project, JComponent component, Point whereToShow, Object breakpoint);

  @Override
  public void perform(@Nonnull Project project, @Nonnull AnActionEvent event) {
    Editor editor = event.getData(Editor.KEY);
    if (editor == null) return;

    Pair<GutterIconRenderer,Object> pair = XBreakpointUtil.findSelectedBreakpoint(project, editor);

    Object breakpoint = pair.second;
    GutterIconRenderer breakpointGutterRenderer = pair.first;

    if (breakpointGutterRenderer == null) return;
    editBreakpoint(project, editor, breakpoint, breakpointGutterRenderer);
  }

  public void editBreakpoint(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Object breakpoint, @Nonnull GutterIconRenderer breakpointGutterRenderer) {
    if (BreakpointsDialogFactory.getInstance(project).isBreakpointPopupShowing()) return;
    EditorGutterComponentEx gutterComponent = ((EditorEx)editor).getGutterComponentEx();
    Point point = gutterComponent.getCenterPoint(breakpointGutterRenderer);
    if (point != null) {
      doShowPopup(project, gutterComponent.getComponent(), point, breakpoint);
    }
  }

  public void editBreakpoint(@Nonnull Project project, @Nonnull JComponent parent, @Nonnull Point whereToShow, @Nonnull BreakpointItem breakpoint) {
    doShowPopup(project, parent, whereToShow, breakpoint.getBreakpoint());
  }
}
