package consulo.ide.impl.idea.xdebugger.impl.actions.handlers;

import consulo.execution.ui.console.ConsoleExecuteAction;
import consulo.execution.ui.console.language.LanguageConsoleView;
import consulo.execution.ui.console.ConsoleView;
import consulo.ui.ex.action.AnAction;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.evaluation.ExpressionInfo;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public class XEvaluateInConsoleFromEditorActionHandler extends XAddToWatchesFromEditorActionHandler {
  @Override
  protected boolean isEnabled(@Nonnull XDebugSession session, DataContext dataContext) {
    return super.isEnabled(session, dataContext) && getConsoleExecuteAction(session) != null;
  }

  @Nullable
  private static ConsoleExecuteAction getConsoleExecuteAction(@Nonnull XDebugSession session) {
    return getConsoleExecuteAction(session.getConsoleView());
  }

  @Nullable
  public static ConsoleExecuteAction getConsoleExecuteAction(@Nullable ConsoleView consoleView) {
    if (!(consoleView instanceof LanguageConsoleView)) {
      return null;
    }

    List<AnAction> actions = ActionUtil.getActions(((LanguageConsoleView)consoleView).getConsoleEditor().getComponent());
    ConsoleExecuteAction action = ContainerUtil.findInstance(actions, ConsoleExecuteAction.class);
    return action == null || !action.isEnabled() ? null : action;
  }

  @Override
  protected void perform(@Nonnull XDebugSession session, DataContext dataContext) {
    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    if (editor == null || !(editor instanceof EditorEx)) {
      return;
    }

    int selectionStart = editor.getSelectionModel().getSelectionStart();
    int selectionEnd = editor.getSelectionModel().getSelectionEnd();
    String text;
    TextRange range;
    if (selectionStart != selectionEnd) {
      range = new TextRange(selectionStart, selectionEnd);
      text = editor.getDocument().getText(range);
    }
    else {
      XDebuggerEvaluator evaluator = session.getDebugProcess().getEvaluator();
      if (evaluator != null) {
        ExpressionInfo expressionInfo = evaluator.getExpressionInfoAtOffset(session.getProject(), editor.getDocument(), selectionStart, true);
        if (expressionInfo == null) {
          return;
        }

        // todo check - is it wrong in case of not-null expressionInfo.second - copied (to console history document) text (text range) could be not correct statement?
        range = expressionInfo.getTextRange();
        text = XDebuggerEvaluateActionHandler.getExpressionText(expressionInfo, editor.getDocument());
      }
      else {
        return;
      }
    }

    if (StringUtil.isEmptyOrSpaces(text)) {
      return;
    }

    ConsoleExecuteAction action = getConsoleExecuteAction(session);
    if (action != null) {
      action.execute(range, text, (EditorEx)editor);
    }
  }
}