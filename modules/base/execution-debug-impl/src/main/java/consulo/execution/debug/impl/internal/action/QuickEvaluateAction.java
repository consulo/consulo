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
package consulo.execution.debug.impl.internal.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutter;
import consulo.codeEditor.LogicalPosition;
import consulo.execution.debug.impl.internal.action.handler.DebuggerActionHandler;
import consulo.execution.debug.impl.internal.action.XDebuggerActionBase;
import consulo.execution.debug.impl.internal.DebuggerSupport;
import consulo.execution.debug.impl.internal.evaluate.QuickEvaluateHandler;
import consulo.execution.debug.impl.internal.evaluate.ValueHintType;
import consulo.execution.debug.impl.internal.evaluate.ValueLookupManager;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class QuickEvaluateAction extends XDebuggerActionBase {
  public QuickEvaluateAction() {
    super(true);
  }

  @Override
  @Nonnull
  protected DebuggerActionHandler getHandler(@Nonnull final DebuggerSupport debuggerSupport) {
    return new QuickEvaluateHandlerWrapper(debuggerSupport.getQuickEvaluateHandler());
  }

  private static class QuickEvaluateHandlerWrapper extends DebuggerActionHandler {
    private final QuickEvaluateHandler myHandler;

    public QuickEvaluateHandlerWrapper(final QuickEvaluateHandler handler) {
      myHandler = handler;
    }

    @Override
    public void perform(@Nonnull final Project project, final AnActionEvent event) {
      Editor editor = event.getData(Editor.KEY);
      if (editor != null) {
        LogicalPosition logicalPosition = editor.getCaretModel().getLogicalPosition();
        ValueLookupManager.getInstance(project)
          .showHint(myHandler, editor, editor.logicalPositionToXY(logicalPosition), ValueHintType.MOUSE_CLICK_HINT);
      }
    }

    @Override
    public boolean isEnabled(@Nonnull final Project project, final AnActionEvent event) {
      if (!myHandler.isEnabled(project)) {
        return false;
      }

      Editor editor = event.getData(Editor.KEY);
      return editor != null && event.getData(EditorGutter.KEY) == null;
    }
  }
}
