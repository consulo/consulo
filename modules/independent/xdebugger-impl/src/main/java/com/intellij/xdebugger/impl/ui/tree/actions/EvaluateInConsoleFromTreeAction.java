package com.intellij.xdebugger.impl.ui.tree.actions;

import com.intellij.execution.console.ConsoleExecuteAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.xdebugger.impl.actions.handlers.XEvaluateInConsoleFromEditorActionHandler;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EvaluateInConsoleFromTreeAction extends XAddToWatchesTreeAction {
  @Override
  protected boolean isEnabled(@Nonnull XValueNodeImpl node, @Nonnull AnActionEvent e) {
    return super.isEnabled(node, e) && getConsoleExecuteAction(e) != null;
  }

  @Override
  public void update(AnActionEvent e) {
    if (getConsoleExecuteAction(e) != null) {
      e.getPresentation().setVisible(true);
      super.update(e);
    }
    else {
      e.getPresentation().setEnabledAndVisible(false);
    }
  }

  @Nullable
  private static ConsoleExecuteAction getConsoleExecuteAction(@Nonnull AnActionEvent e) {
    return XEvaluateInConsoleFromEditorActionHandler.getConsoleExecuteAction(e.getData(LangDataKeys.CONSOLE_VIEW));
  }

  @Override
  protected void perform(XValueNodeImpl node, @Nonnull String nodeName, AnActionEvent e) {
    ConsoleExecuteAction action = getConsoleExecuteAction(e);
    if (action != null) {
      String expression = node.getValueContainer().getEvaluationExpression();
      if (expression != null) {
        action.execute(null, expression, null);
      }
    }
  }
}