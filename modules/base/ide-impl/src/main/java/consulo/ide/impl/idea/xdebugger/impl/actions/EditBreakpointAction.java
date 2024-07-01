/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import consulo.ide.impl.idea.xdebugger.impl.DebuggerSupport;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

public class EditBreakpointAction extends XDebuggerActionBase {

  public static class ContextAction extends AnAction {
    private final GutterIconRenderer myRenderer;
    private final Object myBreakpoint;
    private DebuggerSupport myDebuggerSupport;

    public ContextAction(GutterIconRenderer breakpointRenderer, Object breakpoint, DebuggerSupport debuggerSupport) {
      super(ActionLocalize.actionEditbreakpointText());
      myRenderer = breakpointRenderer;
      myBreakpoint = breakpoint;
      myDebuggerSupport = debuggerSupport;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
      final Editor editor = e.getDataContext().getData(Editor.KEY);
      if (editor == null) return;
      myDebuggerSupport.getEditBreakpointAction().editBreakpoint(e.getData(Project.KEY), editor, myBreakpoint, myRenderer);
    }
  }

  @Nonnull
  @Override
  protected DebuggerActionHandler getHandler(@Nonnull DebuggerSupport debuggerSupport) {
    return debuggerSupport.getEditBreakpointAction();
  }
}
