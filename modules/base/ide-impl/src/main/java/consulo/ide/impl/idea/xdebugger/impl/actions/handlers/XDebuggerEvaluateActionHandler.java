/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.xdebugger.impl.actions.handlers;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.evaluation.ExpressionInfo;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.internal.breakpoint.XExpressionImpl;
import consulo.ide.impl.idea.xdebugger.impl.evaluate.XDebuggerEvaluationDialog;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import consulo.language.Language;
import consulo.language.psi.PsiFile;
import consulo.language.util.LanguageUtil;
import consulo.project.Project;
import consulo.project.ui.util.AppUIUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class XDebuggerEvaluateActionHandler extends XDebuggerActionHandler {
  @Override
  protected void perform(@Nonnull final XDebugSession session, final DataContext dataContext) {
    final XDebuggerEditorsProvider editorsProvider = session.getDebugProcess().getEditorsProvider();
    final XStackFrame stackFrame = session.getCurrentStackFrame();
    final XDebuggerEvaluator evaluator = session.getDebugProcess().getEvaluator();
    if (evaluator == null) {
      return;
    }

    Editor editor = dataContext.getData(Editor.KEY);

    EvaluationMode mode = EvaluationMode.EXPRESSION;
    String selectedText = editor != null ? editor.getSelectionModel().getSelectedText() : null;
    if (selectedText != null) {
      selectedText = evaluator.formatTextForEvaluation(selectedText);
      mode = evaluator.getEvaluationMode(selectedText, editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd(), dataContext.getData(PsiFile.KEY));
    }
    String text = selectedText;

    if (text == null && editor != null) {
      text = getExpressionText(evaluator, dataContext.getData(Project.KEY), editor);
    }

    final VirtualFile file = dataContext.getData(VirtualFile.KEY);

    if (text == null) {
      XValue value = XDebuggerTreeActionBase.getSelectedValue(dataContext);
      if (value != null) {
        value.calculateEvaluationExpression().doWhenDone(expression -> {
          if (expression != null) {
            AppUIUtil.invokeOnEdt(() -> showDialog(session, file, editorsProvider, stackFrame, evaluator, expression));
          }
        });
        return;
      }
    }

    XExpression expression = XExpression.fromText(StringUtil.notNullize(text), mode);
    showDialog(session, file, editorsProvider, stackFrame, evaluator, expression);
  }

  private static void showDialog(@Nonnull XDebugSession session,
                                 VirtualFile file,
                                 XDebuggerEditorsProvider editorsProvider,
                                 XStackFrame stackFrame,
                                 XDebuggerEvaluator evaluator,
                                 @Nonnull XExpression expression) {
    if (expression.getLanguage() == null) {
      Language language = null;
      if (stackFrame != null) {
        XSourcePosition position = stackFrame.getSourcePosition();
        if (position != null) {
          language = LanguageUtil.getFileLanguage(position.getFile());
        }
      }
      if (language == null && file != null) {
        language = LanguageUtil.getFileTypeLanguage(file.getFileType());
      }
      expression = new XExpressionImpl(expression.getExpression(), language, expression.getCustomInfo(), expression.getMode());
    }
    new XDebuggerEvaluationDialog(session, editorsProvider, evaluator, expression, stackFrame == null ? null : stackFrame.getSourcePosition()).show();
  }

  @Nullable
  public static String getExpressionText(@Nullable XDebuggerEvaluator evaluator, @Nullable Project project, @Nonnull Editor editor) {
    if (project == null || evaluator == null) {
      return null;
    }

    Document document = editor.getDocument();
    return getExpressionText(evaluator.getExpressionInfoAtOffset(project, document, editor.getCaretModel().getOffset(), true), document);
  }

  @Nullable
  public static String getExpressionText(@Nullable ExpressionInfo expressionInfo, @Nonnull Document document) {
    if (expressionInfo == null) {
      return null;
    }
    String text = expressionInfo.getExpressionText();
    return text == null ? document.getText(expressionInfo.getTextRange()) : text;
  }

  @Nullable
  public static String getDisplayText(@Nullable ExpressionInfo expressionInfo, @Nonnull Document document) {
    if (expressionInfo == null) {
      return null;
    }
    String text = expressionInfo.getDisplayText();
    return text == null ? document.getText(expressionInfo.getTextRange()) : text;
  }

  @Override
  protected boolean isEnabled(final @Nonnull XDebugSession session, final DataContext dataContext) {
    return session.getDebugProcess().getEvaluator() != null;
  }
}
