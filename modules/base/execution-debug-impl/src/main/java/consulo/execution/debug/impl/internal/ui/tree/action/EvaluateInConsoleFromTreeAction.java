package consulo.execution.debug.impl.internal.ui.tree.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.debug.impl.internal.action.XEvaluateInConsoleFromEditorActionHandler;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.ui.console.ConsoleExecuteAction;
import consulo.execution.ui.console.ConsoleView;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ActionImpl(id = "Debugger.Tree.EvaluateInConsole")
public class EvaluateInConsoleFromTreeAction extends XAddToWatchesTreeAction {
    public EvaluateInConsoleFromTreeAction() {
        super(XDebuggerLocalize.actionTreeEvaluateInConsoleText());
    }

    @Override
    protected boolean isEnabled(@Nonnull XValueNodeImpl node, @Nonnull AnActionEvent e) {
        return super.isEnabled(node, e) && getConsoleExecuteAction(e) != null;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
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
        return XEvaluateInConsoleFromEditorActionHandler.getConsoleExecuteAction(e.getData(ConsoleView.KEY));
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