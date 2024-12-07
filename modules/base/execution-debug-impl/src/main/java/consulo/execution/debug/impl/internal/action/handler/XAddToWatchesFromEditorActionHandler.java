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
package consulo.execution.debug.impl.internal.action.handler;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class XAddToWatchesFromEditorActionHandler extends XDebuggerActionHandler {
  @Override
  protected boolean isEnabled(@Nonnull XDebugSession session, DataContext dataContext) {
    return getTextToEvaluate(dataContext, session) != null;
  }

  @Nullable
  protected static String getTextToEvaluate(DataContext dataContext, XDebugSession session) {
    final Editor editor = dataContext.getData(Editor.KEY);
    if (editor == null) {
      return null;
    }

    String text = editor.getSelectionModel().getSelectedText();
    if (text == null) {
      XDebuggerEvaluator evaluator = session.getDebugProcess().getEvaluator();
      if (evaluator != null) {
        text = XDebuggerEvaluateActionHandler.getExpressionText(evaluator, editor.getProject(), editor);
      }
    }

    return StringUtil.nullize(text, true);
  }

  @Override
  protected void perform(@Nonnull XDebugSession session, DataContext dataContext) {
    final String text = getTextToEvaluate(dataContext, session);
    if (text == null) return;

    ((XDebugSessionImpl)session).getSessionTab().getWatchesView().addWatchExpression(XExpression.fromText(text), -1, true);
  }
}
