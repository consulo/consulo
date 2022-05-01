package consulo.ide.impl.idea.remoteServer.impl.runtime.ui.tree.actions;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.remoteServer.impl.runtime.ui.tree.ServerNode;
import javax.annotation.Nonnull;

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
