package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.application.AllIcons;
import consulo.remoteServer.impl.internal.ui.tree.ServerNode;
import jakarta.annotation.Nonnull;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public class StopServerAction extends ServerActionBase {

  public StopServerAction() {
    super("Stop/Disconnect", "Stop/disconnect from the selected server", AllIcons.Actions.Suspend);
  }

  protected void performAction(@Nonnull ServerNode serverNode) {
    if (serverNode.isStopActionEnabled()) {
      serverNode.stopServer();
    }
  }

  @Override
  protected boolean isEnabledForServer(@Nonnull ServerNode serverNode) {
    return serverNode.isStopActionEnabled();
  }
}
