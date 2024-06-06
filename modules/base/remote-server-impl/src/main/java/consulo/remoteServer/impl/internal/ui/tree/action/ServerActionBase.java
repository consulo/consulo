package consulo.remoteServer.impl.internal.ui.tree.action;

import consulo.remoteServer.impl.internal.ui.ServersToolWindowContent;
import consulo.remoteServer.impl.internal.ui.tree.action.ServersTreeActionBase;
import consulo.ui.ex.action.AnActionEvent;
import consulo.remoteServer.impl.internal.ui.tree.ServerNode;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public abstract class ServerActionBase extends ServersTreeActionBase {

  protected ServerActionBase(String text, String description, Image icon) {
    super(text, description, icon);
  }

  @Override
  protected final boolean isEnabled(@Nonnull ServersToolWindowContent content, AnActionEvent e) {
    Set<ServerNode> selectedServerNodes = content.getSelectedServerNodes();
    Set<?> selectedElements = content.getBuilder().getSelectedElements();
    if (selectedElements.size() != selectedServerNodes.size() || selectedElements.isEmpty()) {
      return false;
    }

    for (ServerNode selectedServer : selectedServerNodes) {
      if (!isEnabledForServer(selectedServer)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected void doActionPerformed(@Nonnull ServersToolWindowContent content) {
    for (ServerNode node : content.getSelectedServerNodes()) {
      performAction(node);
    }
  }

  protected abstract void performAction(@Nonnull ServerNode serverNode);

  protected abstract boolean isEnabledForServer(@Nonnull ServerNode serverNode);
}
