package com.intellij.remoteServer.impl.runtime.ui.tree.actions;

import com.intellij.remoteServer.impl.runtime.ui.ServersToolWindowContent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.remoteServer.impl.runtime.ui.tree.ServerNode;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

import javax.swing.*;
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
