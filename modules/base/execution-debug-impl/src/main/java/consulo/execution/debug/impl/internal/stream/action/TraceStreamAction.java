package consulo.execution.debug.impl.internal.stream.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.stream.StreamDebuggerManager;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.stream.ChainStatus;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
@ActionImpl(id = "StreamTracerAction")
public class TraceStreamAction extends AnAction {
    public TraceStreamAction() {
        super(XDebuggerLocalize.actionStreamtraceractionText(), XDebuggerLocalize.actionStreamtraceractionText(), ExecutionDebugIconGroup.actionTracestream());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        XDebugSession session = e.getData(XDebugSession.DATA_KEY);

        var presentation = e.getPresentation();
        if (session == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        ChainStatus chainStatus = StreamDebuggerManager.getInstance(project).getChainStatus(session);
        if (chainStatus == null) {
            presentation.setEnabledAndVisible(false);
        }
        else {
            switch (chainStatus) {
                case LANGUAGE_NOT_SUPPORTED:
                    presentation.setEnabledAndVisible(false);
                    break;
                case COMPUTING:
                    presentation.setVisible(true);
                    presentation.setEnabled(false);
                    break;
                case FOUND:
                    presentation.setEnabledAndVisible(true);
                    break;
                case NOT_FOUND:
                    presentation.setVisible(true);
                    presentation.setEnabled(false);
                    break;
            }
        }
    }

    @Nonnull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        XDebugSession session = e.getData(XDebugSession.DATA_KEY);

        if (session == null) {
            return;
        }

        TraceStreamRunner.getInstance(project).actionPerformed(session);
    }
}
