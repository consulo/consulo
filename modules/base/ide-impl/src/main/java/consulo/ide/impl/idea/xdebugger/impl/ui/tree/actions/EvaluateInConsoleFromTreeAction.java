package consulo.ide.impl.idea.xdebugger.impl.ui.tree.actions;

import consulo.ide.impl.idea.execution.console.ConsoleExecuteAction;
import consulo.ide.impl.idea.xdebugger.impl.actions.handlers.XEvaluateInConsoleFromEditorActionHandler;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import consulo.execution.ExecutionDataKeys;
import consulo.ui.ex.action.AnActionEvent;

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
    return XEvaluateInConsoleFromEditorActionHandler.getConsoleExecuteAction(e.getData(ExecutionDataKeys.CONSOLE_VIEW));
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