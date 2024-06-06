package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.execution.executor.Executor;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.application.AllIcons;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public class RunServerAction extends RunServerActionBase {

  public RunServerAction() {
    super("Run/Connect", "Run/Connect to the selected server", AllIcons.Actions.Execute);
  }

  @Override
  protected Executor getExecutor() {
    return DefaultRunExecutor.getRunExecutorInstance();
  }
}
